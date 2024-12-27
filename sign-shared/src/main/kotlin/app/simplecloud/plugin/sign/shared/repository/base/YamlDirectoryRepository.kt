package app.simplecloud.plugin.sign.shared.repository.base

import app.simplecloud.plugin.sign.shared.rule.RuleRegistry
import app.simplecloud.plugin.sign.shared.rule.SignRule
import io.leangen.geantyref.TypeToken
import kotlinx.coroutines.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.loader.ParsingException
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Type
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.jar.JarFile
import kotlin.io.path.pathString


abstract class YamlDirectoryRepository<I, E>(
    private val directory: Path,
    private val clazz: Class<E>,
    private val ruleRegistry: RuleRegistry? = null
) : LoadableRepository<I, E> {

    private val watchService = FileSystems.getDefault().newWatchService()
    private val loaders = mutableMapOf<File, YamlConfigurationLoader>()
    protected val entities = mutableMapOf<File, E>()

    private var serializers: TypeSerializerCollection? = null

    abstract fun getFileName(identifier: I): String

    override fun delete(element: E): Boolean {
        val file = entities.keys.find { entities[it] == element } ?: return false
        return deleteFile(file)
    }

    override fun getAll(): List<E> {
        return entities.values.toList()
    }

    override fun load(serializers: TypeSerializerCollection?): List<E> {
        this.serializers = serializers

        if (!directory.toFile().exists()) {
            directory.toFile().mkdirs()
            loadDefaults()
        }

        registerWatcher()

        return Files.walk(directory)
            .toList()
            .filter { !it.toFile().isDirectory && it.toString().endsWith(".yml") }
            .mapNotNull { load(it.toFile()) }
    }

    private fun load(file: File): E? {
        try {
            val loader = getOrCreateLoader(file)
            val node = loader.load(ConfigurationOptions.defaults())
            val entity = node.get(clazz) ?: return null
            entities[file] = entity
            return entity
        } catch (ex: ParsingException) {
            val existedBefore = entities.containsKey(file)
            if (existedBefore) {
                return null
            }

            return null
        }
    }

    private fun deleteFile(file: File): Boolean {
        val deletedSuccessfully = file.delete()
        val removedSuccessfully = entities.remove(file) != null
        return deletedSuccessfully && removedSuccessfully
    }

    protected fun save(fileName: String, entity: E) {
        val file = directory.resolve(fileName).toFile()
        val loader = getOrCreateLoader(file)
        val node = loader.createNode(ConfigurationOptions.defaults())
        node.set(clazz, entity)
        loader.save(node)
        entities[file] = entity
    }

    private fun getOrCreateLoader(file: File): YamlConfigurationLoader {
        return loaders.getOrPut(file) {
            YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .defaultOptions { options ->
                    options.serializers { builder ->
                        serializers?.let { builder.registerAll(it) }

                        ruleRegistry?.let { registry ->
                            builder.register(TypeToken.get(SignRule::class.java), object : TypeSerializer<SignRule> {
                                override fun deserialize(type: Type, node: ConfigurationNode): SignRule {
                                    val ruleName =
                                        node.string ?: throw SerializationException("Rule name cannot be null")

                                    return registry.getRule(ruleName)
                                        ?: throw SerializationException("Unknown rule: $ruleName")
                                }

                                override fun serialize(type: Type, obj: SignRule?, node: ConfigurationNode) {
                                    if (obj != null) {
                                        node.set(obj.getRuleName())
                                    }
                                }
                            })
                        }

                        builder.registerAnnotatedObjects(objectMapperFactory())
                    }
                }.build()
        }
    }

    private fun registerWatcher(): Job {
        directory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val key = watchService.take()

                key.pollEvents().forEach { event ->
                    val path = event.context() as? Path ?: return@forEach
                    if (!path.toString().endsWith(".yml")) return@forEach

                    val resolvedPath = directory.resolve(path)
                    if (Files.isDirectory(resolvedPath)) return@forEach

                    when (event.kind()) {
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                            delay(100)
                            load(resolvedPath.toFile())
                        }

                        StandardWatchEventKinds.ENTRY_DELETE -> {
                            deleteFile(resolvedPath.toFile())
                        }
                    }
                }

                key.reset()
            }
        }
    }

    private fun loadDefaults() {
        val targetDirectory = File(directory.toUri()).apply { mkdirs() }

        val last = directory.pathString.split('/').last()

        val resourceUrl = YamlDirectoryRepository::class.java.getResource("/$last") ?: run {
            println("$last folder not found in resources")
            return
        }

        when (resourceUrl.protocol) {
            "file" -> handleFileProtocol(resourceUrl, targetDirectory)
            "jar" -> handleJarProtocol(resourceUrl, targetDirectory)
            else -> println("Unsupported protocol: ${resourceUrl.protocol}")
        }
    }

    private fun handleFileProtocol(resourceUrl: URL, targetDirectory: File) {
        val resourceDir = File(resourceUrl.toURI())
        if (resourceDir.exists()) {
            resourceDir.copyRecursively(targetDirectory, overwrite = true)
        } else {
            println("Resource directory does not exist: ${resourceUrl.path}")
        }
    }

    private fun handleJarProtocol(resourceUrl: URL, targetDirectory: File) {
        val jarPath = resourceUrl.path.substringBefore("!").removePrefix("file:")
        try {
            JarFile(jarPath).use { jarFile ->
                val last = directory.pathString.split('/').last()
                jarFile.entries().asSequence()
                    .filter { it.name.startsWith("$last/") && !it.isDirectory }
                    .forEach { entry ->
                        val targetFile = File(targetDirectory, entry.name.removePrefix("$last/"))
                        targetFile.parentFile.mkdirs()
                        try {
                            jarFile.getInputStream(entry).use { inputStream ->
                                FileOutputStream(targetFile).use { fos ->
                                    // Force UTF-8 BOM
                                    fos.write(0xEF)
                                    fos.write(0xBB)
                                    fos.write(0xBF)

                                    //copy content
                                    inputStream.copyTo(fos)
                                }
                            }
                        } catch (e: Exception) {
                            println("Error copying file ${entry.name}: ${e.message}")
                        }
                    }
            }
        } catch (e: Exception) {
            println("Error processing JAR file: ${e.message}")
            e.printStackTrace()
        }
    }
}