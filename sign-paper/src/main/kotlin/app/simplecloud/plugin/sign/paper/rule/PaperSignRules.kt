package app.simplecloud.plugin.sign.paper.rule

import app.simplecloud.plugin.sign.shared.rule.RuleChecker
import app.simplecloud.plugin.sign.shared.rule.SignRule
import app.simplecloud.plugin.sign.shared.rule.impl.RuleContext
import build.buf.gen.simplecloud.controller.v1.ServerState
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
enum class PaperSignRules(
    override val serverState: ServerState?,
    override val checker: RuleChecker
) : SignRule {

    CURRENT_SERVER(
        ServerState.AVAILABLE,
        object : RuleChecker {
            override fun check(context: RuleContext): Boolean {
                val server = context.server ?: return false
                if (server.state != ServerState.AVAILABLE) return false

                val playerServer = context.additionalData["playerServer"] as? String
                    ?: return false

                return "${server.group}-${server.numericalId}" == playerServer
            }
        }
    );

    override fun getRuleName(): String {
        return name
    }

}