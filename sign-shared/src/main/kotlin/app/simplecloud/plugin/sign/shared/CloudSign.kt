package app.simplecloud.plugin.sign.shared

import app.simplecloud.api.server.Server

data class CloudSign<T>(

    val location: T,
    val server: Server?

)