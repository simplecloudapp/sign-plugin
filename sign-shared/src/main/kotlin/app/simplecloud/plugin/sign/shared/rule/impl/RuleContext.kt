package app.simplecloud.plugin.sign.shared.rule.impl

import app.simplecloud.controller.shared.server.Server
import build.buf.gen.simplecloud.controller.v1.ServerState

open class RuleContext(
    val server: Server?,
    val serverState: ServerState?,
    val additionalData: Map<String, Any> = emptyMap()
)
