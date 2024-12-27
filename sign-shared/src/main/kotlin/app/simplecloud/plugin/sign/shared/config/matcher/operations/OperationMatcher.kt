package app.simplecloud.plugin.sign.shared.config.matcher.operations

interface OperationMatcher {

    fun matches(key: String, value: String): Boolean

}