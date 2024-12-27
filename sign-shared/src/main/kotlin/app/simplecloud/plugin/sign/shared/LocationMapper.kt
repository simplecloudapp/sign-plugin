package app.simplecloud.plugin.sign.shared

import app.simplecloud.plugin.sign.shared.config.location.SignLocation

interface LocationMapper<T> {

    fun map(location: SignLocation): T

    fun unmap(location: T): SignLocation

}