package app.simplecloud.plugin.sign.paper.util

import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Rotatable
import org.bukkit.block.data.type.WallSign

/**
 * Resolves the direction a sign block is facing, independent of whether it's a
 * wall sign (6-way facing) or a standing sign (16-way rotation).
 */
fun BlockData.signFacing(): BlockFace? = when (this) {
    is WallSign -> facing
    is Rotatable -> rotation.toCardinal()
    else -> null
}

fun BlockData.resolveSignDirection(): String? = when (this) {
    is WallSign -> "WALL:${facing.name}"
    is Rotatable -> "STANDING:${rotation.name}"
    else -> null
}

/**
 * Standing-sign rotation is 16-way; snaps it down to the nearest of the 4
 * horizontal cardinal faces so it can be used to find an adjacent block.
 */
private fun BlockFace.toCardinal(): BlockFace = when (this) {
    BlockFace.NORTH_NORTH_WEST, BlockFace.NORTH, BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_WEST ->
        BlockFace.NORTH

    BlockFace.EAST_NORTH_EAST, BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.NORTH_EAST ->
        BlockFace.EAST

    BlockFace.SOUTH_SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_EAST ->
        BlockFace.SOUTH

    BlockFace.WEST_SOUTH_WEST, BlockFace.WEST, BlockFace.WEST_NORTH_WEST, BlockFace.SOUTH_WEST ->
        BlockFace.WEST

    else -> BlockFace.NORTH
}
