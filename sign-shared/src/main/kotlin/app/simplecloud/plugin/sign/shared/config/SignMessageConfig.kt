package app.simplecloud.plugin.sign.shared.config

class SignMessageConfig {

    companion object {
        private const val PREFIX = "<color:#38bdf8><bold>⚡</bold></color>"

        const val GROUP_NOT_FOUND =
            "$PREFIX <color:#dc2626>There is no group named <color:#a3a3a3>'</color><color:#fbbf24><group></color><color:#a3a3a3>'</color>"
        const val SIGN_NOT_FOUND =
            "$PREFIX <color:#dc2626>To execute this command, you need to be looking at a sign</color>"
        const val SIGN_ALREADY_REGISTERED = "$PREFIX <color:#dc2626>Sign is already registered"
        const val SIGN_CREATE_SUCCESS =
            "$PREFIX <color:#a3e635>Sign for group <color:#a3a3a3>'</color><color:#fbbf24><group></color><color:#a3a3a3>'</color></color> <color:#a3e635>was successfully created"
        const val SIGN_REMOVE_NOT_REGISTERED = "$PREFIX <color:#dc2626>Sign is not registered as a CloudSign"
        const val SIGN_REMOVE_SUCCESS =
            "$PREFIX <color:#a3e635>CloudSign successfully removed"
        const val SIGN_REMOVE_GROUP_NOT_REGISTERED =
            "$PREFIX <color:#dc2626>No Signs were found for group <color:#a3a3a3>'</color><color:#fbbf24><group></color><color:#a3a3a3>'</color>"
        const val SIGN_REMOVE_GROUP_SUCCESS =
            "$PREFIX <color:#a3e635>Successfully removed <color:#fbbf24><amount></color><color:#a3e635> CloudSign(s) for group <color:#a3a3a3>'</color><color:#fbbf24><group></color><color:#a3a3a3>'</color>"
    }

}