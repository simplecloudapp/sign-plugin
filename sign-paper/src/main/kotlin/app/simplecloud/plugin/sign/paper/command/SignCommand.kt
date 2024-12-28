package app.simplecloud.plugin.sign.paper.command

import app.simplecloud.plugin.sign.paper.PaperSignsPlugin
import app.simplecloud.plugin.sign.paper.PaperSignsPluginBootstrap
import app.simplecloud.plugin.sign.paper.dispatcher.BukkitMainDispatcher
import app.simplecloud.plugin.sign.shared.config.location.SignLocation
import build.buf.gen.simplecloud.controller.v1.ServerType
import com.google.common.cache.CacheBuilder
import io.grpc.StatusException
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.entity.Player
import org.incendo.cloud.Command
import org.incendo.cloud.description.Description
import org.incendo.cloud.paper.util.sender.Source
import org.incendo.cloud.parser.standard.IntegerParser
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.processors.cache.GuavaCache
import org.incendo.cloud.processors.confirmation.ConfirmationManager
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

object SignMessages {
    const val SIGN_NOT_FOUND = "<red>You must look at a sign!"
    const val NO_SIGNS_REGISTERED = "<red>There are no signs registered!"
    const val SIGN_ALREADY_REGISTERED = "<red>This sign is already registered!"
    const val SIGN_CREATE_SUCCESS = "<green>Successfully created sign for group <group>!"
    const val GROUP_NOT_FOUND = "<red>The group <group> was not found!"
    const val SIGN_REMOVE_SUCCESS = "<green>Successfully removed sign!"
    const val SIGN_REMOVE_NOT_REGISTERED = "<red>This sign is not registered!"
    const val SIGN_REMOVE_GROUP_NOT_REGISTERED = "<red>No signs registered for group <group>!"
    const val SIGN_REMOVE_GROUP_SUCCESS = "<green>Successfully removed <amount> signs of group <group>!"
    const val GENERAL_ERROR = "<red>An error occurred while processing your request."
    const val NO_PENDING_COMMAND = "<red>You have no pending command to confirm!"

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

class SignCommand(private val bootstrap: PaperSignsPluginBootstrap) {
    private val logger = LoggerFactory.getLogger(SignCommand::class.java)
    private val commandScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val miniMessage = MiniMessage.miniMessage()
    private var confirmationManager: ConfirmationManager<Source>? = null

    private val messageCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, Component>()

    companion object {
        private const val LOCATIONS_PER_PAGE = 5
        private const val MAX_SIGN_DISTANCE = 8
        private val COMMAND_NAMES = arrayOf(
            "simplecloudsigns",
            "simplecloudsign",
            "scsigns",
            "scsign",
            "cloudsigns",
            "cloudsign",
            "signs",
            "sign"
        )
    }

    fun register() {
        bootstrap.commandManager.command(
            createBaseCommand().also { registerCommands(it) }.build()
        )
    }

    private fun registerCommands(baseCommand: Command.Builder<Source>) {
        bootstrap.commandManager.apply {
            listOf(
                createHelpCommand(baseCommand),
                createListCommand(baseCommand),
                createAddCommand(baseCommand),
                createRemoveCommand(baseCommand),
                createRemoveGroupCommand(baseCommand),
            ).forEach { command(it) }
        }
    }

    private fun createBaseCommand() = bootstrap.commandManager
        .commandBuilder(COMMAND_NAMES[0], *COMMAND_NAMES)
        .senderType(Source::class.java)
        .commandDescription(Description.of("SimpleCloud Sign Plugin!"))
        .permission(SignPermission.BASE.node)
        .handler { printHelp(it.sender()) }

    private fun createHelpCommand(baseCommand: Command.Builder<Source>) =
        baseCommand.literal("help")
            .senderType(Source::class.java)
            .commandDescription(Description.of("SimpleCloud Sign Plugin!"))
            .permission(SignPermission.BASE.node)
            .handler { printHelp(it.sender()) }

    private fun createListCommand(baseCommand: Command.Builder<Source>) =
        baseCommand.literal("list")
            .commandDescription(Description.of("List Registered CloudSigns"))
            .optional(
                "group",
                StringParser.stringParser(),
                Description.of("The name of the Target group"),
                registeredGroupSuggestions()
            ).optional(
                "page",
                IntegerParser.integerParser(),
            )
            .permission(SignPermission.LIST.node)
            .handler { context ->
                (context.sender().source() as? Player)?.let { player ->
                    val group = context.getOrDefault("group", "")
                    val page = context.getOrDefault("page", 1)
                    handleListCommand(player, group, page)
                }
            }

