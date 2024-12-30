package app.simplecloud.plugin.sign.paper.dispatcher

import app.simplecloud.plugin.sign.paper.PaperSignsPluginBootstrap
import app.simplecloud.plugin.sign.shared.dispatcher.PlatformDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import kotlin.coroutines.CoroutineContext

class PaperPlatformDispatcher(
    private val bootstrap: PaperSignsPluginBootstrap,
) : PlatformDispatcher {

    override fun getDispatcher(): CoroutineDispatcher = BukkitMainDispatcher(bootstrap.plugin)

    private inner class BukkitMainDispatcher(private val plugin: JavaPlugin) : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            Bukkit.getScheduler().runTask(plugin, block)
        }
    }
}