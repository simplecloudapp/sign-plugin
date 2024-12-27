package app.simplecloud.plugin.sign.shared

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the state of signs in a thread-safe manner.
 *
 * @param T The type parameter for location identification
 */
data class SignState<T : Any>(
    private val cloudSigns: ConcurrentHashMap<T, CloudSign<T>> = ConcurrentHashMap(),
    private val frameIndexes: ConcurrentHashMap<String, Int> = ConcurrentHashMap(),
    private val frameUpdates: ConcurrentHashMap<String, Long> = ConcurrentHashMap(),
) {

    private val logger: Logger = LoggerFactory.getLogger(SignState::class.java)
    private val mutex = Mutex()

    /**
     * Updates or adds a cloud sign for a specific location.
     *
     * @param location The location identifier
     * @param sign The cloud sign to update or add
     * @return The previous sign at this location, if any
     */
    suspend fun updateCloudSign(location: T, sign: CloudSign<T>): CloudSign<T>? =
        mutex.withLock {
            try {
                cloudSigns.put(location, sign).also {
                    logger.debug("Updated CloudSign at location: {}", location)
                }
            } catch (e: Exception) {
                logger.error("Failed to update CloudSign at location: {}", location, e)
                throw e
            }
        }

    /**
     * Retrieves a cloud sign for a specific location.
     *
     * @param location The location to lookup
     * @return The cloud sign if found, null otherwise
     */
    fun getCloudSign(location: T): CloudSign<T>? = cloudSigns[location]

    /**
     * Retrieves all cloud signs for a specific group.
     *
     * @param group The group identifier
     * @return List of cloud signs for the group, null if none found
     */
    fun getCloudSignsByGroup(group: String): List<CloudSign<T>>? =
        cloudSigns.values
            .filter { it.server?.group == group }
            .takeIf { it.isNotEmpty() }

    /**
     * Removes a cloud sign from a specific location.
     *
     * @param location The location to remove the sign from
     * @return The removed sign, if any
     */
    suspend fun removeCloudSign(location: T): CloudSign<T>? =
        mutex.withLock {
            try {
                cloudSigns.remove(location).also {
                    logger.debug("Removed CloudSign at location: {}", location)
                }
            } catch (e: Exception) {
                logger.error("Failed to CloudSign sign at location: {}", location, e)
                throw e
            }
        }

    /**
     * Updates the frame index for a specific layout.
     *
     * @param layoutName The name of the layout
     * @param frameCount Total number of frames in the layout
     * @return The new frame index
     */
    suspend fun updateFrameIndex(layoutName: String, frameCount: Int): Int =
        mutex.withLock {
            try {
                val currentIndex = frameIndexes.getOrDefault(layoutName, 0)
                val newIndex = (currentIndex + 1) % frameCount
                frameIndexes[layoutName] = newIndex
                frameUpdates[layoutName] = System.currentTimeMillis()
                logger.trace("Updated frame index for layout {} to {}", layoutName, newIndex)
                newIndex
            } catch (e: Exception) {
                logger.error("Failed to update frame index for layout: {}", layoutName, e)
                throw e
            }
        }

    /**
     * Gets the current frame index for a layout.
     *
     * @param layoutName The name of the layout
     * @return The current frame index, defaulting to 0
     */
    fun getCurrentFrameIndex(layoutName: String): Int =
        frameIndexes.getOrDefault(layoutName, 0)

    /**
     * Checks if enough time has passed to update a layout's frame.
     *
     * @param layoutName The name of the layout
     * @param updateInterval The minimum time between updates in milliseconds
     * @return True if the layout should be updated
     */
    fun shouldUpdateFrame(layoutName: String, updateInterval: Long): Boolean {
        val lastUpdate = frameUpdates.getOrDefault(layoutName, 0L)
        return System.currentTimeMillis() - lastUpdate >= updateInterval
    }

    /**
     * Checks if a server is already assigned to any sign.
     *
     * @param serverId The unique identifier of the server
     * @return True if the server is already assigned
     */
    fun isServerAssigned(serverId: String): Boolean =
        cloudSigns.values.any { it.server?.uniqueId == serverId }

    /**
     * Gets all locations that currently have signs.
     *
     * @return Set of all locations with signs
     */
    fun getAllLocations(): Set<T> =
        cloudSigns.keys

    /**
     * Gets all cloud signs currently managed.
     *
     * @return Collection of all cloud signs
     */
    fun getAllSigns(): Collection<CloudSign<T>> =
        cloudSigns.values

    /**
     * Clears all state information.
     */
    suspend fun clear() =
        mutex.withLock {
            try {
                cloudSigns.clear()
                frameIndexes.clear()
                frameUpdates.clear()
                logger.info("Sign state cleared successfully")
            } catch (e: Exception) {
                logger.error("Failed to clear sign state", e)
                throw e
            }
        }

    /**
     * Gets statistics about the current state.
     *
     * @return Map of statistical information
     */
    fun getStats(): Map<String, Int> {
        val stats = mapOf(
            "totalSigns" to cloudSigns.size,
            "activeLayouts" to frameIndexes.size,
            "updatedLayouts" to frameUpdates.size
        )
        logger.debug("Current state stats: {}", stats)
        return stats
    }

    companion object {
        const val DEFAULT_FRAME_INDEX = 0
    }
}
