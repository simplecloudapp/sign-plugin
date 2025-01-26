package app.simplecloud.plugin.sign.shared.rule.impl

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.rule.RuleContext

class ServerRuleContext(
    val server: Server? = null,
    override val additionalData: Map<String, Any> = emptyMap()
) : RuleContext