package app.simplecloud.plugin.sign.paper

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.plugin.sign.shared.SignPlugin
import app.simplecloud.plugin.sign.shared.SignUpdater
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager

class PaperSignsPluginBootstrap : PluginBootstrap {

    private val controllerApi = ControllerApi.createFutureApi()
    private val miniMessage = MiniMessage.miniMessage()

    private lateinit var signPlugin: SignPlugin<Location>
    private lateinit var commandManager: PaperCommandManager.Bootstrapped<CommandSourceStack>

    private val plugin by lazy { PaperSignsPlugin(signPlugin, commandManager) }

    override fun bootstrap(context: BootstrapContext) {
        signPlugin = SignPlugin(
            controllerApi,
            context.dataDirectory,
            PaperLocationMapper,
            SignUpdater { location, frameConfig ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    val sign = location.block.state as? Sign ?: return@Runnable

                    frameConfig.lines.forEachIndexed { index, line ->
                        sign.getSide(Side.FRONT).line(index, miniMessage.deserialize(line))
                    }

                    sign.update()
                })
            }
        )

        commandManager = PaperCommandManager.builder()
            .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
            .buildBootstrapped(context)

        context.pluginSource

        SignCommand(signPlugin, commandManager).register()
    }

}