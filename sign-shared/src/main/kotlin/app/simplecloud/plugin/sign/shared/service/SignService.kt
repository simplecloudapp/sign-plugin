package app.simplecloud.plugin.sign.shared.service

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.plugin.sign.shared.CloudSign
import app.simplecloud.plugin.sign.shared.config.location.SignLocation

interface SignService<T> {
    val controllerApi: ControllerApi.Coroutine

    fun getCloudSign(location: T): CloudSign<T>?

    fun getAllLocations(): List<SignLocation>

    fun getAllGroupsRegistered(): List<String>

    fun getLocationsByGroup(group: String): List<SignLocation>?

    fun register(group: String, location: T)

    suspend fun removeCloudSign(location: T)

    fun exists(group: String): Boolean

    fun map(location: SignLocation): T

    fun unmap(location: T): SignLocation


}