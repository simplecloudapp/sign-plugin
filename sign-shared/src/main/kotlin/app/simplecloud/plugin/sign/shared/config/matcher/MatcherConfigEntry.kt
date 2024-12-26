package app.simplecloud.plugin.sign.shared.config.matcher

import app.simplecloud.plugin.sign.shared.config.matcher.operations.MatcherOperations
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MatcherConfigEntry(
    val operation: MatcherOperations = MatcherOperations.STARTS_WITH,
    val value: String = "",
)
