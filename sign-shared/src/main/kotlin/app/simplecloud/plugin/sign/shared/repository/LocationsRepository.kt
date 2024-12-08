package app.simplecloud.plugin.sign.shared.repository

import app.simplecloud.plugin.sign.shared.LocationMapper
import app.simplecloud.plugin.sign.shared.config.LocationsConfig
import java.nio.file.Path

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

    fun saveLocation(groupName: String, location: T) {
        val config = find(groupName) ?: LocationsConfig(serverGroup = groupName)
        val unmappedLocation = locationMapper.unmap(location)

        println(unmappedLocation)
        unmappedLocation.forEach { t, u ->
            println("$t: $u ${u::class}")
        }

        save(
            config.copy(
                locations = listOf(*config.locations.toTypedArray(), unmappedLocation)
            )
        )
        println("Saved location $unmappedLocation")
    }

    fun removeLocation(location: T) {
        val config = findByLocation(location) ?: return
        val unmappedLocation = locationMapper.unmap(location)
        save(
            config.copy(
                locations = config.locations.filter { it != unmappedLocation }
            )
        )
    }

    fun findByLocation(location: T): LocationsConfig? {
        return entities.values.find { it.locations.any { it == locationMapper.unmap(location) } }
    }

}
