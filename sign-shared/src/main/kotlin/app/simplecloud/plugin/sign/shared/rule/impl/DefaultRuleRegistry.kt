package app.simplecloud.plugin.sign.shared.rule.impl

import app.simplecloud.plugin.sign.shared.rule.RuleRegistry
import app.simplecloud.plugin.sign.shared.rule.SignRule

open class DefaultRuleRegistry : RuleRegistry {

    private val rules = mutableListOf<SignRule>()

    init {
        DefaultSignRules.entries.forEach(::registerRule)
    }

    override fun registerRule(rule: SignRule) {
        if (!hasRule(rule)) {
            rules.add(rule)
        }
    }

    override fun getRules(): List<SignRule> = rules.toList()

    override fun clearRules() {
        rules.clear()
    }

    override fun hasRule(rule: SignRule): Boolean {
        return rules.contains(rule)
    }

    override fun getRule(name: String): SignRule? {
        return rules.find { it.getRuleName() == name }
    }

}