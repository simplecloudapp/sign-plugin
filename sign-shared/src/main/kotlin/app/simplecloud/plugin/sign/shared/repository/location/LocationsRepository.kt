package app.simplecloud.plugin.sign.shared.repository.location

import app.simplecloud.plugin.sign.shared.LocationMapper
import app.simplecloud.plugin.sign.shared.config.location.LocationsConfig
import app.simplecloud.plugin.sign.shared.repository.base.YamlDirectoryRepository
import java.nio.file.Path

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class LocationsRepository<T>(
    private val directoryPath: Path,
    private val locationMapper: LocationMapper<T>,
) : YamlDirectoryRepository<String, LocationsConfig>(directoryPath, LocationsConfig::class.java) {

    override fun save(element: LocationsConfig) {
        save(getFileName(element.group), element)
    }

    override fun getFileName(groupName: String): String {
        return "$groupName.yml"
    }

    override fun find(groupName: String): LocationsConfig? {
        return entities.values.find { it.group == groupName }
    }

    private fun findByLocation(location: T): LocationsConfig? {
        return entities.values.find { it -> it.locations.any { it == locationMapper.unmap(location) } }
    }

    fun saveLocation(group: String, location: T) {
        val config = find(group) ?: LocationsConfig(group)
        val signLocation = locationMapper.unmap(location)

        save(
            config.copy(
                locations = config.locations + signLocation
            )
        )
    }

    fun removeLocation(location: T) {
        val config = findByLocation(location) ?: return
        val signLocation = locationMapper.unmap(location)

        save(config.copy(locations = config.locations - signLocation))
    }
}
