package app.simplecloud.plugin.sign.paper

import app.simplecloud.plugin.sign.paper.listener.SignListener
import app.simplecloud.plugin.sign.shared.SignManager
import com.google.common.io.ByteStreams
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.paper.PaperCommandManager
import org.incendo.cloud.paper.util.sender.Source

class PaperSignsPlugin(
    val signManager: SignManager<Location>,
    private val commandManager: PaperCommandManager.Bootstrapped<Source>
) : JavaPlugin() {

    companion object {
        lateinit var instance: PaperSignsPlugin
            private set
    }

    override fun onEnable() {
        instance = this
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

        Bukkit.getPluginManager().registerEvents(SignListener(this), this)

        signManager.start()
        commandManager.onEnable()
    }

    override fun onDisable() {
        signManager.stop()
    }

    fun sendPlayerToServer(player: Player, serverName: String) {
        val dataOutput = ByteStreams.newDataOutput()
        dataOutput.writeUTF("Connect")
        dataOutput.writeUTF(serverName)
        player.sendPluginMessage(this, "BungeeCord", dataOutput.toByteArray())
    }
}