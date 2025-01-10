package app.simplecloud.plugin.sign.shared.rule.context.impl

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.rule.context.RuleContext

class ServerRuleContext(
    val server: Server? = null,
    override val additionalData: Map<String, Any> = emptyMap()
) : RuleContext