package app.simplecloud.plugin.sign.paper.sender

import app.simplecloud.plugin.sign.paper.PaperSignsPlugin
import app.simplecloud.plugin.sign.shared.config.location.SignLocation
import app.simplecloud.plugin.sign.shared.sender.SignCommandSender
import app.simplecloud.plugin.sign.shared.utils.SignCommandMessages
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.FluidCollisionMode
import org.bukkit.block.Sign
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
class PaperCommandSender(
    val sourceStack: CommandSourceStack
) : SignCommandSender {

    override fun sendMessage(component: Component) =
        sourceStack.sender.sendMessage(component)

    override suspend fun getTargetBlock(maxDistance: Int): SignLocation? {
        val player = sourceStack.sender as? Player ?: return null

        return withContext(PaperSignsPlugin.instance.bootstrap.platformDispatcher.getDispatcher()) {
            val targetBlock =
                player.getTargetBlockExact(maxDistance, FluidCollisionMode.NEVER) ?: return@withContext null

            if (targetBlock.state !is Sign) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(SignCommandMessages.SIGN_NOT_FOUND))
                return@withContext null
            }

            SignLocation(
                world = targetBlock.world.name,
                x = targetBlock.x.toDouble(),
                y = targetBlock.y.toDouble(),
                z = targetBlock.z.toDouble()
            )
        }
    }
}