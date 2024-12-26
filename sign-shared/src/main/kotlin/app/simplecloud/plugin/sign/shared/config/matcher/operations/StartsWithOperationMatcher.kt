package app.simplecloud.plugin.sign.shared.config.matcher.operations

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
class StartsWithOperationMatcher : OperationMatcher {

    override fun matches(key: String, value: String): Boolean {
        return key.startsWith(value)
    }

}