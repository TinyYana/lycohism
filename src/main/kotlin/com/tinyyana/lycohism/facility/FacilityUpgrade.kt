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

    enum class Result { UPGRADED, NOT_REPAIRED, ALREADY_MAX, NO_NEXUS, MISSING_MATERIALS, MISSING_ENERGY, UNKNOWN }

    val FACILITIES = listOf("workshop", "study", "greenhouse")

    fun upgrade(plugin: Lycohism, player: Player, facility: String): Result {
        val id = facility.lowercase()
        if (id !in FACILITIES) return Result.UNKNOWN
        val data = plugin.playerDataManager.get(player.uniqueId)
        val level = level(data, id)
        if (level < 1) return Result.NOT_REPAIRED
        if (level >= 2) return Result.ALREADY_MAX

        val nexus = plugin.nexusManager.accessibleNexus(player) ?: return Result.NO_NEXUS
        val cfg = plugin.config.getConfigurationSection("facility-upgrade")
        val sunCost = (cfg?.getInt("nexus-sun", 500) ?: 500).coerceAtLeast(0)
        val moonCost = (cfg?.getInt("nexus-moon", 500) ?: 500).coerceAtLeast(0)
        val materials = Cost.parse(cfg?.getStringList("materials") ?: emptyList(), plugin)

        if (plugin.nexusManager.get(nexus, EnergyType.SUN) < sunCost ||
            plugin.nexusManager.get(nexus, EnergyType.MOON) < moonCost
        ) return Result.MISSING_ENERGY
        if (!Cost.hasAll(player, materials)) return Result.MISSING_MATERIALS

        plugin.nexusManager.spend(nexus, EnergyType.SUN, sunCost)
        plugin.nexusManager.spend(nexus, EnergyType.MOON, moonCost)
        Cost.consume(player, materials)
        plugin.playerDataManager.update(player.uniqueId) { setLevel(it, id, 2) }
        Audit.log(player, "facility-upgrade", "$id -> 2 (sun $sunCost, moon $moonCost)")
        return Result.UPGRADED
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
}
