package app.simplecloud.plugin.sign.shared.command

import app.simplecloud.plugin.sign.shared.config.location.SignLocation
import app.simplecloud.plugin.sign.shared.dispatcher.PlatformDispatcher
import app.simplecloud.plugin.sign.shared.sender.SignCommandSender
import app.simplecloud.plugin.sign.shared.service.SignService
import app.simplecloud.plugin.sign.shared.utils.SignCommandMessages
import app.simplecloud.plugin.sign.shared.utils.SignCommandPermission
import build.buf.gen.simplecloud.controller.v1.ServerType
import com.google.common.cache.CacheBuilder
import io.grpc.StatusException
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.Command
import org.incendo.cloud.CommandManager
import org.incendo.cloud.description.Description
import org.incendo.cloud.parser.standard.IntegerParser
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class SignCommand<C : SignCommandSender, T>(
    private val commandManager: CommandManager<C>,
    private val signService: SignService<T>,
    private val signStateManager: SignStateManager<T>,
    private val platformDispatcher: PlatformDispatcher,
) {

    private val logger = LoggerFactory.getLogger(SignCommand::class.java)
    private val commandScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val miniMessage = MiniMessage.miniMessage()

    private val messageCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, Component>()

    companion object {
        private const val LOCATIONS_PER_PAGE = 5
        private const val MAX_SIGN_DISTANCE = 8

        private val COMMAND_NAMES = arrayOf(
            "simplecloudsign",
            "simplecloudsigns",
            "scsigns",
            "scsign",
            "cloudsigns",
            "cloudsign",
            "signs",
            "sign"
        )
    }

    fun register() {
        commandManager.command(
            createBaseCommand().also { registerSubCommands(it) }.build()
        )
    }

    private fun registerSubCommands(baseCommand: Command.Builder<C>) {
        commandManager.apply {
            listOf(
                createHelpCommand(baseCommand),
                createListCommand(baseCommand),
                createAddCommand(baseCommand),
                createRemoveCommand(baseCommand),
                createRemoveGroupCommand(baseCommand),
            ).forEach { command(it) }
        }
    }

    private fun createBaseCommand() = commandManager
        .commandBuilder(COMMAND_NAMES[0], *COMMAND_NAMES)
        .commandDescription(Description.of("SimpleCloud Sign Plugin!"))
        .permission(SignCommandPermission.BASE.node)
        .handler { printHelp(it.sender()) }

    private fun createHelpCommand(baseCommand: Command.Builder<C>) =
        baseCommand.literal("help")
            .commandDescription(Description.of("SimpleCloud Sign Plugin!"))
            .permission(SignCommandPermission.BASE.node)
            .handler { printHelp(it.sender()) }

    private fun createListCommand(baseCommand: Command.Builder<C>) =
        baseCommand.literal("list")
            .commandDescription(Description.of("List Registered CloudSigns"))
            .optional(
                "page",
                IntegerParser.integerParser(),
            )
            .optional(
                "group",
                StringParser.stringParser(),
                Description.of("The name of the Target group"),
                registeredGroupSuggestions()
            )
            .permission(SignCommandPermission.LIST.node)
            .handler { context ->
                val page = context.getOrDefault("page", 1)
                val group = context.getOrDefault("group", "")
                handleListCommand(context.sender(), group, page)
            }

    private fun createAddCommand(baseCommand: Command.Builder<C>) =
        baseCommand.literal("add")
            .commandDescription(Description.of("Register a new CloudSign"))
            .required(
                "group",
                StringParser.stringParser(),
                Description.of("The name of the Target group"),
                groupSuggestions()
            )
            .permission(SignCommandPermission.ADD.node)
            .handler { context ->
                commandScope.launch {
                    handleSignOperation(
                        context.sender(),
                        context.get("group"),
                        SignOperation.ADD
                    ) { location, group ->
                        executeAddSign(location, group)
                    }
                }
            }

    private fun createRemoveCommand(baseCommand: Command.Builder<C>) =
        baseCommand.literal("remove")
            .commandDescription(Description.of("Unregister a SimpleCloud Sign"))
            .permission(SignCommandPermission.REMOVE.node)
            .handler { context ->
                CoroutineScope(Dispatchers.IO).launch {
                    handleSignOperation(
                        context.sender(),
                        operation = SignOperation.REMOVE
                    ) { location, _ ->
                        executeRemoveSign(location)
                    }
                }
            }

    private fun createRemoveGroupCommand(baseCommand: Command.Builder<C>) =
        baseCommand.literal("remove")
            .commandDescription(Description.of("Unregister all signs for a group"))
            .required(
                "group",
                StringParser.stringParser(),
                Description.of("The name of the Targeted group"),
                registeredGroupSuggestions()
            )
            .permission(SignCommandPermission.REMOVE_GROUP.node)
            .handler { context ->
                handleRemoveGroupCommand(context.sender(), context.getOrDefault("group", ""))
            }

    private fun handleListCommand(sender: SignCommandSender, group: String, page: Int = 1) {
        commandScope.launch {
            runCatching {
                when {
                    group.isBlank() -> listAllSigns(sender, page)
                    else -> listGroupSigns(sender, group, page)
                }
            }.onFailure {
                logger.error("Error while listing $group")
                sendMessage(sender, SignCommandMessages.GENERAL_ERROR)
            }
        }
    }

    private fun listAllSigns(sender: SignCommandSender, page: Int) {
        val allSigns = signService.getAllGroupsRegistered()
            .filter { group -> signService.getLocationsByGroup(group)?.isNotEmpty() == true }

        if (allSigns.isEmpty()) {
            sendMessage(sender, SignCommandMessages.NO_SIGNS_REGISTERED)
            return
        }

        repeat(100) {
            sender.sendMessage(Component.empty())
        }

        sendMessage(sender, "<color:#38bdf8>✦ <bold>All Registered Signs</bold></color>")
        sendMessage(sender, "<color:#a8a8a8>════════════════════════</color>")

        val allLocations = allSigns.flatMap { group ->
            signService.getLocationsByGroup(group)?.map { location ->
                group to location
            } ?: emptyList()
        }

        sendAllSignsInformation(sender, allLocations, page)
    }

    private fun sendAllSignsInformation(
        sender: SignCommandSender,
        groupedLocations: List<Pair<String, SignLocation>>,
        page: Int
    ) {
        val totalPages = ceil(groupedLocations.size.toDouble() / LOCATIONS_PER_PAGE).toInt()
        val validPage = page.coerceIn(1, maxOf(1, totalPages))

        val startIndex = (validPage - 1) * LOCATIONS_PER_PAGE
        val endIndex = minOf(startIndex + LOCATIONS_PER_PAGE, groupedLocations.size)

        val paginatedLocations = groupedLocations.subList(startIndex, endIndex)
        val locationInformation = paginatedLocations.joinToString("\n") { (group, location) ->
            """<click:run_command:/sign tp ${location.world} ${location.x.toInt()} ${location.y.toInt()} ${location.z.toInt()}><hover:show_text:'Click to teleport'>
<color:#a8a8a8>└─ <color:#4ade80>Group:</color> <color:#ffffff>${group}</color>
   <color:#a8a8a8>├─</color> <color:#38bdf8>World:</color> <color:#ffffff>${location.world}</color>
   <color:#a8a8a8>├─</color> <color:#38bdf8>X:</color> <color:#ffffff>${location.x}</color>
   <color:#a8a8a8>├─</color> <color:#38bdf8>Y:</color> <color:#ffffff>${location.y}</color>
   <color:#a8a8a8>└─</color> <color:#38bdf8>Z:</color> <color:#ffffff>${location.z}</color></hover></click>"""
        }

        val navigationButtons = buildString {
            if (validPage > 1) {
                append("<click:run_command:/sign list ${validPage - 1}><color:#38bdf8>« Previous</color></click> ")
            }
            append("<color:#a8a8a8> [ </color><color:#ffffff>$validPage/$totalPages</color><color:#a8a8a8> ] </color>")
            if (validPage < totalPages) {
                append(" <click:run_command:/sign list ${validPage + 1}><color:#38bdf8>Next »</color></click>")
            }
        }

        sendMessage(
            sender,
            """
$locationInformation
<color:#a8a8a8>════════════════════════</color>
$navigationButtons
            """.trimIndent()
        )
    }

    private fun listGroupSigns(sender: SignCommandSender, group: String, page: Int) {
        val locations = signService.getLocationsByGroup(group)
        if (locations.isNullOrEmpty()) {
            sendMessage(sender, SignCommandMessages.SIGN_REMOVE_GROUP_NOT_REGISTERED, "group" to group)
            return
        }

        sendMessage(sender, SignCommandMessages.LIST_HEADER)
        sendSignGroupInformation(sender, group, locations, page)
    }

    private fun sendSignGroupInformation(
        sender: SignCommandSender,
        group: String,
        locations: List<SignLocation>,
        page: Int
    ) {
        val totalPages = ceil(locations.size.toDouble() / LOCATIONS_PER_PAGE).toInt()
        val validPage = page.coerceIn(1, maxOf(1, totalPages))

        val startIndex = (validPage - 1) * LOCATIONS_PER_PAGE
        val endIndex = minOf(startIndex + LOCATIONS_PER_PAGE, locations.size)

        repeat(100) {
            sender.sendMessage(Component.empty())
        }

        sendMessage(
            sender,
            "<color:#38bdf8>✦ <bold>Group Signs: ${group}</bold></color> <gray>(${locations.size} signs)</gray>"
        )
        sendMessage(sender, "<color:#a8a8a8>════════════════════════</color>")

        val paginatedLocations = locations.subList(startIndex, endIndex)
        val locationInformation = paginatedLocations.joinToString("\n") { location ->
            """<click:run_command:/minecraft:tp ${location.x} ${location.y} ${location.z}><hover:show_text:'Click to teleport'>
<color:#a8a8a8>└─ <color:#4ade80>World:</color> <color:#ffffff>${location.world}</color>
   <color:#a8a8a8>├─</color> <color:#38bdf8>X:</color> <color:#ffffff>${location.x}</color>
   <color:#a8a8a8>├─</color> <color:#38bdf8>Y:</color> <color:#ffffff>${location.y}</color>
   <color:#a8a8a8>└─</color> <color:#38bdf8>Z:</color> <color:#ffffff>${location.z}</color></hover></click>"""
        }

        val navigationButtons = buildString {
            if (validPage > 1) {
                append("<click:run_command:/sign list $group ${validPage - 1}><color:#38bdf8>« Previous</color></click> ")
            }
            append("<color:#a8a8a8> [ </color><color:#ffffff>$validPage/$totalPages</color><color:#a8a8a8> ] </color>")
            if (validPage < totalPages) {
                append(" <click:run_command:/sign list $group ${validPage + 1}><color:#38bdf8>Next »</color></click>")
            }
        }

        sendMessage(
            sender,
            """
$locationInformation
<color:#a8a8a8>════════════════════════</color>
$navigationButtons
            """.trimIndent()
        )
    }

    private suspend inline fun handleSignOperation(
        sender: SignCommandSender,
        group: String = "",
        operation: SignOperation = SignOperation.ADD,
        crossinline action: suspend (T, String) -> CommandResult
    ) {
        val signLocation = getSignLocation(sender, operation) ?: return

        commandScope.launch {
            try {
                executeCommand(sender) { action(signService.map(signLocation), group) }
            } catch (e: Exception) {
                logger.error("Error executing sign operation", e)
                sendMessage(sender, SignCommandMessages.GENERAL_ERROR)
            }
        }
    }

    private fun handleRemoveGroupCommand(sender: SignCommandSender, group: String) {
        if (group.isBlank() || !signService.exists(group)) {
            sendMessage(
                sender,
                SignCommandMessages.SIGN_REMOVE_GROUP_NOT_REGISTERED,
                "group" to group
            )
            return
        }

        commandScope.launch {
            try {
                executeCommand(sender) { executeRemoveGroupSigns(group) }
            } catch (e: Exception) {
                logger.error("Error removing group signs", e)
                sendMessage(sender, SignCommandMessages.GENERAL_ERROR)
            }
        }
    }

    private suspend fun executeAddSign(location: T, group: String): CommandResult {
        return runCatching {
            if (signService.getCloudSign(location) != null) {
                return CommandResult.Error(SignCommandMessages.SIGN_ALREADY_REGISTERED)
            }

            signService.controllerApi.getGroups().getGroupByName(group)
            signService.register(group, location)
            CommandResult.Success(mapOf("group" to group))
        }.getOrElse { e ->
            when (e) {
                is StatusException -> CommandResult.Error(SignCommandMessages.GROUP_NOT_FOUND)
                else -> {
                    logger.error("Error registering sign", e)
                    CommandResult.Error(SignCommandMessages.GENERAL_ERROR)
                }
            }
        }
    }

    private suspend fun executeRemoveSign(location: T): CommandResult {
        return runCatching {
            println("Removing sign $location")
            val cloudSign = signService.getCloudSign(location)
                ?: return CommandResult.Error(SignCommandMessages.SIGN_REMOVE_NOT_REGISTERED)

            signService.removeCloudSign(cloudSign.location)
            clearSign(cloudSign.location)

            CommandResult.Success()
        }.getOrElse { e ->
            logger.error("Error removing sign", e)
            CommandResult.Error(SignCommandMessages.GENERAL_ERROR)
        }
    }

    private suspend fun executeRemoveGroupSigns(group: String): CommandResult {
        return runCatching {
            val locations = signService.getLocationsByGroup(group)
                ?.takeIf { it.isNotEmpty() }
                ?: return CommandResult.Error(
                    SignCommandMessages.SIGN_REMOVE_GROUP_NOT_REGISTERED.replace("<group>", group)
                )

            val amount = locations.size

            locations.forEach { location ->
                val mapLocation = signService.map(location)
                signService.removeCloudSign(mapLocation)
            }

            withContext(platformDispatcher.getDispatcher()) {
                locations.forEach { location ->
                    val mapLocation = signService.map(location)
                    clearSign(mapLocation)
                }
            }

            CommandResult.Success(mapOf("amount" to amount))
        }.getOrElse { e ->
            logger.error("Error removing group signs", e)
            CommandResult.Error(SignCommandMessages.GENERAL_ERROR)
        }
    }

    private suspend fun clearSign(location: T) {
        withContext(platformDispatcher.getDispatcher()) {
            signStateManager.clearSign(location)
        }
    }

    private suspend inline fun executeCommand(
        sender: SignCommandSender,
        crossinline action: suspend () -> CommandResult
    ) {
        when (val result = action()) {
            is CommandResult.Success -> {
                val message = when {
                    result.data.containsKey("group") -> SignCommandMessages.SIGN_CREATE_SUCCESS
                    result.data.containsKey("amount") -> SignCommandMessages.SIGN_REMOVE_GROUP_SUCCESS
                    else -> SignCommandMessages.SIGN_REMOVE_SUCCESS
                }

                sendMessage(sender, message, *result.data.toArray())
            }

            is CommandResult.Error -> sendMessage(sender, result.message)
        }
    }

    private fun printHelp(sender: SignCommandSender) {
        sendMessage(sender, "")
        sendMessage(sender, SignCommandMessages.HELP_HEADER)
        sendMessage(sender, SignCommandMessages.HELP_ADD)
        sendMessage(sender, SignCommandMessages.HELP_REMOVE)
        sendMessage(sender, SignCommandMessages.HELP_LIST)
    }

    private fun getCachedMessage(message: String, vararg placeholders: Pair<String, String>): Component {
        val cacheKey = message + placeholders.contentToString()
        return messageCache.get(cacheKey) {
            miniMessage.deserialize(
                message,
                *placeholders.map { Placeholder.unparsed(it.first, it.second) }.toTypedArray()
            )
        }
    }

    private fun sendMessage(sender: SignCommandSender, message: String, vararg placeholders: Pair<String, String>) {
        sender.sendMessage(getCachedMessage(message, *placeholders))
    }

    private suspend fun getSignLocation(
        sender: SignCommandSender,
        operation: SignOperation = SignOperation.ADD
    ): SignLocation? =
        sender.getTargetBlock(MAX_SIGN_DISTANCE)?.let { location ->
            val hasExistingSign = signService.getCloudSign(signService.map(location)) != null
            when (operation) {
                SignOperation.ADD -> if (!hasExistingSign) location else {
                    sender.sendMessage(getCachedMessage(SignCommandMessages.SIGN_ALREADY_REGISTERED))
                    null
                }

                SignOperation.REMOVE -> if (hasExistingSign) location else {
                    sender.sendMessage(getCachedMessage(SignCommandMessages.SIGN_REMOVE_NOT_REGISTERED))
                    null
                }
            }
        }


    private fun registeredGroupSuggestions(): BlockingSuggestionProvider<C?> =
        BlockingSuggestionProvider { _, _ ->
            signService.getAllGroupsRegistered()
                .map { Suggestion.suggestion(it) }
        }

    private fun groupSuggestions(): BlockingSuggestionProvider<C?> =
        BlockingSuggestionProvider { _, _ ->
            runBlocking {
                signService.controllerApi.getGroups().getAllGroups()
                    .filterNot { it.type == ServerType.PROXY }
                    .map { Suggestion.suggestion(it.name) }
            }
        }

    fun cleanup() {
        commandScope.cancel()
        messageCache.invalidateAll()
    }

    private fun Map<String, Any>.toArray(): Array<Pair<String, String>> =
        entries.map { (key, value) -> key to value.toString() }.toTypedArray()
}

private enum class SignOperation {
    ADD,
    REMOVE
}

sealed interface CommandResult {
    data class Success(val data: Map<String, Any> = emptyMap()) : CommandResult
    data class Error(val message: String) : CommandResult
}