package app.simplecloud.plugin.sign.shared.rule

/**
 * Registry for managing SignRules.
 * This interface allows different platforms to register and manage their own rules
 * while maintaining compatibility with the shared sign system.
 */
interface RuleRegistry {
    /**
     * Registers a new rule to the registry
     * @param rule The SignRule to register
     */
    fun registerRule(rule: SignRule)

    /**
     * Retrieves all registered rules
     * @return List of all registered SignRules
     */
    fun getRules(): List<SignRule>

    /**
     * Clears all registered rules
     */
    fun clearRules()

    /**
     * Checks if a rule is already registered
     * @param rule The SignRule to check
     * @return true if the rule is already registered, false otherwise
     */
    fun hasRule(rule: SignRule): Boolean

    /**
     * Gets a rule by its name
     * @param name The name of the rule to get
     * @return The SignRule if found, null otherwise
     */
    fun getRule(name: String): SignRule?
}