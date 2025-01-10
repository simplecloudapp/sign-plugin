package app.simplecloud.plugin.sign.shared.repository

import app.simplecloud.plugin.api.shared.repository.YamlDirectoryRepository
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.exists
import kotlin.io.path.pathString

abstract class ResourcedYamlDirectoryRepository<I, E>(
    private val directory: Path,
    clazz: Class<E>,
) : YamlDirectoryRepository<I, E>(directory, clazz) {

    override fun load(): List<E> {
        if (!directory.exists()) loadDefaults()

        return super.load()
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
                                    fos.write(0xEF)
                                    fos.write(0xBB)
                                    fos.write(0xBF)

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