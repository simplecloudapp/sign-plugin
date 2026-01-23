package app.simplecloud.plugin.sign.shared.rule

import app.simplecloud.api.server.ServerState
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
interface SignRule {
    val serverState: ServerState?
    val checker: RuleChecker
    fun getRuleName(): String
}