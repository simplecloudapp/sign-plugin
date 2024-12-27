package app.simplecloud.plugin.sign.shared.config.layout

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class FrameConfig(
    val lines: Array<String> = emptyArray(),
) {

    init {
        require(lines.size <= 4) { "Frame cannot have more than 4 lines (current: ${lines.size})" }
    }

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