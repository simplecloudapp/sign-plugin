package app.simplecloud.plugin.sign.shared.utils

enum class SignCommandPermission(val node: String) {

    BASE("simplecloud.command.signs"),
    LIST("${BASE.node}.list"),
    TP("${BASE.node}.tp"),
    ADD("${BASE.node}.add"),
    REMOVE("${BASE.node}.remove"),
    REMOVE_GROUP("${REMOVE.node}.group");

    companion object {
        fun fromPermission(permission: String): SignCommandPermission? =
            entries.find { it.node == permission }
    }
}