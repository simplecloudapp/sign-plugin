package app.simplecloud.plugin.sign.shared.rule

import app.simplecloud.controller.shared.server.Server

fun interface SignRuleChecker {

    fun check(server: Server?): Boolean

}