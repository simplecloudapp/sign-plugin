package app.simplecloud.plugin.sign.shared

interface LocationMapper<T> {

    fun map(location: Map<String, String>): T

    fun unmap(location: T): Map<String, String>

}