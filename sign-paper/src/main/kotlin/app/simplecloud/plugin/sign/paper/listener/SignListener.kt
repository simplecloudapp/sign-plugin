package app.simplecloud.plugin.sign.paper.listener

import app.simplecloud.plugin.sign.paper.PaperSignsPlugin
import app.simplecloud.plugin.sign.paper.rule.PlayerRuleContext
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent

data class SignListener(private val plugin: PaperSignsPlugin) : Listener {

    //TODO: Add Click Catching for Different Rules e.g handle click on sign that is staring/offline/current_server

    @EventHandler
    fun handleSignInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.clickedBlock == null) return

        val sign = (event.clickedBlock?.state as? Sign) ?: return

        plugin.bootstrap.signManager.getCloudSign(sign.location)?.let { cloudSign ->
            event.isCancelled = true

            val playerRuleContext = PlayerRuleContext(
                server = cloudSign.server,
                serverState = cloudSign.server?.state,
                event.player
            )

            cloudSign.server?.let { server ->
                val serverName = plugin.bootstrap.signManager.getLayout(playerRuleContext).constructName(server)
                plugin.sendPlayerToServer(event.player, serverName)
            }
        }
    }

    @EventHandler
    fun handleSignBreak(event: BlockBreakEvent) {
        val sign = (event.block.state as? Sign) ?: return

        plugin.bootstrap.signManager.getCloudSign(sign.location)?.let {
            event.isCancelled = true
        }
    }
}
