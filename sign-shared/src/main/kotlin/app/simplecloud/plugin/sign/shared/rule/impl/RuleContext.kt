package app.simplecloud.plugin.sign.shared.rule.impl

import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState

open class RuleContext(
    val server: Server?,
    val serverState: ServerState?,
    val additionalData: Map<String, Any> = emptyMap()
)
