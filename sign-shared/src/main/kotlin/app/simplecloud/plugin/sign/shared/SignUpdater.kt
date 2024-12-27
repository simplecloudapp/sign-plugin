package app.simplecloud.plugin.sign.shared

import app.simplecloud.plugin.sign.shared.config.layout.FrameConfig

fun interface SignUpdater<T> {

    fun update(cloudSign: CloudSign<T>, frameConfig: FrameConfig)

}