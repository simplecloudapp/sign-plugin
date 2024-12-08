package app.simplecloud.plugin.sign.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class LocationsConfig(
    val serverGroup: String = "",
    val locations: List<Map<String, String>> = emptyList(),
)