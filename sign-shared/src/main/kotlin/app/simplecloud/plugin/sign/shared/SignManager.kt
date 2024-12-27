package app.simplecloud.plugin.sign.shared

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.cache.ServerCache
import app.simplecloud.plugin.sign.shared.config.layout.LayoutConfig
import app.simplecloud.plugin.sign.shared.config.location.LocationsConfig
import app.simplecloud.plugin.sign.shared.config.location.SignLocation
import app.simplecloud.plugin.sign.shared.config.matcher.MatcherConfigEntry
import app.simplecloud.plugin.sign.shared.config.matcher.MatcherType
import app.simplecloud.plugin.sign.shared.repository.layout.LayoutRepository
import app.simplecloud.plugin.sign.shared.repository.location.LocationsRepository
import app.simplecloud.plugin.sign.shared.rule.RuleRegistry
import app.simplecloud.plugin.sign.shared.rule.SignRule
import app.simplecloud.plugin.sign.shared.rule.impl.RuleContext
import app.simplecloud.plugin.sign.shared.rule.serialize.SignRuleSerializer
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import java.nio.file.Path

class SignManager<T : Any>(
    controllerApi: ControllerApi.Coroutine,
    directoryPath: Path,
    private val locationMapper: LocationMapper<T>,
    private val ruleRegistry: RuleRegistry,
    private val signUpdater: SignUpdater<T>
) {

    private val logger = LoggerFactory.getLogger(SignManager::class.java)

    private val state = SignState<T>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var updateJob: Job? = null

    private val locationsRepository = LocationsRepository(
        directoryPath.resolve("locations"),
        locationMapper
    )
    private val layoutRepository = LayoutRepository(
        directoryPath.resolve("layouts"),
    )

    private val serializers = TypeSerializerCollection.defaults().childBuilder().apply {
        register(SignRule::class.java, SignRuleSerializer(ruleRegistry))
    }.build()

    private val serverCache = ServerCache(controllerApi, locationsRepository)

    init {
        setRuleRegistry(ruleRegistry)
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
        }.onFailure { error ->
            logger.error("Error during SignManager shutdown", error)
        }
    }

    fun register(groupName: String, location: T) {
        logger.debug("Registering new location for group: {}", groupName)
        locationsRepository.saveLocation(groupName, location)
    }

    fun getLayout(context: RuleContext): LayoutConfig {
        val serverName = "${context.server?.group}-${context.server?.numericalId}"

        return layoutRepository.getAll()
            .asSequence()
            .sortedByDescending { it.priority }
            .filter { layoutConfig ->
                val rule = ruleRegistry.getRule(layoutConfig.rule.getRuleName())
                rule?.checker?.check(context) == true
            }.firstOrNull { layoutConfig -> checkMatches(layoutConfig.matcher, serverName) }
            ?: LayoutConfig()
    }

    private fun checkMatches(matchers: Map<MatcherType, List<MatcherConfigEntry>>, serverName: String): Boolean {
        if (matchers.containsKey(MatcherType.MATCH_ALL)) {
            val matchAllResult = matchers[MatcherType.MATCH_ALL]?.all {
                it.operation.matches(serverName, it.value)
            } ?: false

            if (!matchAllResult) {
                return false
            }
        }

        if (matchers.containsKey(MatcherType.MATCH_ANY)) {
            return matchers[MatcherType.MATCH_ANY]?.any {
                it.operation.matches(serverName, it.value)
            } ?: false
        }

        return true
    }

    fun getCloudSign(location: T): CloudSign<T>? =
        state.getCloudSign(location)

    fun getAllGroupsRegistered(): List<String> =
        locationsRepository.getAll()
            .map { it.group }
            .distinct()

    fun getLocationsByGroup(group: String): List<SignLocation>? =
        locationsRepository.find(group)?.locations

    fun mapLocation(location: SignLocation): T =
        locationMapper.map(location)

    suspend fun removeCloudSign(location: T) {
        state.removeCloudSign(location)
        locationsRepository.removeLocation(location)
    }

    fun exists(group: String): Boolean =
        locationsRepository.getAll().any { it.group == group }

    private fun loadConfigurations() {
        locationsRepository.load()
        layoutRepository.load(serializers)
        logger.info("Loaded {} Sign Layouts", layoutRepository.getAll().size)
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
                val context = RuleContext(server, server.state)
                ruleRegistry.getRules().any { rule -> rule.checker.check(context) }
            }
            .sortedBy { it.numericalId }
            .iterator()

        locationsConfig.locations.forEach { locationConfig ->
            processLocation(locationConfig, unusedServers, servers)
        }
    }

    private suspend fun processLocation(
        locationConfig: SignLocation,
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

        val context = RuleContext(cloudSign.server, cloudSign.server?.state)

        val layout = getLayout(context)
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

        private var staticRuleRegistry: RuleRegistry? = null

        fun getRuleRegistry(): RuleRegistry? = staticRuleRegistry

        fun setRuleRegistry(registry: RuleRegistry) {
            staticRuleRegistry = registry
        }
    }
}

private fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null