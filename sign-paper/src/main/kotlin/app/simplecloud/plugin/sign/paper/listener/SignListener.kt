package app.simplecloud.plugin.sign.paper.listener

import app.simplecloud.plugin.sign.paper.PaperSignsPlugin
import app.simplecloud.plugin.sign.shared.rule.impl.ServerRuleContext
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent

class SignListener(private val plugin: PaperSignsPlugin) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun handleSignInteract(event: PlayerInteractEvent) {
        if (!isValidSignInteraction(event)) return

        val sign = event.clickedBlock?.state as? Sign ?: return
        val cloudSign = plugin.bootstrap.signManager.getCloudSign(sign.location) ?: return

        event.isCancelled = true

        cloudSign.server?.let { server ->
            val serverRuleContext = ServerRuleContext(server = server)
            val serverName = plugin.bootstrap.signManager.getLayout(serverRuleContext)
                .constructName(server)

            plugin.sendPlayerToServer(event.player, serverName)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun handleSignBreak(event: BlockBreakEvent) {
        if (!isSign(event.block)) return

        val sign = event.block.state as Sign
        if (plugin.bootstrap.signManager.getCloudSign(sign.location) != null) {
            event.isCancelled = true
        }
    }

    private fun isValidSignInteraction(event: PlayerInteractEvent): Boolean {
        return event.action == Action.RIGHT_CLICK_BLOCK &&
                event.clickedBlock != null &&
                isSign(event.clickedBlock!!)
    }

    private fun isSign(block: Block): Boolean = block.state is Sign

}
