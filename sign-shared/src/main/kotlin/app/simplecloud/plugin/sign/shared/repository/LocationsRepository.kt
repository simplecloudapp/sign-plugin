package app.simplecloud.plugin.sign.shared.repository

import app.simplecloud.plugin.sign.shared.LocationMapper
import app.simplecloud.plugin.sign.shared.config.LocationsConfig
import java.nio.file.Files
import java.nio.file.Path

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class LocationsRepository<T>(
    private val directoryPath: Path,
    private val locationMapper: LocationMapper<T>,
) : YamlDirectoryRepository<String, LocationsConfig>(directoryPath, LocationsConfig::class.java) {

    override fun save(element: LocationsConfig) {
        save(getFileName(element.serverGroup), element)
    }

    override fun getFileName(groupName: String): String {
        return "$groupName.yml"
    }

    override fun find(groupName: String): LocationsConfig? {
        return entities.values.find { it.serverGroup == groupName }
    }

    private fun findByLocation(location: T): LocationsConfig? {
        return entities.values.find { it -> it.locations.any { it == locationMapper.unmap(location) } }
    }

    fun saveLocation(groupName: String, location: T) {
        val config = find(groupName) ?: LocationsConfig(serverGroup = groupName)
        val unmappedLocation = locationMapper.unmap(location)

        save(
            config.copy(
                locations = listOf(*config.locations.toTypedArray(), unmappedLocation)
            )
        )
    }

    fun removeLocation(location: T) {
        val config = findByLocation(location) ?: return
        val unmappedLocation = locationMapper.unmap(location)
        val updatedList = config.locations.filterNot { it == unmappedLocation }

        save(config.copy(locations = updatedList))

        if (updatedList.isEmpty()) {
            Files.delete(directoryPath.resolve(getFileName(config.serverGroup)))
        }
    }
}
