package com.tinyyana.lycohism.puzzle

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Audit
import com.tinyyana.lycohism.util.Items
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

/**
 * 封印祠 per-player lock system. A sealed shrine generates in the world with a chiseled-deepslate
 * controller and a reward chest; the chest is blocked (InventoryOpenEvent) until the player
 * right-clicks the controller while carrying the required discovery + item. Solved state is
 * stored in PlayerData.unsealed. World-side registrations persist in seals.yml.
 */
class SealManager(private val plugin: Lycohism) {

    data class SealEntry(val controllerKey: String, val chestKey: String)

    enum class UnlockResult { UNLOCKED, ALREADY_OPEN, MISSING_DISCOVERY, MISSING_ITEM, NOT_A_SEAL }

    private val entries = mutableMapOf<String, SealEntry>()
    private val chestIndex = mutableMapOf<String, String>() // chestKey → controllerKey
    private val file = File(plugin.dataFolder, FILE_NAME)

    var requiredDiscovery = "energy_nexus"
        private set
    var requiredItem = "energy_crystal"
        private set

    init {
        load()
    }

    fun load() {
        val yaml = if (file.exists()) YamlConfiguration.loadConfiguration(file) else YamlConfiguration()
        requiredDiscovery = yaml.getString("condition.discovery", "energy_nexus")!!
        requiredItem = yaml.getString("condition.item", "energy_crystal")!!
        entries.clear()
        chestIndex.clear()
        for (line in yaml.getStringList("seals")) {
            val parts = line.split("|")
            if (parts.size != 2) continue
            val ck = parts[0]; val chestK = parts[1]
            entries[ck] = SealEntry(ck, chestK)
            chestIndex[chestK] = ck
        }
        plugin.logger.info("Loaded ${entries.size} sealed shrines.")
    }

    fun save() {
        plugin.dataFolder.mkdirs()
        val yaml = YamlConfiguration()
        yaml.set("condition.discovery", requiredDiscovery)
        yaml.set("condition.item", requiredItem)
        yaml.set("seals", entries.values.map { "${it.controllerKey}|${it.chestKey}" })
        yaml.save(file)
    }

    /** Called by StructureGenerator when a new shrine is placed. */
    fun register(controller: Location, chest: Location) {
        val ck = locKey(controller)
        val chestK = locKey(chest)
        entries[ck] = SealEntry(ck, chestK)
        chestIndex[chestK] = ck
        save()
    }

    fun isSealController(block: Block): Boolean = blockKey(block) in entries

    /** True if this chest belongs to a seal the player hasn't yet solved. */
    fun isSealedChestFor(player: Player, block: Block): Boolean {
        val ck = chestIndex[blockKey(block)] ?: return false
        return ck !in plugin.playerDataManager.get(player.uniqueId).unsealed
    }

    /**
     * Checks conditions and, on success, records the solve and fires visual feedback.
     * Returns [UnlockResult.NOT_A_SEAL] if [block] is not a registered controller.
     */
    fun tryUnlock(player: Player, block: Block): UnlockResult {
        val ck = blockKey(block)
        if (ck !in entries) return UnlockResult.NOT_A_SEAL
        val data = plugin.playerDataManager.get(player.uniqueId)
        if (ck in data.unsealed) return UnlockResult.ALREADY_OPEN
        val heldId = Items.idOf(player.inventory.itemInMainHand) ?: ""
        val result = canUnlock(data.discoveries, heldId, requiredDiscovery, requiredItem)
        if (result != UnlockResult.UNLOCKED) return result

        data.unsealed.add(ck)
        plugin.playerDataManager.save(player.uniqueId)
        plugin.playerDataManager.discover(player.uniqueId, "sealed_shrine")
        val centre = block.location.add(0.5, 0.5, 0.5)
        block.world.spawnParticle(Particle.END_ROD, centre, 60, 1.0, 1.0, 1.0, 0.05)
        block.world.spawnParticle(Particle.WITCH, centre, 30, 0.6, 0.6, 0.6, 0.01)
        block.world.playSound(centre, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.8f)
        Audit.log(player, "seal-unlock", ck)
        return UnlockResult.UNLOCKED
    }

    companion object {
        const val FILE_NAME = "seals.yml"

        // Bukkit-free pure predicate extracted for unit testing.
        fun canUnlock(
            discoveries: Set<String>,
            heldId: String,
            requiredDiscovery: String,
            requiredItem: String,
        ): UnlockResult = when {
            requiredDiscovery !in discoveries -> UnlockResult.MISSING_DISCOVERY
            heldId != requiredItem -> UnlockResult.MISSING_ITEM
            else -> UnlockResult.UNLOCKED
        }

        fun blockKey(block: Block): String = "${block.world.name},${block.x},${block.y},${block.z}"
        private fun locKey(loc: Location): String = "${loc.world!!.name},${loc.blockX},${loc.blockY},${loc.blockZ}"
    }
}
