package app.simplecloud.plugin.sign.paper

import app.simplecloud.plugin.sign.paper.listener.SignListener
import com.google.common.io.ByteStreams
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PaperSignsPlugin(
    val bootstrap: PaperSignsPluginBootstrap
) : JavaPlugin() {

    private val logger: Logger = LoggerFactory.getLogger(PaperSignsPlugin::class.java)

    companion object {
        lateinit var instance: PaperSignsPlugin
            private set
    }

    override fun onLoad() {
        instance = this
    }

    override fun onEnable() {
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

        Bukkit.getPluginManager().registerEvents(SignListener(this), this)

        bootstrap.signManager.start()
        bootstrap.commandManager.onEnable()
    }

    override fun onDisable() {
        runBlocking {
            try {
                bootstrap.signManager.stop()
                bootstrap.disable()
            } catch (e: Exception) {
                logger.error("Error stopping SignManager", e)
            }
        }
    }

    fun sendPlayerToServer(player: Player, serverName: String) {
        val dataOutput = ByteStreams.newDataOutput()
        dataOutput.writeUTF("Connect")
        dataOutput.writeUTF(serverName)
        player.sendPluginMessage(this, "BungeeCord", dataOutput.toByteArray())
    }
}