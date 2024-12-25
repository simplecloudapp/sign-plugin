package app.simplecloud.plugin.sign.shared.config.layout

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.repository.layout.LayoutRepository
import java.nio.file.Files
import java.nio.file.Path

class LayoutManager(private val layoutDirectory: Path) {

    private val defaultRepository = LayoutRepository(layoutDirectory)
    private val groupRepositories = mutableMapOf<String, LayoutRepository>()

    fun getAllLayouts(): List<LayoutConfig> {
        return defaultRepository.getAll() + groupRepositories.values.flatMap { it.getAll() }
    }

    fun getLayout(server: Server?): LayoutConfig {
        val serverGroup = server?.group

        // Try group-specific layouts first if server has a group
        serverGroup?.let { group ->
            groupRepositories[group]?.getAll()
                ?.filter { it.group == group }
                ?.sortedBy { it.priority }
                ?.firstOrNull { it.rule.checker.check(server) }
                ?.let { return it }
        }

        // Fall back to default layouts
        return defaultRepository.getAll()
            .filter { it.group.isBlank() }
            .sortedBy { it.priority }
            .firstOrNull { it.rule.checker.check(server) }
            ?: LayoutConfig()
    }

    fun load() {
        defaultRepository.load()

        Files.list(layoutDirectory).use { path ->
            path.filter { Files.isDirectory(it) }
                .forEach { groupDirectory ->
                    val groupName = groupDirectory.fileName.toString()
                    val groupLayoutRepository = LayoutRepository(layoutDirectory.resolve(groupName))

                    groupRepositories[groupName] = groupLayoutRepository

                    groupLayoutRepository.load()
                }
        }
    }
}