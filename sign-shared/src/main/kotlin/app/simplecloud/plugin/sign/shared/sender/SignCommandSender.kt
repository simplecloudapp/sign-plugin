package app.simplecloud.plugin.sign.shared.sender

import app.simplecloud.plugin.sign.shared.config.location.SignLocationConfig
import net.kyori.adventure.text.Component

interface SignCommandSender {

    fun sendMessage(component: Component)

    suspend fun getTargetBlock(maxDistance: Int): SignLocationConfig?

}