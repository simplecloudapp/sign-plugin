package app.simplecloud.plugin.sign.shared.cache

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.repository.location.LocationsRepository
import com.google.common.collect.Multimaps
import kotlinx.coroutines.*

class ServerCache<T>(
    private val controllerApi: ControllerApi.Coroutine,
    private val locationsRepository: LocationsRepository<T>
) {

    private val serverCache =
        Multimaps.synchronizedMultimap(Multimaps.newListMultimap<String, Server>(mutableMapOf(), ::arrayListOf))

    private var cacheJob: Job? = null

    fun startCacheJob() {
        cacheJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                updateCache()
                delay(1000)
            }
        }
    }

    fun stopCacheJob() {
        cacheJob?.cancel()
        serverCache.clear()
    }

    fun getServersByGroup(group: String): List<Server> {
        return serverCache[group].orEmpty().toList()
    }

    private suspend fun updateCache() {
        locationsRepository.getAll()
            .asSequence()
            .map { it.group }
            .toSet()
            .forEach { group ->
                val servers = controllerApi.getServers().getServersByGroup(group)
                serverCache.replaceValues(group, servers)
            }
    }
}