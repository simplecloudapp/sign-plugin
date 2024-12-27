package app.simplecloud.plugin.sign.paper.command

import app.simplecloud.plugin.sign.paper.PaperSignsPluginBootstrap
import build.buf.gen.simplecloud.controller.v1.ServerType
import io.grpc.StatusException
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.entity.Player
import org.incendo.cloud.Command
import org.incendo.cloud.description.Description
import org.incendo.cloud.paper.util.sender.PlayerSource
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion
import org.slf4j.LoggerFactory

class SignCommand(private val bootstrap: PaperSignsPluginBootstrap) : CoroutineScope {

    private val logger = LoggerFactory.getLogger(SignCommand::class.java)

    private val job = SupervisorJob()
    override val coroutineContext = Dispatchers.IO + job

    private val miniMessage = MiniMessage.miniMessage()

    companion object {
        private const val MAX_SIGN_DISTANCE = 8
        private val COMMAND_NAMES = arrayOf(
            "simplecloudsigns",
            "scsigns",
            "cloudsigns",
            "signs",
            "sign"
        )

        // Messages
        private const val SIGN_NOT_FOUND = "<red>You must look at a sign!"
        private const val SIGN_ALREADY_REGISTERED = "<red>This sign is already registered!"
        private const val SIGN_CREATE_SUCCESS = "<green>Successfully created sign for group <group>!"
        private const val GROUP_NOT_FOUND = "<red>The group <group> was not found!"
        private const val SIGN_REMOVE_SUCCESS = "<green>Successfully removed sign!"
        private const val SIGN_REMOVE_NOT_REGISTERED = "<red>This sign is not registered!"
        private const val SIGN_REMOVE_GROUP_NOT_REGISTERED = "<red>No signs registered for group <group>!"
        private const val SIGN_REMOVE_GROUP_SUCCESS = "<green>Successfully removed <amount> signs of group <group>!"
        private const val GENERAL_ERROR = "<red>An error occurred while processing your request."

        private const val HELP_HEADER =
            "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Commands of Cloud Sign Plugin"
        private const val HELP_ADD = "   <color:#a3a3a3>/cloudsigns add <group>"
        private const val HELP_REMOVE = "   <color:#a3a3a3>/cloudsigns remove [group]"
    }

    fun register() {
        val baseCommand = createBaseCommand()
        registerAddCommand(baseCommand)
        registerRemoveCommand(baseCommand)
        registerRemoveGroupCommand(baseCommand)
        bootstrap.commandManager.command(baseCommand.build())
    }

    private fun createBaseCommand() = bootstrap.commandManager
        .commandBuilder(COMMAND_NAMES[0], *COMMAND_NAMES)
        .senderType(PlayerSource::class.java)
        .commandDescription(Description.of("SimpleCloud Sign Plugin!"))
        .permission(SignPermission.BASE.node)
        .handler { printHelp(it.sender()) }

    private fun registerAddCommand(baseCommand: Command.Builder<PlayerSource>) {
        bootstrap.commandManager.command(
            baseCommand.literal("add")
                .commandDescription(Description.of("Register a new CloudSign"))
                .required(
                    "group",
                    StringParser.stringParser(),
                    Description.of("The name of the Target group"),
                    groupSuggestions()
                )
                .permission(SignPermission.ADD.node)
                .handler {
                    handleAddCommand(
                        it.sender().source(),
                        it.getOrDefault("group", "")
                    )
                }
        )
    }

    private fun handleAddCommand(player: Player, group: String) {
        val signLocation = getSignLocation(player) ?: run {
            sendMessage(player, SIGN_NOT_FOUND)
            return
        }

        launch {
            when (val result = executeAddSign(signLocation, group)) {
                is CommandResult.Success -> sendMessage(
                    player,
                    SIGN_CREATE_SUCCESS,
                    "group" to group
                )

                is CommandResult.Error -> sendMessage(player, result.message)
            }
        }
    }

    private fun registerRemoveCommand(baseCommand: Command.Builder<PlayerSource>) {
        bootstrap.commandManager.command(
            baseCommand.literal("remove")
                .commandDescription(Description.of("Unregister a SimpleCloud Sign"))
                .permission(SignPermission.REMOVE.node)
                .handler { context ->
                    handleRemoveCommand(context.sender().source())
                }
        )
    }

    private fun handleRemoveCommand(player: Player) {
        val signLocation = getSignLocation(player) ?: run {
            sendMessage(player, SIGN_NOT_FOUND)
            return
        }

        launch {
            when (val result = executeRemoveSign(signLocation)) {
                is CommandResult.Success -> sendMessage(player, SIGN_REMOVE_SUCCESS)
                is CommandResult.Error -> sendMessage(player, result.message)
            }
        }
    }