    private fun createAddCommand(baseCommand: Command.Builder<Source>) =
        baseCommand.literal("add")
            .commandDescription(Description.of("Register a new CloudSign"))
            .required(
                "group",
                StringParser.stringParser(),
                Description.of("The name of the Target group"),
                groupSuggestions()
            )
            .permission(SignPermission.ADD.node)
            .handler { context ->
                (context.sender().source() as? Player)?.let { player ->
                    handleSignOperation(player, context.getOrDefault("group", "")) { location, group ->
                        executeAddSign(location, group)
                    }
                }
            }

    private fun createRemoveCommand(baseCommand: Command.Builder<Source>) =
        baseCommand.literal("remove")
            .commandDescription(Description.of("Unregister a SimpleCloud Sign"))
            .permission(SignPermission.REMOVE.node)
            .handler { context ->
                (context.sender().source() as? Player)?.let { player ->
                    handleSignOperation(player) { location, _ ->
                        executeRemoveSign(location)
                    }
                }
            }

    private fun createRemoveGroupCommand(baseCommand: Command.Builder<Source>) =
        baseCommand.literal("remove")
            .commandDescription(Description.of("Unregister all signs for a group"))
            .required(
                "group",
                StringParser.stringParser(),
                Description.of("The name of the Targeted group"),
                registeredGroupSuggestions()
            )
            .permission(SignPermission.REMOVE_GROUP.node)
            .meta(ConfirmationManager.META_CONFIRMATION_REQUIRED, false)
            .handler { context ->
                (context.sender().source() as? Player)?.let { player ->
                    handleRemoveGroupCommand(player, context.getOrDefault("group", ""))
                }
            }

    private fun createConfirmCommand(baseCommand: Command.Builder<Source>) =
        baseCommand.literal("confirm")
            .senderType(Source::class.java)
            .permission(SignPermission.REMOVE_GROUP.node)
            .handler(confirmationManager!!.createExecutionHandler())

    private fun createConfirmationManager() =
        ConfirmationManager.confirmationManager<Source> { builder ->
            builder
                .cache(
                    GuavaCache.of(
                        CacheBuilder.newBuilder()
                            .expireAfterWrite(30, TimeUnit.SECONDS)
                            .build()
                    )
                )
                .noPendingCommandNotifier { sender ->
                    sender.source().sendMessage(
                        getCachedMessage(SignMessages.NO_PENDING_COMMAND)
                    )
                }
                .confirmationRequiredNotifier { sender, context ->
                    val group = context.commandContext().get<String>("group")
                    val locations = bootstrap.signManager.getLocationsByGroup(group)
                    val amount = locations?.size ?: 0

                    sender.source().sendMessage(
                        getCachedMessage(
                            SignMessages.CONFIRM_REMOVAL,
                            "amount" to amount.toString(),
                            "group" to group
                        )
                    )
                }
        }

    private fun handleListCommand(player: Player, group: String, page: Int = 1) {
        commandScope.launch {
            runCatching {
                when {
                    group.isBlank() -> listAllSigns(player, page)
                    else -> listGroupSigns(player, group, page)
                }
            }.onFailure {
                logger.error("Error while listing $group")
                sendMessage(player, SignMessages.GENERAL_ERROR)
            }
        }
    }

    private fun listAllSigns(player: Player, page: Int) {
        val allSigns = bootstrap.signManager.getAllGroupsRegistered()
        if (allSigns.isEmpty()) {
            sendMessage(player, SignMessages.NO_SIGNS_REGISTERED)
            return
        }

        sendMessage(player, SignMessages.LIST_HEADER)
        allSigns.forEach {
            val locations = bootstrap.signManager.getLocationsByGroup(it) ?: return@forEach
            sendSignGroupInformation(player, it, locations, page)
        }
    }

    private fun listGroupSigns(player: Player, group: String, page: Int) {
        val locations = bootstrap.signManager.getLocationsByGroup(group)
        if (locations.isNullOrEmpty()) {
            sendMessage(player, SignMessages.SIGN_REMOVE_GROUP_NOT_REGISTERED, "group" to group)
            return
        }

        sendMessage(player, SignMessages.LIST_HEADER)
        sendSignGroupInformation(player, group, locations, page)
    }

