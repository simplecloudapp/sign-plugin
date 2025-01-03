package app.simplecloud.plugin.sign.shared.rule.impl

import app.simplecloud.plugin.sign.shared.rule.RuleChecker
import app.simplecloud.plugin.sign.shared.rule.SignRule
import build.buf.gen.simplecloud.controller.v1.ServerState
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
enum class DefaultSignRules(
    override val serverState: ServerState?, override val checker: RuleChecker
) : SignRule {

    STARTING(
        ServerState.STARTING, object : RuleChecker {
            override fun check(context: RuleContext): Boolean {
                return context.server?.state == ServerState.STARTING
            }
        }),

    OFFLINE(
        null, object : RuleChecker {
            override fun check(context: RuleContext): Boolean {
                return context.server == null
            }
        }),

    MAINTENANCE(
        ServerState.AVAILABLE, object : RuleChecker {
            override fun check(context: RuleContext): Boolean {
                return context.server?.properties?.getOrDefault("maintenance", false) == true
            }
        }),

    FULL(
        ServerState.AVAILABLE, object : RuleChecker {
            override fun check(context: RuleContext): Boolean {
                val server = context.server ?: return false
                return server.playerCount == server.maxPlayers
            }
        }),

    EMPTY(
        ServerState.AVAILABLE, object : RuleChecker {
            override fun check(context: RuleContext): Boolean {
                val server = context.server ?: return false
                return server.playerCount == server.maxPlayers
            }
        }),

    ONLINE(
        ServerState.AVAILABLE, object : RuleChecker {
            override fun check(context: RuleContext): Boolean {
                return context.server?.state == ServerState.AVAILABLE
            }
        });

    override fun getRuleName(): String = name
}