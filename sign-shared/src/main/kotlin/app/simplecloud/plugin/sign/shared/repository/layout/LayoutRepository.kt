package app.simplecloud.plugin.sign.shared.repository.layout

import app.simplecloud.plugin.sign.shared.config.layout.LayoutConfig
import app.simplecloud.plugin.sign.shared.repository.ResourcedYamlDirectoryRepository
import java.nio.file.Path

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class LayoutRepository(
    directory: Path
) : ResourcedYamlDirectoryRepository<String, LayoutConfig>(directory, LayoutConfig::class.java) {

    override fun save(element: LayoutConfig) {
        save(getFileName(element.name), element)
    }

    override fun getFileName(name: String): String {
        return "$name.yml"
    }

    override fun find(name: String): LayoutConfig? {
        return getAll().find { it.name == name }
    }
}