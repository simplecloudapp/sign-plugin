package app.simplecloud.plugin.sign.paper

import app.simplecloud.plugin.sign.shared.LocationMapper
import org.bukkit.Location

object PaperLocationMapper : LocationMapper<Location> {

    override fun map(location: Map<String, String>): Location {
        return Location.deserialize(location)
    }

    override fun unmap(location: Location): Map<String, String> {
        return location.serialize().mapValues { it.value.toString() }
    }

}