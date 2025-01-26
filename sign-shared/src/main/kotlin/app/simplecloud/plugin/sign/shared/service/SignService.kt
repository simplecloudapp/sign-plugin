package app.simplecloud.plugin.sign.shared.service

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.plugin.sign.shared.CloudSign
import app.simplecloud.plugin.sign.shared.config.location.SignLocationConfig
import app.simplecloud.plugin.sign.shared.config.rule.RuleConfig

interface SignService<T> {
    val controllerApi: ControllerApi.Coroutine

    fun getCloudSign(location: T): CloudSign<T>?

    fun getAllLocations(): List<SignLocationConfig>

    fun getAllGroupsRegistered(): List<String>

    fun getLocationsByGroup(group: String): List<SignLocationConfig>?

    fun register(group: String, location: T)

    suspend fun removeCloudSign(location: T)

    fun exists(group: String): Boolean

    fun getAllRules(): List<RuleConfig>

    fun getRule(ruleName: String): RuleConfig?

    fun map(location: SignLocationConfig): T

    fun unmap(location: T): SignLocationConfig


}