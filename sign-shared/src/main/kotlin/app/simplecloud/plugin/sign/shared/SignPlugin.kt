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

class SignPlugin<T>(
    private val controllerApi: ControllerApi.Future,
    private val directoryPath: Path,
    private val locationMapper: LocationMapper<T>,
    private val signUpdater: SignUpdater<T>
) {

    private val cloudSigns = mutableMapOf<T, CloudSign<T>>()
    private val currentFrameIndexes = mutableMapOf<String, Int>()
    private val lastFrameUpdates = mutableMapOf<String, Long>()

    val locationsRepository = LocationsRepository(directoryPath.resolve("locations"), locationMapper)
    private val layoutRepository = LayoutRepository(directoryPath.resolve("layouts"))

    private val serverCache = ServerCache(controllerApi, locationsRepository)

    fun start() {
        locationsRepository.load()
        layoutRepository.load()

        serverCache.startCacheJob()
        startUpdateSignJob()
    }

    fun getLayout(server: Server?): LayoutConfig {
        return layoutRepository.getAll().firstOrNull { it.rule.checker.check(server) } ?: LayoutConfig()
    }

    fun getCloudSign(location: T): CloudSign<T>? {
        return cloudSigns[location]
    }

    private fun startUpdateSignJob() {

        CoroutineScope(Dispatchers.Default).launch {
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

    private fun updateSign(cloudSign: CloudSign<T>) {
        cloudSigns[cloudSign.location] = cloudSign

        val layout = getLayout(cloudSign.server)
        val currentFrameIndex = currentFrameIndexes.getOrDefault(layout.name, 0)
        if (layout.frames.isEmpty()) {
            return
        }

        val currentFrame = layout.frames[currentFrameIndex] ?: return
        signUpdater.update(cloudSign.location, currentFrame)
    }

    private fun updateLayoutIndexes() {
        layoutRepository.getAll()
            .forEach {
                val lastFrameUpdate = lastFrameUpdates.getOrDefault(it.name, 0)
                if (System.currentTimeMillis() - lastFrameUpdate < it.frameUpdateInterval) {
                    return@forEach
                }

                currentFrameIndexes.compute(it.name) { _, index ->
                    if (index == null) 0
                    else (index + 1) % it.frames.size
                }
                lastFrameUpdates[it.name] = System.currentTimeMillis()
            }
    }

}