package app.simplecloud.plugin.sign.shared.repository

import app.simplecloud.plugin.sign.shared.config.LayoutConfig
import java.nio.file.Path

class LayoutRepository(
    directoryPath: Path
): YamlDirectoryRepository<String, LayoutConfig>(directoryPath, LayoutConfig::class.java) {

    override fun save(element: LayoutConfig) {
        save(getFileName(element.name), element)
    }

    override fun getFileName(name: String): String {
        return "$name.yml"
    }

    override fun find(name: String): LayoutConfig? {
        return entities.values.find { it.name == name }
    }
}