    private fun sendSignGroupInformation(player: Player, group: String, locations: List<SignLocation>, page: Int) {
        val startIndex = (page - 1) * LOCATIONS_PER_PAGE
        val endIndex = minOf(startIndex + LOCATIONS_PER_PAGE, locations.size)
        val totalPages = ceil(locations.size.toDouble() / LOCATIONS_PER_PAGE)

        val paginatedLocations = locations.subList(startIndex, endIndex)
        val locationInformation = paginatedLocations.joinToString(
            separator = "\n"
        ) { location ->
            """<click:run_command:/minecraft:teleport @s ${location.x} ${location.y} ${location.z}><hover:show_text:'Click to teleport'><color:#a3a3a3>- World: <color:#ffffff>${location.world}</color>, X: <color:#4ade80>${location.x}</color>, Y: <color:#4ade80>${location.y}</color>, Z: <color:#4ade80>${location.z}</color>, Yaw: <color:#4ade80>${location.yaw}</color>, Pitch: <color:#4ade80>${location.pitch}</color></color></hover></click>
            """.trimIndent()
        }

        val navigationButtons = buildString {
            if (page > 1) {
                append("<click:run_command:/sign list $group ${page - 1}><color:#38bdf8>[Previous]</color></click> ")
            }

            append("<color:#ffffff>Page $page/${totalPages.toInt()}</color>")

            if (page < totalPages) {
                append(" <click:run_command:/sign list $group ${page + 1}><color:#38bdf8>[Next]</color></click>")
            }
        }

        sendMessage(
            player,
            """
<color:#38bdf8><bold>Group:</bold> <color:#ffffff>$group</color></color> <gray>(${locations.size} signs)</gray>
$locationInformation

$navigationButtons""".trimIndent()
        )
    }

    private inline fun handleSignOperation(
        player: Player,
        group: String = "",
        crossinline operation: suspend (Location, String) -> CommandResult
    ) {
        val signLocation = getSignLocation(player) ?: run {
            sendMessage(player, SignMessages.SIGN_NOT_FOUND)
            return
        }

        commandScope.launch {
            try {
                executeCommand(player) { operation(signLocation, group) }
            } catch (e: Exception) {
                logger.error("Error executing sign operation", e)
                sendMessage(player, SignMessages.GENERAL_ERROR)
            }
        }
    }

    private fun handleRemoveGroupCommand(player: Player, group: String) {
        if (group.isBlank() || !bootstrap.signManager.exists(group)) {
            sendMessage(
                player,
                SignMessages.SIGN_REMOVE_GROUP_NOT_REGISTERED,
                "group" to group
            )
            return
        }

        commandScope.launch {
            try {
                executeCommand(player) { executeRemoveGroupSigns(group) }
            } catch (e: Exception) {
                logger.error("Error removing group signs", e)
                sendMessage(player, SignMessages.GENERAL_ERROR)
            }
        }
    }

    private suspend fun executeAddSign(location: Location, group: String): CommandResult {
        return runCatching {
            if (bootstrap.signManager.getCloudSign(location) != null) {
                return CommandResult.Error(SignMessages.SIGN_ALREADY_REGISTERED)
            }

            bootstrap.getControllerAPI().getGroups().getGroupByName(group)
            bootstrap.signManager.register(group, location)
            CommandResult.Success(mapOf("group" to group))
        }.getOrElse { e ->
            when (e) {
                is StatusException -> CommandResult.Error(SignMessages.GROUP_NOT_FOUND)
                else -> {
                    logger.error("Error registering sign", e)
                    CommandResult.Error(SignMessages.GENERAL_ERROR)
                }
            }
        }
    }

    private suspend fun executeRemoveSign(location: Location): CommandResult {
        return runCatching {
            val cloudSign = bootstrap.signManager.getCloudSign(location)
                ?: return CommandResult.Error(SignMessages.SIGN_REMOVE_NOT_REGISTERED)

            bootstrap.signManager.removeCloudSign(cloudSign.location)
            clearSign(cloudSign.location)

            CommandResult.Success()
        }.getOrElse { e ->
            logger.error("Error removing sign", e)
            CommandResult.Error(SignMessages.GENERAL_ERROR)
        }
    }

