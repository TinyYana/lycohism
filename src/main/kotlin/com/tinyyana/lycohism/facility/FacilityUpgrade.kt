package com.tinyyana.lycohism.facility

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.data.PlayerData
import com.tinyyana.lycohism.energy.EnergyType
import com.tinyyana.lycohism.util.Audit
import org.bukkit.entity.Player

/**
 * Facility level-2 upgrade — the sink that gives a 輝能核心 its purpose (PROJECT_PLAN §「v0.7 規格」§4):
 * upgrading 工房/書房/溫室 spends both materials and the nexus's stored 日輝/月輝. This closes the
 * economy loop (towers → relays → nexus → upgrade). Costs are config-driven (config.yml
 * `facility-upgrade`). Concrete per-level gameplay perks are layered on later; reaching level 2 is the
 * progression milestone here.
 */
object FacilityUpgrade {

    enum class Result { SUCCESS, NOT_REPAIRED, ALREADY_MAX, NO_STRUCTURE, NO_NEXUS, MISSING_MATERIALS, MISSING_ENERGY, UNKNOWN }

    val FACILITIES = listOf("workshop", "study", "greenhouse")

    /** Highest facility level (v0.8 added Lv3, unlocked by the 蝕輝結晶 boss drop). */
    const val MAX_LEVEL = 3

    /** Phenomenon id of the 蝕輝結晶 — the Lv2→Lv3 key dropped by the eclipse boss. */
    const val ECLIPSE_CRYSTAL_ID = "eclipse_crystal"

    /** Current stored level of [facility] for [player]. */
    fun level(plugin: Lycohism, player: Player, facility: String): Int =
        level(plugin.playerDataManager.get(player.uniqueId), facility.lowercase())

    /** The rigid upgrade structure a facility must be built into before it can reach Lv2 (v0.7.4 #3). */
    fun structureId(facility: String): String = "${facility.lowercase()}_tier_2"

    /** True when a complete upgrade structure for [facility] stands within reach of [player]. */
    fun structureComplete(plugin: Lycohism, player: Player): Boolean =
        FACILITIES.any { structureComplete(plugin, player, it) }

    fun structureComplete(plugin: Lycohism, player: Player, facility: String): Boolean {
        val multiblock = plugin.multiblockRegistry.get(structureId(facility)) ?: return false
        val world = player.world
        val base = player.location.block
        // The controller is the facility access block; the player opens/upgrades from right beside it.
        for (dx in -SCAN..SCAN) for (dy in -2..2) for (dz in -SCAN..SCAN) {
            val b = world.getBlockAt(base.x + dx, base.y + dy, base.z + dz)
            if (b.type != multiblock.controller) continue
            if (multiblock.detectRotation(world, b.x, b.y, b.z) != null) return true
        }
        return false
    }

    fun upgrade(plugin: Lycohism, player: Player, facility: String): Result {
        val id = facility.lowercase()
        if (id !in FACILITIES) return Result.UNKNOWN
        val data = plugin.playerDataManager.get(player.uniqueId)
        val level = level(data, id)
        if (level < 1) return Result.NOT_REPAIRED
        if (level >= MAX_LEVEL) return Result.ALREADY_MAX
        val target = level + 1

        // Lv1→Lv2 still requires the rigid upgrade structure; Lv2→Lv3 is gated on the 蝕輝結晶 instead.
        if (target == 2 && !structureComplete(plugin, player, id)) return Result.NO_STRUCTURE

        val nexus = plugin.nexusManager.accessibleNexus(player) ?: return Result.NO_NEXUS
        val cfg = plugin.config.getConfigurationSection("facility-upgrade")
        val (sunCost, moonCost, requirements) = costFor(plugin, cfg, target)

        if (plugin.nexusManager.get(nexus, EnergyType.SUN) < sunCost ||
            plugin.nexusManager.get(nexus, EnergyType.MOON) < moonCost
        ) return Result.MISSING_ENERGY
        if (!Cost.hasAll(player, requirements)) return Result.MISSING_MATERIALS

        plugin.nexusManager.spend(nexus, EnergyType.SUN, sunCost)
        plugin.nexusManager.spend(nexus, EnergyType.MOON, moonCost)
        Cost.consume(player, requirements)
        plugin.playerDataManager.update(player.uniqueId) { setLevel(it, id, target) }
        Audit.log(player, "facility-upgrade", "$id -> $target (sun $sunCost, moon $moonCost)")
        return Result.SUCCESS
    }

    /** Resolves (sun, moon, material+crystal requirements) for upgrading to [target] level. */
    fun costFor(
        plugin: Lycohism,
        cfg: org.bukkit.configuration.ConfigurationSection?,
        target: Int,
    ): Triple<Int, Int, List<Cost.Requirement>> {
        if (target >= 3) {
            val t3 = cfg?.getConfigurationSection("tier-3")
            val sun = (t3?.getInt("nexus-sun", 1200) ?: 1200).coerceAtLeast(0)
            val moon = (t3?.getInt("nexus-moon", 1200) ?: 1200).coerceAtLeast(0)
            val crystal = (t3?.getInt("crystal", 1) ?: 1).coerceAtLeast(0)
            val tokens = (if (crystal > 0) listOf("$ECLIPSE_CRYSTAL_ID:$crystal") else emptyList()) +
                (t3?.getStringList("materials") ?: emptyList())
            return Triple(sun, moon, Cost.parse(tokens, plugin))
        }
        val sun = (cfg?.getInt("nexus-sun", 500) ?: 500).coerceAtLeast(0)
        val moon = (cfg?.getInt("nexus-moon", 500) ?: 500).coerceAtLeast(0)
        return Triple(sun, moon, Cost.parse(cfg?.getStringList("materials") ?: emptyList(), plugin))
    }

    private fun level(data: PlayerData, id: String): Int = when (id) {
        "workshop" -> data.workshopLevel
        "study" -> data.studyLevel
        "greenhouse" -> data.greenhouseLevel
        else -> 0
    }

    private fun setLevel(data: PlayerData, id: String, level: Int) {
        when (id) {
            "workshop" -> data.workshopLevel = level
            "study" -> data.studyLevel = level
            "greenhouse" -> data.greenhouseLevel = level
        }
    }

    /** Horizontal block radius scanned around the player for the upgrade controller. */
    private const val SCAN = 3
}
