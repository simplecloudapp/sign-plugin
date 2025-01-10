package app.simplecloud.plugin.sign.shared.config.layout

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.SignManagerProvider
import app.simplecloud.plugin.sign.shared.config.rule.RuleConfig
import app.simplecloud.plugin.sign.shared.matcher.MatcherConfigEntry
import app.simplecloud.plugin.sign.shared.matcher.MatcherType
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class LayoutConfig(
    val name: String = "",
    @Setting("rule")
    val ruleName: String = "EMPTY",
    val matcher: Map<MatcherType, List<MatcherConfigEntry>> = emptyMap(),
    val serverName: String = "%group%-%numerical-id%",
    val frameUpdateInterval: Long = 500,
    val frames: List<FrameConfig> = listOf(),
) {

    val rule: RuleConfig
        get() = SignManagerProvider.get().getRule(ruleName)
            ?: RuleConfig(name = "UNKNOWN_RULE")

    fun constructName(server: Server): String {
        return serverName
            .replace("%group%", server.group)
            .replace("%numerical-id%", server.numericalId.toString())
    }

}