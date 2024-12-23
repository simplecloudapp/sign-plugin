package app.simplecloud.plugin.sign.paper

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.plugin.sign.paper.command.SignCommand
import app.simplecloud.plugin.sign.shared.CloudSign
import app.simplecloud.plugin.sign.shared.SignManager
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper
import org.incendo.cloud.paper.util.sender.Source

@Suppress("UnstableApiUsage")
class PaperSignsPluginBootstrap : PluginBootstrap {

    val controllerApi = ControllerApi.createCoroutineApi()
    private val miniMessage = MiniMessage.miniMessage()

    lateinit var signManager: SignManager<Location>
    lateinit var commandManager: PaperCommandManager.Bootstrapped<Source>

    private val plugin by lazy { PaperSignsPlugin(signManager, commandManager) }

    override fun bootstrap(context: BootstrapContext) {
        signManager = SignManager(
            controllerApi,
            context.dataDirectory,
            PaperLocationMapper
        ) { cloudSign, frameConfig ->
            val location = cloudSign.location

            Bukkit.getScheduler().runTask(plugin, Runnable {
                val sign = location.block.state as? Sign ?: return@Runnable

                sign.getSide(Side.FRONT).lines().replaceAll { Component.empty() }

                frameConfig.lines.forEachIndexed { index, line ->
                    sign.getSide(Side.FRONT).line(
                        index, miniMessage.deserialize(
                            line,
                            *getPlaceholders(cloudSign).toTypedArray()
                        )
                    )
                }

                sign.update()
            })
        }

        commandManager = PaperCommandManager.builder(PaperSimpleSenderMapper.simpleSenderMapper())
            .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
            .buildBootstrapped(context)

        SignCommand(this).register()
    }

    override fun createPlugin(context: PluginProviderContext): JavaPlugin {
        return plugin
    }

    private fun getPlaceholders(cloudSign: CloudSign<*>): List<TagResolver.Single> {
        return listOf(
            Placeholder.parsed("group", cloudSign.server?.group ?: "unknown"),
            Placeholder.parsed("numerical-id", cloudSign.server?.numericalId?.toString() ?: "0"),
            Placeholder.parsed("type", cloudSign.server?.type?.toString() ?: "unknown"),
            Placeholder.parsed("host", cloudSign.server?.host ?: "unknown"),
            Placeholder.parsed("ip", cloudSign.server?.ip ?: "unknown"),
            Placeholder.parsed("port", cloudSign.server?.port?.toString() ?: "0"),
            Placeholder.parsed("min-memory", cloudSign.server?.minMemory?.toString() ?: "0"),
            Placeholder.parsed("max-memory", cloudSign.server?.maxMemory?.toString() ?: "0"),
            Placeholder.parsed("max-players", cloudSign.server?.maxPlayers?.toString() ?: "0"),
            Placeholder.parsed("player-count", cloudSign.server?.playerCount?.toString() ?: "0"),
            Placeholder.parsed("state", cloudSign.server?.state?.toString() ?: "unknown"),
        )
    }
}
