package app.simplecloud.plugin.sign.paper.command

import app.simplecloud.plugin.sign.paper.PaperSignsPlugin
import app.simplecloud.plugin.sign.paper.PaperSignsPluginBootstrap
import app.simplecloud.plugin.sign.shared.config.SignMessageConfig
import build.buf.gen.simplecloud.controller.v1.ServerType
import io.grpc.StatusException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.entity.Player
import org.incendo.cloud.description.Description
import org.incendo.cloud.paper.util.sender.PlayerSource
import org.incendo.cloud.paper.util.sender.Source
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion

class SignCommand(private var bootstrap: PaperSignsPluginBootstrap) {

    fun register() {
        val signCommand =
            bootstrap.commandManager.commandBuilder("simplecloudsigns", "scsigns", "cloudsigns", "signs", "sign")
                .senderType(PlayerSource::class.java)
                .commandDescription(Description.of("SimpleCloud Sign Plugin!"))
                .permission("simplecloud.command.signs")
                .handler {
                    printHelp(it.sender())
                }

        bootstrap.commandManager.command(
            signCommand.literal("add")
                .commandDescription(Description.of("Register a new SimpleCloud Sign"))
                .required(
                    "group",
                    StringParser.stringParser(),
                    Description.of("The name of the Target group"),
                    groupSuggestions(),
                )
                .permission("simplecloud.command.signs.add")
                .handler {
                    val player = it.sender().source()
                    val group = it.getOrDefault("group", "")

                    val location = getLocation(player)
                    if (location.block.state !is Sign) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(SignMessageConfig.SIGN_NOT_FOUND))
                        return@handler
                    }

                    bootstrap.signManager.getCloudSign(location)?.let {
                        player.sendMessage(
                            MiniMessage.miniMessage().deserialize(SignMessageConfig.SIGN_ALREADY_REGISTERED)
                        )
                        return@handler
                    } ?: run {
                        CoroutineScope(Dispatchers.IO).launch {
                            kotlin.runCatching {
                                bootstrap.controllerApi.getGroups().getGroupByName(group)
                                bootstrap.signManager.register(group, location)

                                player.sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        SignMessageConfig.SIGN_CREATE_SUCCESS,
                                        Placeholder.unparsed("group", group)
                                    )
                                )
                            }.onFailure { exception ->
                                if (exception is StatusException) {
                                    player.sendMessage(
                                        MiniMessage.miniMessage()
                                            .deserialize(
                                                SignMessageConfig.GROUP_NOT_FOUND,
                                                Placeholder.component("group", Component.text(group))
                                            )
                                    )
                                    return@launch
                                }
                            }
                        }
                    }
                }
        )

        bootstrap.commandManager.command(
            signCommand.literal("remove")
                .commandDescription(Description.of("Unregister a SimpleCloud Sign"))
                .permission("simplecloud.command.signs.remove")
                .handler {
                    val player = it.sender().source()
                    val location = getLocation(player)

                    if (getLocation(player).block.state !is Sign) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(SignMessageConfig.SIGN_NOT_FOUND))
                        return@handler
                    }

                    bootstrap.signManager.getCloudSign(location)?.let { cloudSign ->
                        cloudSign.server.let {
                            unregisterSign(location)

                            player.sendMessage(
                                MiniMessage.miniMessage().deserialize(SignMessageConfig.SIGN_REMOVE_SUCCESS)
                            )
                        }
                    } ?: run {
                        player.sendMessage(
                            MiniMessage.miniMessage().deserialize(SignMessageConfig.SIGN_REMOVE_NOT_REGISTERED)
                        )
                        return@handler
                    }
                }
        )

        bootstrap.commandManager.command(
            signCommand.literal("remove")
                .commandDescription(Description.of("Unregister a SimpleCloud Sign"))
                .required(
                    "group",
                    StringParser.stringParser(),
                    Description.of("The name of the Targeted group"),
                    groupRemovalSuggestions()
                )
                .permission("simplecloud.command.signs.remove.group")
                .handler {
                    val player = it.sender().source()
                    val group = it.getOrDefault("group", "")

                    if (bootstrap.signManager.exists(group).not()) {
                        player.sendMessage(
                            MiniMessage.miniMessage().deserialize(
                                SignMessageConfig.SIGN_REMOVE_GROUP_NOT_REGISTERED,
                                Placeholder.unparsed("group", group)
                            )
                        )
                        return@handler
                    }

                    // ---- Remove all CloudSigns associated with group ----//
                    if (group.isNotBlank()) {
                        bootstrap.signManager.getLocationsByGroup(group).let { locations ->
                            val signAmount = locations!!.size
                            locations.forEach { location ->
                                unregisterSign(bootstrap.signManager.mapLocation(location))
                            }

                            player.sendMessage(
                                MiniMessage.miniMessage().deserialize(
                                    SignMessageConfig.SIGN_REMOVE_GROUP_SUCCESS,
                                    Placeholder.unparsed("amount", signAmount.toString()),
                                    Placeholder.unparsed("group", group)
                                )
                            )
                        }

                        bootstrap.signManager.getCloudSignsByGroup(group)?.let { cloudSignList ->
                            cloudSignList.forEach { cloudSign ->
                                run {
                                    unregisterSign(cloudSign.location)
                                }
                            }
                        }

                        return@handler
                    }
                }
        )

        bootstrap.commandManager.command(signCommand.build())
    }

    private fun groupRemovalSuggestions(): BlockingSuggestionProvider<Source?> {
        return BlockingSuggestionProvider<Source?> { _, _ ->
            bootstrap.signManager.getAllGroupsRegistered()
                .map { Suggestion.suggestion(it) }
                .toList()
        }
    }

    private fun groupSuggestions(): BlockingSuggestionProvider<Source?> {
        return BlockingSuggestionProvider<Source?> { _, _ ->
            runBlocking(Dispatchers.IO) {
                bootstrap.controllerApi.getGroups().getAllGroups()
                    .filter { it.type != ServerType.PROXY }
                    .map { Suggestion.suggestion(it.name) }
                    .toList()
            }
        }
    }


    private fun unregisterSign(location: Location) {
        val sign = location.block.state as Sign

        Bukkit.getScheduler().runTask(PaperSignsPlugin.instance, Runnable {
            sign.getSide(Side.FRONT).lines().replaceAll { Component.text("") }
            sign.getSide(Side.BACK).lines().replaceAll { Component.text("") }

            sign.update()
        })

        bootstrap.signManager.removeCloudSign(location)
    }

    private fun getLocation(player: Player): Location {
        return player.getTargetBlock(null, 6).location
    }

    private fun printHelp(sender: PlayerSource) {
        val player = sender.source()

        player.sendMessage(
            MiniMessage.miniMessage()
                .deserialize("<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Commands of Cloud Sign Plugin")
        )
        player.sendMessage(MiniMessage.miniMessage().deserialize("   <color:#a3a3a3>/cloudsigns add <group>"))
        player.sendMessage(MiniMessage.miniMessage().deserialize("   <color:#a3a3a3>/cloudsigns remove [group]"))
    }

}