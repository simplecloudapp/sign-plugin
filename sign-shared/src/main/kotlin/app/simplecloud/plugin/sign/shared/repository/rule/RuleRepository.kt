package app.simplecloud.plugin.sign.shared.repository.rule

import app.simplecloud.plugin.api.shared.repository.ResourcedYamlDirectoryRepository
import app.simplecloud.plugin.sign.shared.config.rule.RuleConfig
import java.nio.file.Path

class RuleRepository(
    directory: Path,
) : ResourcedYamlDirectoryRepository<String, RuleConfig>(directory, RuleConfig::class.java) {

    override fun save(element: RuleConfig) {
        save(getFileName(element.name), element)
    }

    override fun find(identifier: String): RuleConfig? {
        return getAll().find { it.name.uppercase() == identifier.uppercase() }
    }

    override fun getFileName(identifier: String): String {
        return "$identifier.yml"
    }
}