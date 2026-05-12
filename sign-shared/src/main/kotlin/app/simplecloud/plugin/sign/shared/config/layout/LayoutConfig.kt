package app.simplecloud.plugin.sign.shared.config.layout

import app.simplecloud.api.server.Server
import app.simplecloud.plugin.sign.shared.SignManager
import app.simplecloud.plugin.sign.shared.config.matcher.MatcherConfigEntry
import app.simplecloud.plugin.sign.shared.config.matcher.MatcherType
import app.simplecloud.plugin.sign.shared.rule.RuleRegistry
import app.simplecloud.plugin.sign.shared.rule.SignRule
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.serialize.SerializationException

@ConfigSerializable
data class LayoutConfig(
    val name: String = "",
    val type: LayoutType = LayoutType.ALL,
    val matcher: Map<MatcherType, List<MatcherConfigEntry>> = emptyMap(),
    @Setting("rule")
    val ruleName: String = "EMPTY",
    val priority: Int = 0,
    val serverName: String = "%server-name%",
    @Setting("behind-block")
    val behindBlock: String? = null,
    val frameUpdateInterval: Long = 500,
    val frames: List<FrameConfig> = listOf(),
) {

    val rule: SignRule
        get() = SignManager.getRuleRegistry()?.getRule(ruleName)
            ?: throw SerializationException("Rule $ruleName not found")

    companion object {
        private var ruleRegistry: RuleRegistry? = null

        fun setRegistry(registry: RuleRegistry) {
            ruleRegistry = registry
        }
    }

    fun constructName(server: Server): String {
        val baseName = when {
            server.isFromGroup -> "${server.group?.name ?: server.serverGroupId}-${server.numericalId}"
            server.isFromPersistentServer -> server.persistentServer?.name ?: server.persistentServerId ?: server.serverId
            else -> server.serverBase.name
        }
        return serverName
            .replace("%server-name%", baseName)
            .replace("%group%", server.group?.name ?: server.serverGroupId ?: "")
            .replace("%numerical-id%", server.numericalId.toString())
            .replace("%persistent-server%", server.persistentServer?.name ?: server.persistentServerId ?: "")
    }

    /**
     * Checks if this layout applies to the given server type.
     */
    fun appliesTo(server: Server?): Boolean {
        if (server == null) return type == LayoutType.ALL
        return when (type) {
            LayoutType.ALL -> true
            LayoutType.GROUP -> server.isFromGroup
            LayoutType.PERSISTENT -> server.isFromPersistentServer
        }
    }

}
