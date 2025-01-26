package app.simplecloud.plugin.sign.shared.matcher

import app.simplecloud.plugin.api.shared.matcher.OperationType
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MatcherConfigEntry(
    val operation: OperationType = OperationType.STARTS_WITH,
    val key: String = "",
    val value: String = "",
    val negate: Boolean = false
)
