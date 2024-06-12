package app.simplecloud.plugin.sign.shared.config

import app.simplecloud.controller.shared.server.Server
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class LayoutConfig(
    val name: String = "",
    val displayName: String = "%group%-%numerical-id%",
    val frames: List<FrameConfig> = listOf(),
) {

    fun constructName(server: Server): String {
        return displayName
            .replace("%group%", server.group)
            .replace("%numerical-id%", server.numericalId.toString())
    }

}