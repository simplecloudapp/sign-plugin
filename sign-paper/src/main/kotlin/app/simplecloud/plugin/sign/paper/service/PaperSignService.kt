package app.simplecloud.plugin.sign.paper.service

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.sign.paper.PaperSignsPluginBootstrap
import app.simplecloud.plugin.sign.shared.CloudSign
import app.simplecloud.plugin.sign.shared.LocationMapper
import app.simplecloud.plugin.sign.shared.config.location.LocationsConfig
import app.simplecloud.plugin.sign.shared.config.location.SignLocation
import app.simplecloud.plugin.sign.shared.service.SignService
import org.bukkit.Location

class PaperSignService(private val bootstrap: PaperSignsPluginBootstrap) : SignService<Location>,
    LocationMapper<Location> {

    override val controllerApi: CloudApi
        get() = bootstrap.signManager.controllerApi

    override fun getCloudSign(location: Location): CloudSign<Location>? =
        bootstrap.signManager.getCloudSign(location)

    override fun getAllLocations(): List<SignLocation> =
        bootstrap.signManager.getAllLocations()

    override fun getAllConfigs(): List<LocationsConfig> =
        bootstrap.signManager.getAllConfigs()

    override fun getLocationsByKey(key: String): List<SignLocation>? =
        bootstrap.signManager.getLocationsByKey(key)

    override fun registerForGroup(group: String, location: Location) =
        bootstrap.signManager.registerForGroup(group, location)

    override fun registerForPersistentServer(persistentServerId: String, location: Location) =
        bootstrap.signManager.registerForPersistentServer(persistentServerId, location)

    override suspend fun removeCloudSign(location: Location) =
        bootstrap.signManager.removeCloudSign(location)

    override suspend fun removeCloudSign(location: SignLocation) =
        bootstrap.signManager.removeCloudSign(location)

    override fun exists(key: String): Boolean =
        bootstrap.signManager.exists(key)

    override fun map(location: SignLocation): Location =
        Location(
            bootstrap.plugin.server.getWorld(location.world),
            location.x,
            location.y,
            location.z
        )

    override fun unmap(location: Location): SignLocation =
        SignLocation(
            location.world.name,
            location.x,
            location.y,
            location.z
        )
}
