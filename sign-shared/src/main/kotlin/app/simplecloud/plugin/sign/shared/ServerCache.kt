package app.simplecloud.plugin.sign.shared

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.repository.LocationsRepository
import com.google.common.collect.Multimaps
import kotlinx.coroutines.*

class ServerCache<T>(
    private val controllerApi: ControllerApi.Future,
    private val locationsRepository: LocationsRepository<T>
) {

    private val serverCache = Multimaps.synchronizedMultimap(Multimaps.newListMultimap<String, Server>(mutableMapOf(), ::arrayListOf))

    fun startCacheJob(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                updateCache()
                delay(2000)
            }
        }
    }

    fun getServersByGroup(group: String): List<Server> {
        return serverCache[group].orEmpty().toList()
    }

    private fun updateCache() {
        locationsRepository.getAll()
            .map { it.serverGroup }
            .toSet()
            .forEach { group ->
                controllerApi.getServers().getServersByGroup(group).thenAccept {
                    serverCache.replaceValues(group, it)
                }
            }
    }

}