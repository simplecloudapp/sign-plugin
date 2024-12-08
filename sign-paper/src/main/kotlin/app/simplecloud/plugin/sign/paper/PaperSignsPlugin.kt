package app.simplecloud.plugin.sign.paper

import app.simplecloud.plugin.sign.shared.SignPlugin
import com.google.common.io.ByteStreams
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.paper.PaperCommandManager

class PaperSignsPlugin(
    private val signPlugin: SignPlugin<Location>,
    private val commandManager: PaperCommandManager.Bootstrapped<CommandSourceStack>
): JavaPlugin(), Listener {

    override fun onEnable() {
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
        Bukkit.getPluginManager().registerEvents(this, this)

        commandManager.onEnable()
        signPlugin.start()
    }

    @EventHandler
    fun handleInteract(event: PlayerInteractEvent) {
        if (event.useInteractedBlock() == Event.Result.DENY) {
            return
        }

        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val block = event.clickedBlock?.state as? Sign?: return

        val cloudSign = signPlugin.getCloudSign(block.location)?: return
        if (cloudSign.server == null) {
            return
        }
        event.isCancelled = true

        val serverName = signPlugin.getLayout(cloudSign.server).constructName(cloudSign.server!!)
        sendPlayerToServer(event.player, serverName)
    }

    private fun sendPlayerToServer(player: Player, serverName: String) {
        val dataOutput = ByteStreams.newDataOutput()
        dataOutput.writeUTF("Connect")
        dataOutput.writeUTF(serverName)
        player.sendPluginMessage(this, "BungeeCord", dataOutput.toByteArray())
    }

}