package app.simplecloud.plugin.sign.paper

import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager

class PaperSignsPluginBootstrap : PluginBootstrap {

    private lateinit var commandManager: PaperCommandManager.Bootstrapped<CommandSourceStack>

    override fun bootstrap(context: BootstrapContext) {
        commandManager = PaperCommandManager.builder()
            .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
            .buildBootstrapped(context)
    }

    override fun createPlugin(context: PluginProviderContext): JavaPlugin {
        return PaperSignsPlugin(commandManager)
    }

}