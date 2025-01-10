package app.simplecloud.plugin.sign.shared

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.api.shared.placeholder.provider.GroupPlaceholderProvider
import app.simplecloud.plugin.api.shared.placeholder.provider.ServerPlaceholderProvider
import app.simplecloud.plugin.sign.shared.cache.ServerCache
import app.simplecloud.plugin.sign.shared.config.layout.LayoutConfig
import app.simplecloud.plugin.sign.shared.config.location.LocationsConfig
import app.simplecloud.plugin.sign.shared.config.location.SignLocationConfig
import app.simplecloud.plugin.sign.shared.config.rule.RuleConfig
import app.simplecloud.plugin.sign.shared.repository.layout.LayoutRepository
import app.simplecloud.plugin.sign.shared.repository.location.LocationsRepository
import app.simplecloud.plugin.sign.shared.repository.rule.RuleRepository
import app.simplecloud.plugin.sign.shared.rule.context.RuleContext
import app.simplecloud.plugin.sign.shared.rule.context.impl.ServerRuleContext
import app.simplecloud.plugin.sign.shared.service.SignService
import app.simplecloud.plugin.sign.shared.utils.MatcherUtil
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.nio.file.Path

class SignManager<T : Any>(
    override val controllerApi: ControllerApi.Coroutine,
    directoryPath: Path,
    private val locationMapper: LocationMapper<T>,
    private val signUpdater: SignUpdater<T>
) : SignService<T> {

    private val logger = LoggerFactory.getLogger(SignManager::class.java)

    private val state = SignState<T>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var updateJob: Job? = null

    private val locationsRepository = LocationsRepository(
        directoryPath.resolve("locations"),
        locationMapper
    )
    private val ruleRepository = RuleRepository(
        directoryPath.resolve("rules"),
    )
    private val layoutRepository = LayoutRepository(
        directoryPath.resolve("layouts"),
    )

    private val serverCache = ServerCache(controllerApi, locationsRepository)

    val groupPlaceholderProvider: GroupPlaceholderProvider = GroupPlaceholderProvider()
    val serverPlaceholderProvider: ServerPlaceholderProvider = ServerPlaceholderProvider()

    init {
        SignManagerProvider.register(this)
    }

    fun start() {
        logger.info("Starting SignManager")

        kotlin.runCatching {
            loadConfigurations()
            serverCache.startCacheJob()
            startUpdateSignJob()
        }.onFailure { error ->
            logger.error("Failed to start SignManager", error)
            throw IllegalStateException("Failed to start SignManager", error)
        }
    }

    suspend fun stop() {
        logger.info("Stopping SignManager")

        kotlin.runCatching {
            updateJob?.cancelAndJoin()
            scope.cancel()
            serverCache.stopCacheJob()
            state.clear()
        }.onFailure { error ->
            logger.error("Error during SignManager shutdown", error)
        }
    }

    override fun register(group: String, location: T) {
        logger.debug("Registering new location for group: {}", group)
        locationsRepository.saveLocation(group, location)
    }

    override fun getCloudSign(location: T): CloudSign<T>? =
        state.getCloudSign(location)

    override fun getAllLocations(): List<SignLocationConfig> =
        locationsRepository.getAll().map { it.locations }.flatten()

    override fun getAllGroupsRegistered(): List<String> =
        locationsRepository.getAll()
            .map { it.group }
            .distinct()

    override fun getLocationsByGroup(group: String): List<SignLocationConfig>? =
        locationsRepository.find(group)?.locations

    override suspend fun removeCloudSign(location: T) {
        state.removeCloudSign(location)
        locationsRepository.removeLocation(location)
    }

    override fun exists(group: String): Boolean =
        locationsRepository.getAll().any { it.group == group }

    override fun getAllRules(): List<RuleConfig> {
        return ruleRepository.getAll()
    }

    override fun getRule(ruleName: String): RuleConfig? {
        return ruleRepository.find(ruleName)
    }

    override fun map(location: SignLocationConfig): T = locationMapper.map(location)

    override fun unmap(location: T): SignLocationConfig = locationMapper.unmap(location)

    fun getLayout(ruleContext: RuleContext): LayoutConfig {
        return layoutRepository.getAll().firstOrNull {
            MatcherUtil.matches(it.matcher, ruleContext) &&
                    MatcherUtil.matches(it.rule.matcher, ruleContext)
        }
            ?: LayoutConfig()
    }

    private fun loadConfigurations() {
        locationsRepository.load()
        ruleRepository.load()
        layoutRepository.load()
        logger.info("Loaded ${locationsRepository.getAll().size} Sign Locations")
        logger.info("Loaded ${layoutRepository.getAll().size} Sign Layouts")
        logger.info("Loaded ${ruleRepository.getAll().size} Sign Rules")
    }

    private fun startUpdateSignJob() {
        updateJob = scope.launch {
            try {
                while (isActive) {
                    updateLayoutIndexes()
                    updateSigns()
                    delay(UPDATE_INTERVAL)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger.error("Error in update job", exception)
            }
        }
    }

    private suspend fun updateSigns() {
        locationsRepository.getAll().forEach { config ->
            val servers = serverCache.getServersByGroup(config.group)
            updateSigns(config, servers)
        }
    }

    private suspend fun updateSigns(locationsConfig: LocationsConfig, servers: List<Server>) {
        val unusedServers = servers.asSequence()
            .filterNot { server ->
                state.isServerAssigned(server.uniqueId)
            }
            .filter { server ->
                val ruleContext = ServerRuleContext(server)
                ruleRepository.getAll().any { rule -> MatcherUtil.matches(rule.matcher, ruleContext) }
            }
            .sortedBy { it.numericalId }
            .iterator()

        locationsConfig.locations.forEach { locationConfig ->
            processLocation(locationConfig, unusedServers, servers)
        }
    }

    private suspend fun processLocation(
        locationConfig: SignLocationConfig,
        unusedServers: Iterator<Server>,
        allServers: List<Server>
    ) {
        val mappedLocation = locationMapper.map(locationConfig)
        val existingSign = state.getCloudSign(mappedLocation)
        val currentServer = existingSign?.server?.uniqueId?.let { uniqueId ->
            allServers.firstOrNull { it.uniqueId == uniqueId }
        }

        val newSign = when {
            existingSign == null || currentServer == null ->
                CloudSign(mappedLocation, unusedServers.nextOrNull())

            else -> existingSign.copy(server = currentServer)
        }

        updateSign(newSign)
    }

    private suspend fun updateSign(cloudSign: CloudSign<T>) {
        state.updateCloudSign(cloudSign.location, cloudSign)
        val ruleContext = ServerRuleContext(cloudSign.server)

        val layout = getLayout(ruleContext)
        if (layout.frames.isEmpty()) {
            return
        }

        val currentFrameIndex = state.getCurrentFrameIndex(layout.name)
        val currentFrame = layout.frames[currentFrameIndex]
        signUpdater.update(cloudSign, currentFrame)
    }

    private suspend fun updateLayoutIndexes() {
        layoutRepository.getAll().forEach { layout ->
            if (state.shouldUpdateFrame(layout.name, layout.frameUpdateInterval)) {
                state.updateFrameIndex(layout.name, layout.frames.size)
            }
        }
    }

    companion object {
        private const val UPDATE_INTERVAL = 50L
    }
}

private fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null