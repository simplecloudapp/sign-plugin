package app.simplecloud.plugin.sign.shared.repository

import kotlinx.coroutines.*
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.loader.ParsingException
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.jar.JarFile


abstract class YamlDirectoryRepository<I, E>(
    private val directory: Path,
    private val clazz: Class<E>,
) : LoadableRepository<I, E> {

    private val watchService = FileSystems.getDefault().newWatchService()
    private val loaders = mutableMapOf<File, YamlConfigurationLoader>()
    protected val entities = mutableMapOf<File, E>()

    abstract fun getFileName(identifier: I): String

    override fun delete(element: E): Boolean {
        val file = entities.keys.find { entities[it] == element } ?: return false
        return deleteFile(file)
    }

    override fun getAll(): List<E> {
        return entities.values.toList()
    }

    override fun load(): List<E> {
        if (!directory.toFile().exists()) {
            directory.toFile().mkdirs()
        }

        registerWatcher()

        return Files.list(directory)
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
                for (event in key.pollEvents()) {
                    val path = event.context() as? Path ?: continue
                    val resolvedPath = directory.resolve(path)
                    if (Files.isDirectory(resolvedPath) || !resolvedPath.toString().endsWith(".yml")) {
                        continue
                    }
                    val kind = event.kind()
                    when (kind) {
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                            -> {
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

    fun loadLayoutDefaults(classLoader: ClassLoader) {
        val targetDirectory = File(directory.toUri()).apply { mkdirs() }

        val resourceUrl = classLoader.getResource("layouts") ?: run {
            println("Layouts folder not found in resources")
            return
        }

        when (resourceUrl.protocol) {
            "file" -> {
                val resourceDir = File(resourceUrl.toURI())
                resourceDir.copyRecursively(targetDirectory, overwrite = true)
            }

            "jar" -> {
                val jarPath = resourceUrl.path.substringBefore("!")
                    .removePrefix("file:")

                try {
                    JarFile(jarPath).use { jarFile ->
                        jarFile.entries().asSequence()
                            .filter { it.name.startsWith("layouts/") && !it.isDirectory }
                            .forEach { entry ->
                                val targetFile = File(targetDirectory, entry.name.removePrefix("layouts/"))
                                targetFile.parentFile.mkdirs()

                                try {
                                    jarFile.getInputStream(entry).use { resourceStream ->
                                        targetFile.outputStream().use { fileOutputStream ->
                                            resourceStream.copyTo(fileOutputStream)
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

            else -> println("Unsupported protocol: ${resourceUrl.protocol}")
        }
    }
}