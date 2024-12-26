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
import app.simplecloud.plugin.sign.shared.rule.SignRule
import kotlinx.coroutines.*
import java.nio.file.Path

class SignManager<T>(
    controllerApi: ControllerApi.Coroutine,
    directoryPath: Path,
    private val locationMapper: LocationMapper<T>,
    private val signUpdater: SignUpdater<T>
) {

    private val cloudSigns = mutableMapOf<T, CloudSign<T>>()
    private val currentFrameIndexes = mutableMapOf<String, Int>()
    private val lastFrameUpdates = mutableMapOf<String, Long>()

    private val locationsRepository = LocationsRepository(directoryPath.resolve("locations"), locationMapper)
    private val layoutRepository = LayoutRepository(directoryPath.resolve("layouts"))

    private val serverCache = ServerCache(controllerApi, locationsRepository)

    private var updateJob: Job? = null

    fun start() {
        locationsRepository.load()
        layoutRepository.load()

        println("Loaded ${layoutRepository.getAll().size} Sign Layouts")

        serverCache.startCacheJob()
        startUpdateSignJob()
    }

    fun stop() {
        updateJob?.cancel()
        serverCache.stopCacheJob()
    }

    fun register(groupName: String, location: T) {
        locationsRepository.saveLocation(groupName, location)
    }

    fun getLayout(server: Server?): LayoutConfig {
        val serverName = "${server?.group}-${server?.numericalId}"

        return layoutRepository.getAll()
            .sortedBy { it.priority }
            .filter { it.rule.checker.check(server) }
            .firstOrNull { layout -> checkMatches(layout.matcher, serverName) }
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

    fun getCloudSign(location: T): CloudSign<T>? {
        return cloudSigns[location]
    }

    fun getCloudSignsByGroup(group: String): List<CloudSign<T>>? {
        return cloudSigns.values
            .filter { it.server?.group == group }
            .takeIf { it.isNotEmpty() }
    }

    fun getAllGroupsRegistered(): List<String> {
        return locationsRepository.getAll().map { it.group }.toList()
    }

    fun getLocationsByGroup(group: String): List<SignLocation>? {
        return locationsRepository.find(group)?.locations
    }

    fun mapLocation(location: SignLocation): T {
        return locationMapper.map(location)
    }

    fun removeCloudSign(location: T) {
        cloudSigns.remove(location)
        locationsRepository.removeLocation(location)
    }

    fun exists(group: String): Boolean {
        return locationsRepository.getAll().any { it.group == group }
    }

    private fun startUpdateSignJob() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                updateLayoutIndexes()
                updateSigns()
                delay(50)
            }
        }
    }

    private fun updateSigns() {
        locationsRepository.getAll().forEach {
            val servers = serverCache.getServersByGroup(it.group)
            updateSigns(it, servers)
        }
    }

    private fun updateSigns(locationsConfig: LocationsConfig, servers: List<Server>) {
        val unusedServers = servers
            .filterNot { server ->
                cloudSigns.values.any { it.server?.uniqueId == server.uniqueId }
            }
            .filter { SignRule.hasRule(it.state) }
            .sortedBy { it.numericalId }
            .iterator()

        locationsConfig.locations.forEach { locationConfig ->
            val mappedLocation = locationMapper.map(locationConfig)
            val cloudSign = cloudSigns[mappedLocation]
            val server = cloudSign?.server?.uniqueId?.let { uniqueId ->
                servers.firstOrNull { it.uniqueId == uniqueId }
            }

            val newCloudSign = when {
                cloudSign == null || server == null -> CloudSign(mappedLocation, unusedServers.nextOrNull())
                else -> cloudSign.copy(server = server)
            }

            updateSign(newCloudSign)
        }
    }

    fun updateSign(cloudSign: CloudSign<T>) {
        cloudSigns[cloudSign.location] = cloudSign

        val layout = getLayout(cloudSign.server)
        if (layout.frames.isEmpty()) return

        val currentFrameIndex = currentFrameIndexes.getOrDefault(layout.name, 0)
            .coerceIn(0, layout.frames.lastIndex)

        signUpdater.update(cloudSign, layout.frames[currentFrameIndex])
    }

    private fun updateLayoutIndexes() {
        val currentTime = System.currentTimeMillis()

        layoutRepository.getAll().forEach { layout ->
            val lastFrameUpdate = lastFrameUpdates.getOrDefault(layout.name, 0)
            if (currentTime - lastFrameUpdate < layout.frameUpdateInterval) return@forEach

            val currentIndex = currentFrameIndexes[layout.name] ?: 0
            currentFrameIndexes[layout.name] = (currentIndex + 1) % layout.frames.size
            lastFrameUpdates[layout.name] = currentTime
        }
    }

    private fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null
}