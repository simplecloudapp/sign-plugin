package app.simplecloud.plugin.sign.shared.utils

object SignCommandMessages {

    private const val PREFIX = "<color:#38bdf8><bold>⚡</bold></color>"

    const val SIGN_NOT_FOUND =
        "$PREFIX <color:#dc2626>To execute this command, you need to look at a sign.</color>"
    const val SIGN_REMOVE_NOT_REGISTERED =
        "$PREFIX <color:#dc2626>This sign is not registered.</color>"
    const val NO_SIGNS_REGISTERED =
        "$PREFIX <color:#dc2626>There are no signs registered.</color>"
    const val SIGN_ALREADY_REGISTERED =
        "$PREFIX <color:#dc2626>This sign is already registered.</color>"
    const val SIGN_CREATE_SUCCESS =
        "$PREFIX <color:#ffffff>Sign for group <color:#fbbf24><group></color> was <color:#a3e635>created</color><color:#ffffff>.</color>"
    const val GROUP_NOT_FOUND =
        "$PREFIX <color:#dc2626>There is no group named <color:#fbbf24><group></color><color:#dc2626>.</color>"
    const val PERSISTENT_SERVER_NOT_FOUND =
        "$PREFIX <color:#dc2626>There is no persistent server named <color:#fbbf24><persistent-server></color><color:#dc2626>.</color>"
    const val SIGN_REMOVE_SUCCESS =
        "$PREFIX <color:#a3e635>Sign was successfully removed.</color>"
    const val SIGN_REMOVE_GROUP_NOT_REGISTERED =
        "$PREFIX <color:#dc2626>No signs are registered for group <color:#fbbf24><group></color><color:#dc2626>.</color>"
    const val SIGN_REMOVE_GROUP_SUCCESS =
        "$PREFIX <color:#ffffff>Successfully removed <color:#fbbf24><amount></color> sign(s) of group <color:#fbbf24><group></color><color:#ffffff>.</color>"
    const val GENERAL_ERROR =
        "$PREFIX <color:#dc2626>An error occurred while processing your request.</color>"
    const val NO_PENDING_COMMAND =
        "$PREFIX <color:#dc2626>You have no pending command to confirm.</color>"

    const val LIST_HEADER = """
$PREFIX <color:#ffffff>Registered Signs</color>
<color:#a3a3a3>Here are all registered signs:</color>
    """

    const val CONFIRM_REMOVAL = """
$PREFIX <color:#ffffff>Please confirm the following action with</color> <color:#fbbf24>/cloudsigns confirm</color>
   <color:#a3a3a3>Deleting group <group> (<amount> sign(s))</color>
"""

    const val HELP_HEADER = "$PREFIX <color:#ffffff>Commands of Cloud Sign Plugin</color>"
    const val HELP_ADD = "   <color:#a3a3a3>/cloudsigns add group [group]</color>"
    const val HELP_ADD_PERSISTENT = "   <color:#a3a3a3>/cloudsigns add persistent [name]</color>"
    const val HELP_REMOVE = "   <color:#a3a3a3>/cloudsigns remove [group|persistent]</color>"
    const val HELP_LIST = "   <color:#a3a3a3>/cloudsigns list [page] [group]</color>"

}