    private suspend fun executeRemoveGroupSigns(group: String): CommandResult {
        return runCatching {
            val locations = bootstrap.signManager.getLocationsByGroup(group)
                ?.takeIf { it.isNotEmpty() }
                ?: return CommandResult.Error(
                    SignMessages.SIGN_REMOVE_GROUP_NOT_REGISTERED.replace("<group>", group)
                )

            val amount = locations.size

            // First remove all signs from the manager
            locations.forEach { location ->
                val mapLocation = bootstrap.signManager.mapLocation(location)
                bootstrap.signManager.removeCloudSign(mapLocation)
            }

            // Then clear all sign blocks in the world
            withContext(BukkitMainDispatcher(PaperSignsPlugin.instance)) {
                locations.forEach { location ->
                    val mapLocation = bootstrap.signManager.mapLocation(location)
                    clearSign(mapLocation)
                }
            }

            CommandResult.Success(mapOf("amount" to amount))
        }.getOrElse { e ->
            logger.error("Error removing group signs", e)
            CommandResult.Error(SignMessages.GENERAL_ERROR)
        }
    }

    private suspend inline fun executeCommand(
        player: Player,
        crossinline action: suspend () -> CommandResult
    ) {
        when (val result = action()) {
            is CommandResult.Success -> {
                val message = when {
                    result.data.containsKey("group") -> SignMessages.SIGN_CREATE_SUCCESS
                    result.data.containsKey("amount") -> SignMessages.SIGN_REMOVE_GROUP_SUCCESS
                    else -> SignMessages.SIGN_REMOVE_SUCCESS
                }
                sendMessage(player, message, *result.data.toArray())
            }

            is CommandResult.Error -> sendMessage(player, result.message)
        }
    }

    private suspend fun clearSign(location: Location) {
        withContext(BukkitMainDispatcher(PaperSignsPlugin.instance)) {
            location.block.takeIf { it.isSign() }?.let { block ->
                (block.state as? Sign)?.apply {
                    clearSignText()
                    update(true)
                }
            }
        }
    }

    private fun Block.isSign() =
        type.name.endsWith("_SIGN") || type.name.endsWith("_WALL_SIGN")

    private fun Sign.clearSignText() {
        val empty = Component.text("")
        arrayOf(Side.FRONT, Side.BACK).forEach { side ->
            getSide(side).apply {
                lines().indices.forEach { i -> line(i, empty) }
            }
        }
    }

    private fun getSignLocation(player: Player): Location? =
        player.getTargetBlock(null, MAX_SIGN_DISTANCE).location
            .takeIf { validateSignLocation(it) }

    private fun validateSignLocation(location: Location): Boolean =
        location.world?.let { location.block.state is Sign } ?: false

    private fun registeredGroupSuggestions(): BlockingSuggestionProvider<Source?> =
        BlockingSuggestionProvider { _, _ ->
            bootstrap.signManager.getAllGroupsRegistered()
                .map { Suggestion.suggestion(it) }
        }

    private fun groupSuggestions(): BlockingSuggestionProvider<Source?> =
        BlockingSuggestionProvider { _, _ ->
            runBlocking {
                bootstrap.getControllerAPI().getGroups().getAllGroups()
                    .filterNot { it.type == ServerType.PROXY }
                    .map { Suggestion.suggestion(it.name) }
            }
        }

    private fun Map<String, Any>.toArray(): Array<Pair<String, String>> =
        entries.map { (key, value) -> key to value.toString() }.toTypedArray()

    private fun getCachedMessage(message: String, vararg placeholders: Pair<String, String>): Component {
        val cacheKey = message + placeholders.contentToString()
        return messageCache.get(cacheKey) {
            miniMessage.deserialize(
                message,
                *placeholders.map { Placeholder.unparsed(it.first, it.second) }.toTypedArray()
            )
        }
    }

    private fun sendMessage(player: Player, message: String, vararg placeholders: Pair<String, String>) {
        player.sendMessage(getCachedMessage(message, *placeholders))
    }

    private fun printHelp(sender: Source) {
        (sender.source() as? Player)?.let { player ->
            sendMessage(player, "")
            sendMessage(player, SignMessages.HELP_HEADER)
            sendMessage(player, SignMessages.HELP_ADD)
            sendMessage(player, SignMessages.HELP_REMOVE)
            sendMessage(player, SignMessages.HELP_LIST)
        }
    }

    fun cleanup() {
        commandScope.cancel()
        messageCache.invalidateAll()
    }
}

sealed interface CommandResult {
    data class Success(val data: Map<String, Any> = emptyMap()) : CommandResult
    data class Error(val message: String) : CommandResult
}

enum class SignPermission(val node: String) {
    BASE("simplecloud.command.signs"),
    LIST("${BASE.node}.list"),
    ADD("${BASE.node}.add"),
    REMOVE("${BASE.node}.remove"),
    REMOVE_GROUP("${REMOVE.node}.group")
}