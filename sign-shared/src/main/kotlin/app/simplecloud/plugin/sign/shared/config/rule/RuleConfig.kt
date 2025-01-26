package app.simplecloud.plugin.sign.shared.config.rule

import app.simplecloud.plugin.sign.shared.matcher.MatcherConfigEntry
import app.simplecloud.plugin.sign.shared.matcher.MatcherType
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class RuleConfig(
    val name: String = "",
    val inherit: String? = null,
    val matcher: Map<MatcherType, List<MatcherConfigEntry>> = emptyMap(),
)