    private fun registerRemoveGroupCommand(baseCommand: Command.Builder<PlayerSource>) {
        bootstrap.commandManager.command(
            baseCommand.literal("remove")
                .commandDescription(Description.of("Unregister all signs for a group"))
                .required(
                    "group",
                    StringParser.stringParser(),
                    Description.of("The name of the Targeted group"),
                    groupRemovalSuggestions()
                )
                .permission(SignPermission.REMOVE_GROUP.node)
                .handler { context ->
                    handleRemoveGroupCommand(
                        context.sender().source(),
                        context.getOrDefault("group", "")
                    )
                }
        )
    }

    private fun handleRemoveGroupCommand(player: Player, group: String) {
        if (group.isBlank() || !bootstrap.signManager.exists(group)) {
            sendMessage(
                player,
                SIGN_REMOVE_GROUP_NOT_REGISTERED,
                "group" to group
            )
            return
        }

        launch {
            when (val result = executeRemoveGroupSigns(group)) {
                is CommandResult.Success -> sendMessage(
                    player,
                    SIGN_REMOVE_GROUP_SUCCESS,
                    "amount" to result.data["amount"].toString(),
                    "group" to group
                )

                is CommandResult.Error -> sendMessage(player, result.message)
            }
        }
    }

    private suspend fun executeAddSign(location: Location, group: String): CommandResult {
        return try {
            if (bootstrap.signManager.getCloudSign(location) != null) {
                return CommandResult.Error(SIGN_ALREADY_REGISTERED)
            }

            bootstrap.getControllerAPI().getGroups().getGroupByName(group)
            bootstrap.signManager.register(group, location)
            CommandResult.Success()
        } catch (e: StatusException) {
            CommandResult.Error(GROUP_NOT_FOUND)
        } catch (e: Exception) {
            logger.error("Error registering sign", e)
            CommandResult.Error(GENERAL_ERROR)
        }
    }

    private suspend fun executeRemoveSign(location: Location): CommandResult {
        return try {
            val cloudSign = bootstrap.signManager.getCloudSign(location) ?: return CommandResult.Error(
                SIGN_REMOVE_NOT_REGISTERED
            )

            unregisterSign(cloudSign.location)
            CommandResult.Success()
        } catch (e: Exception) {
            logger.error("Error removing sign", e)
            CommandResult.Error(GENERAL_ERROR)
        }
    }

    private suspend fun executeRemoveGroupSigns(group: String): CommandResult {
        return try {
            val locations = bootstrap.signManager.getLocationsByGroup(group) ?: return CommandResult.Success(
                mapOf("amount" to 0)
            )

            locations.forEach { location ->
                unregisterSign(bootstrap.signManager.mapLocation(location))
            }

            CommandResult.Success(mapOf("amount" to locations.size))
        } catch (e: Exception) {
            logger.error("Error removing group signs", e)
            CommandResult.Error(GENERAL_ERROR)
        }
    }

    private suspend fun unregisterSign(location: Location) {
        val sign = location.block.state as Sign

        withContext(Dispatchers.Main) {
            sign.getSide(Side.FRONT).lines().replaceAll { Component.text("") }
            sign.getSide(Side.BACK).lines().replaceAll { Component.text("") }
            sign.update()
        }

        bootstrap.signManager.removeCloudSign(location)
    }

    private fun getSignLocation(player: Player): Location? {
        val location = player.getTargetBlock(null, MAX_SIGN_DISTANCE).location
        return if (validateSignLocation(location)) location else null
    }

    private fun validateSignLocation(location: Location): Boolean =
        location.block.state is Sign && location.world != null

    private fun groupRemovalSuggestions(): BlockingSuggestionProvider<PlayerSource?> =
        BlockingSuggestionProvider { _, _ ->
            bootstrap.signManager.getAllGroupsRegistered()
                .map { Suggestion.suggestion(it) }
        }

    private fun groupSuggestions(): BlockingSuggestionProvider<PlayerSource?> =
        BlockingSuggestionProvider { _, _ ->
            runBlocking {
                bootstrap.getControllerAPI().getGroups().getAllGroups()
                    .filterNot { it.type == ServerType.PROXY }
                    .map { Suggestion.suggestion(it.name) }
            }
        }

    private fun sendMessage(player: Player, message: String, vararg placeholders: Pair<String, String>) {
        player.sendMessage(
            miniMessage.deserialize(
                message,
                *placeholders.map { Placeholder.unparsed(it.first, it.second) }.toTypedArray()
            )
        )
    }

    private fun printHelp(sender: PlayerSource) {
        val player = sender.source()
        sendMessage(player, HELP_HEADER)
        sendMessage(player, HELP_ADD)
        sendMessage(player, HELP_REMOVE)
    }

    fun cleanup() {
        job.cancel()
    }

}

sealed class CommandResult {
    data class Success(val data: Map<String, Any> = emptyMap()) : CommandResult()
    data class Error(val message: String) : CommandResult()
}

enum class SignPermission(val node: String) {
    BASE("simplecloud.command.signs"),

    ADD("${BASE.node}.add"),
    REMOVE("${BASE.node}.remove"),
    REMOVE_GROUP("${REMOVE.node}.group")
}