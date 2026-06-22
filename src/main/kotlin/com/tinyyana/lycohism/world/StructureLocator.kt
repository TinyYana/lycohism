package com.tinyyana.lycohism.world

import com.tinyyana.lycohism.Lycohism
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

/** Small persistent index for generated and activated Lycohism structures. */
class StructureLocator(private val plugin: Lycohism) {

    data class Entry(val id: String, val world: String, val x: Int, val y: Int, val z: Int)

    private val file = File(plugin.dataFolder, "structure-index.yml")
    private val entries = mutableListOf<Entry>()

    init { load() }

    fun ids(): List<String> = (plugin.multiblockRegistry.ids() + LANDMARK_IDS).distinct()

    fun record(id: String, location: Location) {
        val entry = Entry(id, location.world.name, location.blockX, location.blockY, location.blockZ)
        if (entries.any { it.id == id && it.world == entry.world && it.x == entry.x && it.y == entry.y && it.z == entry.z }) return
        entries += entry
        save()
    }

    fun nearest(id: String, player: Player): Location? {
        val template = plugin.multiblockRegistry.get(id)
        val candidates = entries.asSequence()
            .filter { it.id == id && it.world == player.world.name }
            .map { Location(player.world, it.x + 0.5, it.y.toDouble(), it.z + 0.5) }
            .filter { location -> template == null || template.detectRotation(player.world, location.blockX, location.blockY, location.blockZ) != null }
            .toMutableList()

        if (id == "sun_tower" || id == "moon_tower") {
            val sun = id == "sun_tower"
            plugin.energyTowers.all().filter { it.world == player.world.name && (it.type.name == "SUN") == sun }
                .mapTo(candidates) { Location(player.world, it.x + 0.5, (it.y - 15).toDouble(), it.z + 0.5) }
        }
        if (id == "energy_nexus") plugin.nexusManager.allNexuses().filter { it.world == player.world.name }
            .mapTo(candidates) { Location(player.world, it.x + 0.5, it.y.toDouble(), it.z + 0.5) }
        if (id == "energy_relay") plugin.nexusManager.allRelays().filter { it.world == player.world.name }
            .mapTo(candidates) { Location(player.world, it.x + 0.5, it.y.toDouble(), it.z + 0.5) }

        return candidates.minByOrNull { it.distanceSquared(player.location) }
    }

    private fun load() {
        if (!file.exists()) return
        YamlConfiguration.loadConfiguration(file).getStringList("structures").mapNotNullTo(entries) { encoded ->
            val p = encoded.split(';')
            if (p.size != 5) null else Entry(p[0], p[1], p[2].toIntOrNull() ?: return@mapNotNullTo null, p[3].toIntOrNull() ?: return@mapNotNullTo null, p[4].toIntOrNull() ?: return@mapNotNullTo null)
        }
    }

    private fun save() {
        file.parentFile.mkdirs()
        runCatching {
            YamlConfiguration().apply {
                set("structures", entries.map { "${it.id};${it.world};${it.x};${it.y};${it.z}" })
                save(file)
            }
        }.onFailure { plugin.logger.warning("Could not save structure index: ${it.message}") }
    }

    companion object {
        val LANDMARK_IDS = listOf("moss-altar", "dew-well", "wind-circle", "rainfall-dungeon", "moondial", "sundial")
    }
}
