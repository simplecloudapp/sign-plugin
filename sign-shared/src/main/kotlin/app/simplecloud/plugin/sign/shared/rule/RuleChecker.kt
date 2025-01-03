package app.simplecloud.plugin.sign.shared.rule

import app.simplecloud.plugin.sign.shared.rule.impl.RuleContext

interface RuleChecker {

    fun check(context: RuleContext): Boolean

}