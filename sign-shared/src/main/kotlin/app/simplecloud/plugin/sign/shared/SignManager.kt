package app.simplecloud.plugin.sign.shared

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.config.LayoutConfig
import app.simplecloud.plugin.sign.shared.config.LocationsConfig
import app.simplecloud.plugin.sign.shared.repository.LayoutRepository
import app.simplecloud.plugin.sign.shared.repository.LocationsRepository
import app.simplecloud.plugin.sign.shared.rule.SignRule
import kotlinx.coroutines.*
import java.nio.file.Path

class SignManager<T>(
    val controllerApi: ControllerApi.Coroutine,
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

    fun start() {
        locationsRepository.load()
        var layouts = layoutRepository.load()

        if (layouts.isEmpty()) {
            layoutRepository.loadLayoutDefaults(SignManager::class.java.classLoader)
            layouts = layoutRepository.load()
        }

        println("Loaded ${layouts.size} Sign Layouts")

        serverCache.startCacheJob()
        startUpdateSignJob()
    }

    fun register(groupName: String, location: T) {
        locationsRepository.saveLocation(groupName, location)
    }

    fun getLayout(server: Server?): LayoutConfig {
        return layoutRepository.getAll()
            .sortedBy { it.priority }
            .firstOrNull { it.rule.checker.check(server) } ?: LayoutConfig()
    }

    fun getCloudSign(location: T): CloudSign<T>? {
        return cloudSigns[location]
    }

    fun getCloudSignsByGroup(group: String): List<CloudSign<T>>? {
        return cloudSigns.values.filter { it.server?.group == group }.takeIf { it.isNotEmpty() }
    }

    fun getAllGroupsRegistered(): List<String> {
        return locationsRepository.getAll().map { it.serverGroup }.toList()
    }

    fun getLocationsByGroup(group: String): List<Map<String, String>>? {
        return locationsRepository.find(group)?.locations
    }

    fun mapLocation(locationMap: Map<String, String>): T {
        return locationMapper.map(locationMap)
    }

    fun removeCloudSign(location: T) {
        cloudSigns.remove(location)
        locationsRepository.removeLocation(location)
    }

    fun exists(group: String): Boolean {
        return locationsRepository.getAll().any { it.serverGroup == group }
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
            val servers = serverCache.getServersByGroup(it.serverGroup)
            updateSigns(it, servers)
        }
    }

    private fun updateSigns(locationsConfig: LocationsConfig, servers: List<Server>) {
        val unusedServers = servers
            .filter { server -> !cloudSigns.values.any { it.server?.uniqueId == server.uniqueId } }
            .filter { SignRule.hasRule(it.state) }
            .sortedBy { it.numericalId }
            .iterator()

        locationsConfig.locations.forEach { locationConfig ->
            val mappedLocation = locationMapper.map(locationConfig)
            val cloudSign = cloudSigns[mappedLocation]
            val server = servers.firstOrNull { it.uniqueId == cloudSign?.server?.uniqueId }

            if (
                cloudSign == null ||
                server == null
            ) {
                val nextServer = if (unusedServers.hasNext()) unusedServers.next() else null
                val newCloudSign = CloudSign(mappedLocation, nextServer)
                updateSign(newCloudSign)
                return@forEach
            }

            val newCloudSign = cloudSign.copy(server = server)
            updateSign(newCloudSign)
        }
    }

    fun updateSign(cloudSign: CloudSign<T>) {
        cloudSigns[cloudSign.location] = cloudSign

        val layout = getLayout(cloudSign.server)
        val currentFrameIndex = currentFrameIndexes.getOrDefault(layout.name, 0)
        if (layout.frames.isEmpty()) {
            return
        }

        val currentFrame = layout.frames[currentFrameIndex]
        signUpdater.update(cloudSign, currentFrame)
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
}