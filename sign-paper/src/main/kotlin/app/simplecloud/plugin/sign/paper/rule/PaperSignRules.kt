package app.simplecloud.plugin.sign.paper.rule

import app.simplecloud.plugin.sign.shared.rule.RuleChecker
import app.simplecloud.plugin.sign.shared.rule.SignRule
import build.buf.gen.simplecloud.controller.v1.ServerState
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
enum class PaperSignRules(
    override val serverState: ServerState?,
    override val checker: RuleChecker
) : SignRule {

    ;

    override fun getRuleName(): String {
        return name
    }

}