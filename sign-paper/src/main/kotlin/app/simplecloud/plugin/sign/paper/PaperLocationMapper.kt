package app.simplecloud.plugin.sign.paper

import app.simplecloud.plugin.sign.shared.LocationMapper
import app.simplecloud.plugin.sign.shared.config.location.SignLocation
import org.bukkit.Bukkit
import org.bukkit.Location

object PaperLocationMapper : LocationMapper<Location> {

    override fun map(location: SignLocation): Location {
        val world = Bukkit.getWorld(location.world)
        requireNotNull(world) { "World ${location.world} not found" }

        return Location(
            world,
            location.x,
            location.y,
            location.z,
            location.yaw,
            location.pitch,
        )
    }

    override fun unmap(location: Location): SignLocation {
        return SignLocation(
            world = location.world.name,
            x = location.x,
            y = location.y,
            z = location.z,
            pitch = location.pitch,
            yaw = location.yaw,
        )
    }

}