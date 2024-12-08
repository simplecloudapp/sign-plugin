package app.simplecloud.plugin.sign.paper

import app.simplecloud.plugin.sign.shared.SignPlugin
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Location
import org.bukkit.entity.Player
import org.incendo.cloud.paper.PaperCommandManager
import org.incendo.cloud.parser.standard.StringParser

class SignCommand(
    private val signPlugin: SignPlugin<Location>,
    private val commandManager: PaperCommandManager<CommandSourceStack>
) {

    fun register() {
        val signsRootCommandNode = commandManager
            .commandBuilder("signs")
//            .permission("cloud.signs")


        commandManager.command(signsRootCommandNode.literal("add")
            .required("groupName", StringParser.stringParser())
            .handler {
                val player = it.sender().executor as? Player ?: return@handler
                val groupName = it.get<String>("groupName")
                val location = getLocation(player)

                signPlugin.locationsRepository.saveLocation(groupName, location)
                player.sendMessage("Location saved $groupName")
            })

        commandManager.command(signsRootCommandNode.literal("remove")
            .handler {
                val player = it.sender().executor as? Player ?: return@handler
                val location = getLocation(player)

                signPlugin.locationsRepository.removeLocation(location)
                player.sendMessage("Location removed")
            })

        commandManager.command(signsRootCommandNode.build())
    }

    private fun getLocation(player: Player): Location {
        return player.getTargetBlock(null, 6).location
    }

}