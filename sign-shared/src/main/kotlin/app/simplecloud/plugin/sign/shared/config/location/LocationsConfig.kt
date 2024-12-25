package app.simplecloud.plugin.sign.shared.config.location

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class LocationsConfig(
    val group: String = "",
    val locations: List<SignLocation> = emptyList()
)
