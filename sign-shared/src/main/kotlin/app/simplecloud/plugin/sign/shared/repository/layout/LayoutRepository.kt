package app.simplecloud.plugin.sign.shared.repository.layout

import app.simplecloud.plugin.api.shared.repository.ResourcedYamlDirectoryRepository
import app.simplecloud.plugin.sign.shared.config.layout.LayoutConfig
import java.nio.file.Path

class LayoutRepository(
    directory: Path
) : ResourcedYamlDirectoryRepository<String, LayoutConfig>(directory, LayoutConfig::class.java) {

    override fun save(element: LayoutConfig) {
        save(getFileName(element.name), element)
    }

    override fun getFileName(identifier: String): String {
        return "$identifier.yml"
    }

    override fun find(identifier: String): LayoutConfig? {
        return getAll().find { it.name == identifier }
    }
}