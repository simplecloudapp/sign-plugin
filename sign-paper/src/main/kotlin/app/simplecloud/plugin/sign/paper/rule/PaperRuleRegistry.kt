package app.simplecloud.plugin.sign.paper.rule

import app.simplecloud.plugin.sign.shared.rule.impl.DefaultRuleRegistry

class PaperRuleRegistry : DefaultRuleRegistry() {

    init {
        PaperSignRules.entries.forEach(::registerRule)
    }

}