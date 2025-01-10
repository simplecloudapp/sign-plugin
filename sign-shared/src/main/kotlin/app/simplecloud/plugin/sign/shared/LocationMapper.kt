package app.simplecloud.plugin.sign.shared

import app.simplecloud.plugin.sign.shared.config.location.SignLocationConfig

interface LocationMapper<T> {

    fun map(location: SignLocationConfig): T

    fun unmap(location: T): SignLocationConfig

}