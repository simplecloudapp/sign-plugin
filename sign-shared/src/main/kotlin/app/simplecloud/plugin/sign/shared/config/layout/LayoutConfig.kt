package app.simplecloud.plugin.sign.shared.config.layout

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.SignManager
import app.simplecloud.plugin.sign.shared.config.matcher.MatcherConfigEntry
import app.simplecloud.plugin.sign.shared.config.matcher.MatcherType
import app.simplecloud.plugin.sign.shared.rule.RuleRegistry
import app.simplecloud.plugin.sign.shared.rule.SignRule
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.serialize.SerializationException

@ConfigSerializable
data class LayoutConfig(
    val name: String = "",
    val matcher: Map<MatcherType, List<MatcherConfigEntry>> = emptyMap(),
    @Setting("rule")
    val ruleName: String = "EMPTY",
    val priority: Int = 0,
    val serverName: String = "%group%-%numerical-id%",
    val frameUpdateInterval: Long = 500,
    val frames: List<FrameConfig> = listOf(),
) {

    val rule: SignRule
        get() = SignManager.getRuleRegistry()?.getRule(ruleName)
            ?: throw SerializationException("Rule $ruleName not found")

    companion object {
        private var ruleRegistry: RuleRegistry? = null

        fun setRegistry(registry: RuleRegistry) {
            ruleRegistry = registry
        }
    }

    fun constructName(server: Server): String {
        return serverName
            .replace("%group%", server.group)
            .replace("%numerical-id%", server.numericalId.toString())
    }

}