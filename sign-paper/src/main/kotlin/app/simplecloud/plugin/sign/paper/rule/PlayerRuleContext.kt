package app.simplecloud.plugin.sign.paper.rule

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.rule.impl.RuleContext
import build.buf.gen.simplecloud.controller.v1.ServerState
import org.bukkit.entity.Player

class PlayerRuleContext(
    server: Server?,
    serverState: ServerState?,
    val player: Player,
    additionalData: Map<String, Any> = emptyMap()
) : RuleContext(
    server = server,
    serverState = serverState,
    additionalData = additionalData + mapOf(
        "playerId" to player.uniqueId.toString(),
        "playerName" to player.name,
        "playerServer" to player.server.name
    )
)
