package app.simplecloud.plugin.sign.shared.command

import app.simplecloud.plugin.sign.shared.config.location.SignLocationConfig
import net.kyori.adventure.text.Component

interface SignStateManager<T> {

    suspend fun clearSign(location: T)
    suspend fun updateSign(location: SignLocationConfig, lines: List<Component>)

}