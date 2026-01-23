package app.simplecloud.plugin.sign.shared.service

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.sign.shared.CloudSign
import app.simplecloud.plugin.sign.shared.config.location.LocationsConfig
import app.simplecloud.plugin.sign.shared.config.location.SignLocation

interface SignService<T> {
    val controllerApi: CloudApi

    fun getCloudSign(location: T): CloudSign<T>?

    fun getAllLocations(): List<SignLocation>

    fun getAllConfigs(): List<LocationsConfig>

    fun getLocationsByKey(key: String): List<SignLocation>?

    fun registerForGroup(group: String, location: T)

    fun registerForPersistentServer(persistentServerId: String, location: T)

    suspend fun removeCloudSign(location: T)

    fun exists(key: String): Boolean

    fun map(location: SignLocation): T

    fun unmap(location: T): SignLocation
}