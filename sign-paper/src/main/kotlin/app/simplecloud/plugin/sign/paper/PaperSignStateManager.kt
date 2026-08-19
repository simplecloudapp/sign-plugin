package app.simplecloud.plugin.sign.paper

import app.simplecloud.plugin.sign.paper.dispatcher.PaperPlatformDispatcher
import app.simplecloud.plugin.sign.shared.command.SignStateManager
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side

class PaperSignStateManager(
    private val bootstrap: PaperSignsPluginBootstrap,
) : SignStateManager<Location> {

    private val dispatcher by lazy { PaperPlatformDispatcher(bootstrap) }

    override suspend fun clearSign(location: Location) {
        withContext(dispatcher.getDispatcher()) {
            val world = location.world ?: return@withContext
            val block = world.getBlockAt(location.x.toInt(), location.y.toInt(), location.z.toInt())

            if (block.state !is Sign) return@withContext

            val sign = block.state as Sign
            sign.getSide(Side.FRONT).lines().indices.forEach { i ->
                sign.getSide(Side.FRONT).line(i, Component.empty())
                sign.getSide(Side.BACK).line(i, Component.empty())
            }

            sign.update(true)
        }
    }
}