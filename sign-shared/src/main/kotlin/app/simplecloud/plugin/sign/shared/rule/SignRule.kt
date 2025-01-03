package app.simplecloud.plugin.sign.shared.rule

import build.buf.gen.simplecloud.controller.v1.ServerState
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
interface SignRule {
    val serverState: ServerState?
    val checker: RuleChecker
    fun getRuleName(): String
}