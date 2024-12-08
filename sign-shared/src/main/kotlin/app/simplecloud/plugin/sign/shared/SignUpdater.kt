package app.simplecloud.plugin.sign.shared

import app.simplecloud.plugin.sign.shared.config.FrameConfig

fun interface SignUpdater<T> {

    fun update(location: T, frameConfig: FrameConfig)

}