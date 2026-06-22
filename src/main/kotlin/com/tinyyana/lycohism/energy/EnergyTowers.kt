package com.tinyyana.lycohism.energy

import com.tinyyana.lycohism.Lycohism
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Remembers where 輝能 towers (日晷 / 月晷) were generated, so the [EnergyService] can emit their
 * particle aura and grant a charging bonus to nearby players. Persisted to towers.yml in the data
 * folder (structures generate as players explore; without this the registry would reset on restart).
 *
 * ponytail: a flat list + linear scan. Fine for a small co-op server's handful of towers; swap for
 * a chunk-keyed index only if a world ever accumulates thousands.
 */
class EnergyTowers(private val plugin: Lycohism) {

    data class Tower(val world: String, val x: Int, val y: Int, val z: Int, val type: EnergyType)

    private val towers = mutableListOf<Tower>()
    private val file = File(plugin.dataFolder, FILE_NAME)

    init {
        load()
    }

    fun load() {
        towers.clear()
        if (!file.exists()) return
        YamlConfiguration.loadConfiguration(file).getStringList("towers").forEach { encoded ->
            val parts = encoded.split(";")
            if (parts.size != 5) return@forEach
            val type = runCatching { EnergyType.valueOf(parts[4]) }.getOrNull() ?: return@forEach
            val x = parts[1].toIntOrNull() ?: return@forEach
            val y = parts[2].toIntOrNull() ?: return@forEach
            val z = parts[3].toIntOrNull() ?: return@forEach
            towers += Tower(parts[0], x, y, z, type)
        }
    }

    @Synchronized
    fun record(world: String, x: Int, y: Int, z: Int, type: EnergyType) {
        towers += Tower(world, x, y, z, type)
        save()
    }

    /** Forgets a tower (its multiblock was dismantled); stops its aura/production. */
    @Synchronized
    fun remove(world: String, x: Int, y: Int, z: Int): Boolean {
        val removed = towers.removeIf { it.world == world && it.x == x && it.y == y && it.z == z }
        if (removed) save()
        return removed
    }

    fun all(): List<Tower> = towers.toList()

    fun exists(world: String, x: Int, y: Int, z: Int): Boolean =
        towers.any { it.world == world && it.x == x && it.y == y && it.z == z }

    /** True when [location] is within [radius] of a loaded tower of [type]. */
    fun nearActive(location: Location, type: EnergyType, radius: Double): Boolean {
        val world = location.world ?: return false
        val r2 = radius * radius
        return towers.any { t ->
            t.type == type && t.world == world.name &&
                square(t.x - location.x) + square(t.z - location.z) + square(t.y - location.y) <= r2
        }
    }

    private fun square(d: Double) = d * d

    private fun save() {
        val yaml = YamlConfiguration()
        yaml.set("towers", towers.map { "${it.world};${it.x};${it.y};${it.z};${it.type.name}" })
        runCatching { plugin.dataFolder.mkdirs(); yaml.save(file) }
            .onFailure { plugin.logger.warning("Could not save $FILE_NAME: ${it.message}") }
    }

    private companion object {
        const val FILE_NAME = "towers.yml"
    }
}
