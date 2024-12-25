package app.simplecloud.plugin.sign.shared.config.layout

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.rule.SignRule
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class LayoutConfig(
    val group: String = "",
    val name: String = "",
    val rule: SignRule = SignRule.EMPTY,
    val priority: Int = 0,
    val displayName: String = "%group%-%numerical-id%",
    val frameUpdateInterval: Long = 500,
    val frames: List<FrameConfig> = listOf(),
) {

    fun constructName(server: Server): String {
        return displayName
            .replace("%group%", server.group)
            .replace("%numerical-id%", server.numericalId.toString())
    }
}