package app.simplecloud.plugin.sign.shared.config.layout

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.SignManagerProvider
import app.simplecloud.plugin.sign.shared.config.rule.RuleConfig
import app.simplecloud.plugin.sign.shared.rule.context.RuleContext
import app.simplecloud.plugin.sign.shared.utils.MatcherUtil
import kotlinx.coroutines.runBlocking
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class LayoutConfig(
    val name: String = "",
    @Setting("rule")
    val ruleName: String = "",
    val priority: Int = 0,
    val serverName: String = "<group_name>-<numerical_id>",
    val frameUpdateInterval: Long = 500,
    val frames: List<FrameConfig> = listOf(),
) {

    val rule: RuleConfig
        get() = SignManagerProvider.get().getRule(ruleName)
            ?: RuleConfig(name = "UNKNOWN_RULE")

    fun matches(ruleContext: RuleContext): Boolean {
        return runBlocking {
            rule.inherit?.let { inheritRuleName ->
                SignManagerProvider.get().getRule(inheritRuleName)?.let { inheritRule ->
                    MatcherUtil.matches(inheritRule, ruleContext)
                }
            }

            MatcherUtil.matches(rule, ruleContext)
        }
    }

    fun constructName(server: Server): String {
        return serverName
            .replace("<group_name>", server.group)
            .replace("<numerical_id>", server.numericalId.toString())
    }

}