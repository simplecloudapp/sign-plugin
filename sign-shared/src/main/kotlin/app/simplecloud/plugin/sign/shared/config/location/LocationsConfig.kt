package app.simplecloud.plugin.sign.shared.config.location

import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * Configuration for sign locations.
 * Either [group] or [persistentServer] must be set, but not both.
 */
@ConfigSerializable
data class LocationsConfig(
    val group: String? = null,
    val persistentServer: String? = null,
    val locations: List<SignLocation> = emptyList()
) {
    /**
     * Returns the identifier used for caching and lookup.
     */
    fun getIdentifier(): String = group ?: persistentServer ?: ""

    /**
     * Returns true if this config is for a persistent server.
     */
    fun isPersistentServer(): Boolean = !persistentServer.isNullOrEmpty()

    /**
     * Returns true if this config is for a group.
     */
    fun isGroup(): Boolean = !group.isNullOrEmpty()
}
