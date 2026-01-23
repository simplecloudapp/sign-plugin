package app.simplecloud.plugin.sign.shared.cache

import app.simplecloud.api.CloudApi
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerQuery
import app.simplecloud.plugin.sign.shared.config.location.LocationsConfig
import app.simplecloud.plugin.sign.shared.repository.location.LocationsRepository
import com.google.common.collect.Multimaps
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await

class ServerCache<T : Any>(
    private val controllerApi: CloudApi,
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

    fun getServers(config: LocationsConfig): List<Server> {
        return serverCache[config.getIdentifier()].orEmpty().toList()
    }

    private suspend fun updateCache() {
        locationsRepository.getAll()
            .asSequence()
            .distinctBy { it.getIdentifier() }
            .forEach { config ->
                val servers = fetchServers(config)
                serverCache.replaceValues(config.getIdentifier(), servers)
            }
    }

    private suspend fun fetchServers(config: LocationsConfig): List<Server> {
        return when {
            config.isGroup() -> {
                controllerApi.server().getServersByGroup(config.group!!).await()
            }
            config.isPersistentServer() -> {
                val persistentServer = controllerApi.persistentServer()
                    .getPersistentServerByName(config.persistentServer!!)
                    .await()
                val query = ServerQuery.create()
                    .filterByPersistentServerId(persistentServer.persistentServerId)
                controllerApi.server().getAllServers(query).await()
            }
            else -> emptyList()
        }
    }
}