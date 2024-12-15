package app.simplecloud.plugin.sign.paper

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.plugin.sign.shared.CloudSign
import app.simplecloud.plugin.sign.shared.SignPlugin
import app.simplecloud.plugin.sign.shared.SignUpdater
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
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

class PaperSignsPluginBootstrap : PluginBootstrap {

    private val controllerApi = ControllerApi.createCoroutineApi()
    private val miniMessage = MiniMessage.miniMessage()

    private lateinit var signPlugin: SignPlugin<Location>
    private lateinit var commandManager: PaperCommandManager.Bootstrapped<CommandSourceStack>

    private val plugin by lazy { PaperSignsPlugin(signPlugin, commandManager) }

    override fun bootstrap(context: BootstrapContext) {
        signPlugin = SignPlugin(
            controllerApi,
            context.dataDirectory,
            PaperLocationMapper,
            SignUpdater { cloudSign, frameConfig ->
                val location = cloudSign.location
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    val sign = location.block.state as? Sign ?: return@Runnable

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
        )

        commandManager = PaperCommandManager.builder()
            .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
            .buildBootstrapped(context)

        context.pluginSource

        SignCommand(signPlugin, commandManager).register()
    }

    override fun createPlugin(context: PluginProviderContext): JavaPlugin {
        return plugin
    }

    /**
     *  public final val uniqueId: kotlin.String /* compiled code */
     *
     *     public final val type: build.buf.gen.simplecloud.controller.v1.ServerType /* compiled code */
     *
     *     public final val group: kotlin.String /* compiled code */
     *
     *     public final val host: kotlin.String? /* compiled code */
     *
     *     public final val numericalId: kotlin.Int /* compiled code */
     *
     *     public final val ip: kotlin.String /* compiled code */
     *
     *     public final val port: kotlin.Long /* compiled code */
     *
     *     public final val minMemory: kotlin.Long /* compiled code */
     *
     *     public final val maxMemory: kotlin.Long /* compiled code */
     *
     *     public final val maxPlayers: kotlin.Long /* compiled code */
     *
     *     public final var playerCount: kotlin.Long /* compiled code */
     *
     *     public final val properties: kotlin.collections.MutableMap<kotlin.String, kotlin.String> /* compiled code */
     *
     *     public final var state: build.buf.gen.simplecloud.controller.v1.ServerState /* compiled code */
     *
     *     public final val createdAt: java.time.LocalDateTime /* compiled code */
     *
     *     public final val updatedAt: java.time.LocalDateTime /* compiled code */
     */

    private fun getPlaceholders(cloudSign: CloudSign<*>): List<TagResolver.Single> {
        return listOf(
            Placeholder.parsed("group", cloudSign.server?.group?: "unkwown"),
            Placeholder.parsed("numerical-id", cloudSign.server?.numericalId?.toString()?: "0"),
            Placeholder.parsed("type", cloudSign.server?.type?.toString()?: "unknown"),
            Placeholder.parsed("host", cloudSign.server?.host?: "unknown"),
            Placeholder.parsed("ip", cloudSign.server?.ip?: "unknown"),
            Placeholder.parsed("port", cloudSign.server?.port?.toString()?: "0"),
            Placeholder.parsed("min-memory", cloudSign.server?.minMemory?.toString()?: "0"),
            Placeholder.parsed("max-memory", cloudSign.server?.maxMemory?.toString()?: "0"),
            Placeholder.parsed("max-players", cloudSign.server?.maxPlayers?.toString()?: "0"),
            Placeholder.parsed("player-count", cloudSign.server?.playerCount?.toString()?: "0"),
            Placeholder.parsed("state", cloudSign.server?.state?.toString()?: "unknown"),
        )
    }

}