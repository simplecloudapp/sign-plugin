package app.simplecloud.plugin.sign.shared.config.location

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SignLocationConfig(
    val world: String = "",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
)
