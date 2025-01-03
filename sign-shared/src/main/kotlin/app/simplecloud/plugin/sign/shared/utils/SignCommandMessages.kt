package app.simplecloud.plugin.sign.shared.utils

object SignCommandMessages {

    const val SIGN_NOT_FOUND = "<red>You must look at a sign"
    const val SIGN_REMOVE_NOT_REGISTERED = "<red>This sign is not registered"
    const val NO_SIGNS_REGISTERED = "<red>There are no signs registered"
    const val SIGN_ALREADY_REGISTERED = "<red>This sign is already registered"
    const val SIGN_CREATE_SUCCESS = "<green>Successfully created sign for group <group>"
    const val GROUP_NOT_FOUND = "<red>The group <group> was not found"
    const val SIGN_REMOVE_SUCCESS = "<green>Successfully removed sign"
    const val SIGN_REMOVE_GROUP_NOT_REGISTERED = "<red>No signs registered for group <group>"
    const val SIGN_REMOVE_GROUP_SUCCESS = "<green>Successfully removed <amount> signs of group <group>"
    const val GENERAL_ERROR = "<red>An error occurred while processing your request"
    const val NO_PENDING_COMMAND = "<red>You have no pending command to confirm"

    const val LIST_HEADER = """
<color:#38bdf8><b>Registered Signs</b></color>
<color:#a3a3a3>Here are all the registered signs:</color>
    """

    const val CONFIRM_REMOVAL = """
<color:#f59e0b><bold>Confirmation Required</bold></color>

<color:#a3a3a3>You are about to remove</color> <color:#ef4444><bold><amount></bold></color> <color:#a3a3a3>signs from group</color> <color:#38bdf8>'<group>'</color>

<color:#a3a3a3>To proceed, click</color> <hover:show_text:'<color:#22c55e>Click to confirm removal</color>'><click:run_command:/cloudsigns confirm><color:#22c55e><bold>⚡ Confirm</bold></color></click></hover> <color:#a3a3a3>or run</color> <color:#ffffff>/cloudsigns confirm</color>
"""
    const val HELP_HEADER =
        "<color:#38bdf8><hover:show_text:'<color:#38bdf8><bold>SimpleCloud Sign Plugin</bold></color>\n\n" +
                "<color:#a3a3a3>A powerful plugin for creating and managing\n" +
                "server signs with real-time status updates\n\n" +
                "<color:#4ade80><bold>✓</bold> Real-time updates\n" +
                "<bold>✓</bold> Easy to configure\n" +
                "<bold>✓</bold> Group management</color>'><bold>⚡</bold></hover></color> " +
                "<color:#ffffff><hover:show_text:'<color:#38bdf8>Need help?</color>\n\n" +
                "<color:#a3a3a3>Type <color:#ffffff>/cloudsigns help</color> to see all available commands</color>'>Commands of Cloud Sign Plugin</hover>"

    const val HELP_ADD =
        "   <color:#a3a3a3><hover:show_text:'<color:#38bdf8><bold>Add a Sign</bold></color>\n\n" +
                "<color:#a3a3a3>Create a new server status sign by looking\n" +
                "at any sign and running the command:</color>\n\n" +
                "<color:#ffffff>/cloudsigns add <color:#4ade80><group></color></color>\n\n" +
                "<color:#cbd5e1>The sign will automatically update with\n" +
                "the current status of the specified group.</color>'><click:suggest_command:'/cloudsigns add '>/cloudsigns add <group</click></hover>"

    const val HELP_REMOVE =
        "   <color:#a3a3a3><hover:show_text:'<color:#38bdf8><bold>Remove Signs</bold></color>\n\n" +
                "<color:#a3a3a3>You can remove signs in two ways:</color>\n\n" +
                "<color:#ffffff>1. Remove a specific sign:</color>\n" +
                "<color:#cbd5e1>Look at the sign and type:</color>\n" +
                "<color:#ffffff>/cloudsigns remove</color>\n\n" +
                "<color:#ffffff>2. Remove all signs of a group:</color>\n" +
                "<color:#cbd5e1>Remove all signs from a specific group:</color>\n" +
                "<color:#ffffff>/cloudsigns remove <color:#4ade80><group></color></color>'><click:suggest_command:'/cloudsigns remove '>/cloudsigns remove [group]</click></hover>"

    const val HELP_LIST =
        "   <color:#a3a3a3><hover:show_text:'<color:#38bdf8><bold>List Signs</bold></color>\n\n" +
                "<color:#a3a3a3>View all registered signs with these commands:</color>\n\n" +
                "<color:#ffffff>1. List all signs:</color>\n" +
                "<color:#ffffff>/cloudsigns list</color>\n\n" +
                "<color:#ffffff>2. List group-specific signs:</color>\n" +
                "<color:#cbd5e1>View signs for a particular group:</color>\n" +
                "<color:#ffffff>/cloudsigns list <color:#4ade80><group></color></color>'><click:suggest_command:'/cloudsigns list '>/cloudsigns list [group]</click></hover>"

}