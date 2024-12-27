package app.simplecloud.plugin.sign.shared.rule

import build.buf.gen.simplecloud.controller.v1.ServerState

enum class SignRule(
    val serverState: ServerState?,
    val checker: SignRuleChecker = SignRuleChecker { server -> server?.state == serverState }
) {

    STARTING(ServerState.STARTING),
    OFFLINE(null, { it == null }),
    MAINTENANCE(ServerState.AVAILABLE, { it?.properties?.getOrDefault("maintenance", false) == true }),
    FULL(ServerState.AVAILABLE, { it?.playerCount == it?.maxPlayers }),
    EMPTY(ServerState.AVAILABLE, { (it?.playerCount ?: 0L) == 0L }),
    ONLINE(ServerState.AVAILABLE);

    companion object {
        fun hasRule(serverState: ServerState?): Boolean {
            return entries.any { it.serverState == serverState }
        }
    }
}