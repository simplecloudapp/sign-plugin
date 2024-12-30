package app.simplecloud.plugin.sign.shared.command

import app.simplecloud.plugin.sign.shared.config.location.SignLocation
import net.kyori.adventure.text.Component

interface SignStateManager<T> {

    suspend fun clearSign(location: T)
    suspend fun updateSign(location: SignLocation, lines: List<Component>)

}