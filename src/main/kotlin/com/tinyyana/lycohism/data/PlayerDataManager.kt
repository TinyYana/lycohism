package com.tinyyana.lycohism.data

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Items
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads, caches and persists [PlayerData]. Storage is one YAML file per player under
 * plugins/lycohism/playerdata/, which keeps the skeleton dependency-free and easy to
 * inspect by hand. Can be swapped for SQLite later without touching callers.
 */
class PlayerDataManager(private val plugin: Lycohism) {

    private val folder = File(plugin.dataFolder, "playerdata").apply { mkdirs() }
    private val cache = ConcurrentHashMap<UUID, PlayerData>()

    /** Returns cached data, loading from disk if needed. */
    fun get(uuid: UUID): PlayerData = cache[uuid] ?: load(uuid)

    fun load(uuid: UUID): PlayerData {
        cache[uuid]?.let { return it }

        val data = PlayerData(uuid)
        val file = fileFor(uuid)
        if (file.exists()) {
            val yaml = YamlConfiguration.loadConfiguration(file)
            data.discoveries.addAll(yaml.getStringList("discoveries"))
            data.knownMaterials.addAll(yaml.getStringList("known-materials"))
            data.workshopLevel = yaml.getInt("workshop-level", 0)
            data.studyLevel = yaml.getInt("study-level", 0)
            data.greenhouseLevel = yaml.getInt("greenhouse-level", 0)
            data.confirmVanillaCrafts = yaml.getBoolean("confirm-vanilla-crafts", true)
            data.confirmStructureBreaks = yaml.getBoolean("confirm-structure-breaks", true)
            data.sunEnergy = yaml.getInt("sun-energy", 0)
            data.moonEnergy = yaml.getInt("moon-energy", 0)
        }
        cache[uuid] = data
        return data
    }

    fun save(uuid: UUID) {
        val data = cache[uuid] ?: return
        val yaml = YamlConfiguration()
        yaml.set("discoveries", data.discoveries.toList())
        yaml.set("known-materials", data.knownMaterials.toList())
        yaml.set("workshop-level", data.workshopLevel)
        yaml.set("study-level", data.studyLevel)
        yaml.set("greenhouse-level", data.greenhouseLevel)
        yaml.set("confirm-vanilla-crafts", data.confirmVanillaCrafts)
        yaml.set("confirm-structure-breaks", data.confirmStructureBreaks)
        yaml.set("sun-energy", data.sunEnergy)
        yaml.set("moon-energy", data.moonEnergy)
        runCatching { saveAtomically(uuid, yaml) }
            .onFailure { plugin.logger.warning("Failed to save player data for $uuid: ${it.message}") }
    }

    /** Applies one progression change and persists it immediately. */
    fun update(uuid: UUID, change: (PlayerData) -> Unit): PlayerData {
        val data = get(uuid)
        change(data)
        save(uuid)
        syncProgression(uuid)
        return data
    }

    /** Records a discovery once and writes only when the set actually changed. */
    fun discover(uuid: UUID, id: String): Boolean {
        val data = get(uuid)
        if (!data.discoveries.add(id)) return false
        save(uuid)
        syncProgression(uuid)
        return true
    }

    fun discoverMaterial(uuid: UUID, material: Material): Boolean {
        val data = get(uuid)
        if (!data.knownMaterials.add(material.name)) return false
        save(uuid)
        return true
    }

    /** Reveals Lycohism materials already held, including chest and admin grants. */
    fun rememberInventoryMaterials(player: Player): PlayerData {
        val data = get(player.uniqueId)
        val found = player.inventory.contents.asSequence()
            .mapNotNull(Items::idOf)
            .toSet()
        if (data.discoveries.addAll(found)) {
            save(player.uniqueId)
            syncProgression(player.uniqueId)
        }
        return data
    }

    fun saveAndUnload(uuid: UUID) {
        save(uuid)
        cache.remove(uuid)
    }

    fun saveAll() {
        cache.keys.forEach { save(it) }
    }

    private fun saveAtomically(uuid: UUID, yaml: YamlConfiguration) {
        folder.mkdirs()
        val target = fileFor(uuid)
        val temporary = File(folder, ".$uuid.yml.tmp")
        yaml.save(temporary)
        runCatching {
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }.getOrElse {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun fileFor(uuid: UUID) = File(folder, "$uuid.yml")

    private fun syncProgression(uuid: UUID) {
        plugin.server.getPlayer(uuid)?.let { plugin.progressionManager.syncAdvancements(it) }
    }
}
