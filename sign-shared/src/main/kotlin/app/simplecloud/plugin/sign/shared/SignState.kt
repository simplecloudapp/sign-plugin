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

    fun getCloudSign(location: T): CloudSign<T>? = cloudSigns[location]

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

    fun getCurrentFrameIndex(layoutName: String): Int =
        frameIndexes.getOrDefault(layoutName, 0)

    fun shouldUpdateFrame(layoutName: String, updateInterval: Long): Boolean {
        val lastUpdate = frameUpdates.getOrDefault(layoutName, 0L)
        return System.currentTimeMillis() - lastUpdate >= updateInterval
    }

    fun isServerAssigned(serverId: String): Boolean =
        cloudSigns.values.any { it.server?.uniqueId == serverId }

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

}
