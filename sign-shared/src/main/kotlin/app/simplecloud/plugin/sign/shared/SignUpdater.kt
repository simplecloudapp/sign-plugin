package app.simplecloud.plugin.sign.shared

import app.simplecloud.plugin.sign.shared.config.layout.LayoutConfig
import app.simplecloud.plugin.sign.shared.config.layout.FrameConfig

fun interface SignUpdater<T> {

    fun update(cloudSign: CloudSign<T>, layoutConfig: LayoutConfig, frameConfig: FrameConfig)

}
