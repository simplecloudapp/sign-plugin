package app.simplecloud.plugin.sign.paper

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.paper.PaperCommandManager

class PaperSignsPlugin(
    private val commandManager: PaperCommandManager.Bootstrapped<CommandSourceStack>
): JavaPlugin() {

    override fun onEnable() {
        commandManager.onEnable()
    }

}