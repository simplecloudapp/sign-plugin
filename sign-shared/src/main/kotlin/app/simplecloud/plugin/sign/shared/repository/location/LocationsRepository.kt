package app.simplecloud.plugin.sign.shared.repository.location

import app.simplecloud.plugin.sign.shared.LocationMapper
import app.simplecloud.plugin.sign.shared.config.location.LocationsConfig
import app.simplecloud.plugin.sign.shared.config.location.SignLocation
import app.simplecloud.plugin.sign.shared.repository.base.YamlDirectoryRepository
import java.nio.file.Files
import java.nio.file.Path

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class LocationsRepository<T : Any>(
    private val directoryPath: Path,
    private val locationMapper: LocationMapper<T>,
) : YamlDirectoryRepository<String, LocationsConfig>(directoryPath, LocationsConfig::class.java) {

    override fun save(element: LocationsConfig) {
        val prefix = if (element.isPersistentServer()) "persistent-" else "group-"
        save(getFileName(prefix + element.getIdentifier()), element)
    }

    override fun getFileName(key: String): String {
        return "$key.yml"
    }

    override fun find(key: String): LocationsConfig? {
        return entities.values.find { it.getIdentifier() == key }
    }

    private fun findByLocation(location: T): LocationsConfig? {
        return entities.values.find { it -> it.locations.any { it == locationMapper.unmap(location) } }
    }

    private fun findBySignLocation(location: SignLocation): LocationsConfig? {
        return entities.values.find { config -> config.locations.any { it == location } }
    }

    fun saveLocationForGroup(group: String, location: T) {
        val config = find(group) ?: LocationsConfig(group = group)
        val signLocation = locationMapper.unmap(location)

        save(
            config.copy(
                locations = config.locations + signLocation
            )
        )
    }

    fun saveLocationForPersistentServer(persistentServerId: String, location: T) {
        val config = find(persistentServerId) ?: LocationsConfig(persistentServer = persistentServerId)
        val signLocation = locationMapper.unmap(location)

        save(
            config.copy(
                locations = config.locations + signLocation
            )
        )
    }

    fun removeLocationConfig(key: String) {
        Files.delete(directoryPath.resolve(getFileName(key)))
    }

    fun removeLocation(location: T) {
        val config = findByLocation(location) ?: return
        val signLocation = locationMapper.unmap(location)

        save(config.copy(locations = config.locations - signLocation))
    }

    fun removeSignLocation(location: SignLocation) {
        val config = findBySignLocation(location) ?: return
        save(config.copy(locations = config.locations - location))
    }
}
