package app.simplecloud.plugin.sign.shared.rule.serialize

import app.simplecloud.plugin.sign.shared.rule.RuleRegistry
import app.simplecloud.plugin.sign.shared.rule.SignRule
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class SignRuleSerializer(private val ruleRegistry: RuleRegistry) : TypeSerializer<SignRule> {

    override fun deserialize(type: Type, node: ConfigurationNode): SignRule {
        val ruleName = node.get(String::class.java) ?: throw SerializationException("Rule name must be specified.")

        return ruleRegistry.getRule(ruleName) ?: throw SerializationException("Rule $ruleName not found.")
    }

    override fun serialize(type: Type, signRule: SignRule?, node: ConfigurationNode) {
        if (signRule == null) {
            node.set(null)
            return
        }

        node.set(signRule.getRuleName())
    }
}