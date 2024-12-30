package app.simplecloud.plugin.sign.shared.dispatcher

import kotlinx.coroutines.CoroutineDispatcher

interface PlatformDispatcher {

    fun getDispatcher(): CoroutineDispatcher

}