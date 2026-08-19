package app.simplecloud.plugin.sign.shared.command

interface SignStateManager<T> {

    suspend fun clearSign(location: T)

}