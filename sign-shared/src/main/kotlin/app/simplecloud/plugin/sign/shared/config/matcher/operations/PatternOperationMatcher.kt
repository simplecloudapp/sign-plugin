package app.simplecloud.plugin.sign.shared.config.matcher.operations

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.regex.Pattern

@ConfigSerializable
class PatternOperationMatcher : OperationMatcher {

    override fun matches(key: String, value: String): Boolean {
        return Pattern.matches(value, key)
    }

}