package app.simplecloud.plugin.sign.paper

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.plugin.sign.paper.dispatcher.PaperPlatformDispatcher
import app.simplecloud.plugin.sign.paper.sender.PaperCommandSender
import app.simplecloud.plugin.sign.paper.sender.PaperCommandSenderMapper
import app.simplecloud.plugin.sign.paper.service.PaperSignService
import app.simplecloud.plugin.sign.shared.CloudSign
import app.simplecloud.plugin.sign.shared.SignManager
import app.simplecloud.plugin.sign.shared.command.SignCommand
import app.simplecloud.plugin.sign.shared.config.layout.FrameConfig
import app.simplecloud.plugin.sign.shared.utils.MatcherUtil
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager
import org.slf4j.LoggerFactory

@Suppress("UnstableApiUsage")
class PaperSignsPluginBootstrap : PluginBootstrap {

    private val logger = LoggerFactory.getLogger(PaperSignsPluginBootstrap::class.java)
    private val controllerApi = ControllerApi.createCoroutineApi()
    private val miniMessage = MiniMessage.miniMessage()

    private var isEnabled = false

    lateinit var signManager: SignManager<Location>
        private set
    lateinit var commandManager: PaperCommandManager.Bootstrapped<PaperCommandSender>
        private set

    val platformDispatcher = PaperPlatformDispatcher(this)
    private var signCommand: SignCommand<PaperCommandSender, Location>? = null


    val plugin by lazy { PaperSignsPlugin(this) }

    override fun bootstrap(context: BootstrapContext) {
        try {
            isEnabled = true

            initializeSignManager(context)
            initializeCommandManager(context)
            registerCommands()

            logger.info("Successfully bootstrapped PaperSignsPlugin")
        } catch (e: Exception) {
            logger.error("Failed to bootstrap PaperSignsPlugin", e)
            throw e
        }
    }


    private fun initializeSignManager(context: BootstrapContext) {
        signManager = SignManager(
            controllerApi,
            context.dataDirectory,
            PaperSignService(this),
        ) { cloudSign, frameConfig ->
            updateSign(cloudSign, frameConfig)
        }
    }

    private fun initializeCommandManager(context: BootstrapContext) {
        commandManager = PaperCommandManager.builder(PaperCommandSenderMapper(this))
            .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
            .buildBootstrapped(context)

    }

    private fun registerCommands() {
        signCommand = SignCommand(
            commandManager,
            PaperSignService(this),
            PaperSignStateManager(this),
            platformDispatcher
        ).apply {
            register()
        }
    }

    override fun createPlugin(context: PluginProviderContext): JavaPlugin = plugin

    fun disable() {
        isEnabled = false
        runBlocking {
            try {
                signCommand?.cleanup()
                signManager.stop()
                logger.info("Successfully cleaned up PaperSignsPlugin resources")
            } catch (e: Exception) {
                logger.error("Error during plugin cleanup", e)
            }
        }
    }

    private fun updateSign(cloudSign: CloudSign<Location>, frameConfig: FrameConfig) {
        if (!isEnabled || !plugin.isEnabled) {
            logger.debug("Skipping sign update - plugin is disabled")
            return
        }

        val location = cloudSign.location
        plugin.server.scheduler.runTask(plugin, Runnable {
            try {
                val sign = location.block.state as? Sign ?: return@Runnable

                updateSignLines(sign, frameConfig, cloudSign)
            } catch (e: Exception) {
                logger.error("Failed to update sign at location: $location", e)
            }
        })
    }

    private fun updateSignLines(sign: Sign, frameConfig: FrameConfig, cloudSign: CloudSign<*>) {
        clearSignLines(sign)

        frameConfig.lines.forEachIndexed { index, line ->
            val resolvedLine = miniMessage.deserialize(MatcherUtil.resolveAllPlaceholders(line, cloudSign.server))
            sign.getSide(Side.FRONT).line(index, resolvedLine)
            sign.getSide(Side.BACK).line(index, resolvedLine)
        }

        sign.update()
    }

    private fun clearSignLines(sign: Sign) {
        val emptyComponent = Component.empty()
        sign.getSide(Side.FRONT).lines().replaceAll { emptyComponent }
        sign.getSide(Side.BACK).lines().replaceAll { emptyComponent }
    }
}
