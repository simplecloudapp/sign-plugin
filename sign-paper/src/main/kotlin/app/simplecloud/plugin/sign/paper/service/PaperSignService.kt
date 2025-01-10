package app.simplecloud.plugin.sign.paper.service

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.plugin.sign.paper.PaperSignsPluginBootstrap
import app.simplecloud.plugin.sign.shared.CloudSign
import app.simplecloud.plugin.sign.shared.LocationMapper
import app.simplecloud.plugin.sign.shared.config.location.SignLocationConfig
import app.simplecloud.plugin.sign.shared.config.rule.RuleConfig
import app.simplecloud.plugin.sign.shared.service.SignService
import org.bukkit.Location

class PaperSignService(private val bootstrap: PaperSignsPluginBootstrap) : SignService<Location>,
    LocationMapper<Location> {

    override val controllerApi: ControllerApi.Coroutine
        get() = bootstrap.signManager.controllerApi

    override fun getCloudSign(location: Location): CloudSign<Location>? =
        bootstrap.signManager.getCloudSign(location)

    override fun getAllLocations(): List<SignLocationConfig> =
        bootstrap.signManager.getAllLocations()

    override fun getAllGroupsRegistered(): List<String> =
        bootstrap.signManager.getAllGroupsRegistered()

    override fun getLocationsByGroup(group: String): List<SignLocationConfig>? =
        bootstrap.signManager.getLocationsByGroup(group)

    override fun register(group: String, location: Location) =
        bootstrap.signManager.register(group, location)

    override suspend fun removeCloudSign(location: Location) =
        bootstrap.signManager.removeCloudSign(location)

    override fun exists(group: String): Boolean =
        bootstrap.signManager.exists(group)

    override fun getAllRules(): List<RuleConfig> =
        bootstrap.signManager.getAllRules()

    override fun getRule(ruleName: String): RuleConfig? =
        bootstrap.signManager.getRule(ruleName)

    override fun map(location: SignLocationConfig): Location =
        Location(
            bootstrap.plugin.server.getWorld(location.world),
            location.x,
            location.y,
            location.z
        )

    override fun unmap(location: Location): SignLocationConfig =
        SignLocationConfig(
            location.world.name,
            location.x,
            location.y,
            location.z
        )
}