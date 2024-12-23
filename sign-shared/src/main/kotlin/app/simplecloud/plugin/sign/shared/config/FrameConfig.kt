package app.simplecloud.plugin.sign.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class FrameConfig(
    val lines: Array<String> = emptyArray(),
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FrameConfig

        return lines.contentEquals(other.lines)
    }

    override fun hashCode(): Int {
        return lines.contentHashCode()
    }
}