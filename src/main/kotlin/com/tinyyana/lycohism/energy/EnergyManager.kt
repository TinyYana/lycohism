package com.tinyyana.lycohism.energy

import com.tinyyana.lycohism.Lycohism
import org.bukkit.entity.Player

/**
 * Owns each player's 輝能 pool (日輝 / 月輝) — the energy that powers later tools and, in time,
 * facility upgrades. The pool lives on PlayerData; the portable 蓄能晶 charges outdoors and banks
 * into this pool. Caps are config-driven.
 *
 * ponytail: per-player two-Int pool, no transport graph. Energy relays / 據點 base nodes and
 * facility-upgrade consumption are the v0.7 layer on top — add when that version lands.
 */
class EnergyManager(private val plugin: Lycohism) {

    private var sunCap = 200
    private var moonCap = 200

    init {
        load()
    }

    fun load() {
        val node = plugin.config.getConfigurationSection("energy")
        sunCap = (node?.getInt("sun-cap", sunCap) ?: sunCap).coerceAtLeast(1)
        moonCap = (node?.getInt("moon-cap", moonCap) ?: moonCap).coerceAtLeast(1)
    }

    fun cap(type: EnergyType): Int = when (type) {
        EnergyType.SUN -> sunCap
        EnergyType.MOON -> moonCap
    }

    fun get(player: Player, type: EnergyType): Int {
        val data = plugin.playerDataManager.get(player.uniqueId)
        return when (type) {
            EnergyType.SUN -> data.sunEnergy
            EnergyType.MOON -> data.moonEnergy
        }
    }

    /**
     * Adds up to the cap; returns the amount actually stored. The passive per-second accrual passes
     * persist=false so it mutates only the cached data (saved on quit/disable like everything else) —
     * persisting every tick would be a disk write + advancement re-sync each second per player.
     */
    fun add(player: Player, type: EnergyType, amount: Int, persist: Boolean = true): Int {
        if (amount <= 0) return 0
        val current = get(player, type)
        val stored = (current + amount).coerceAtMost(cap(type))
        val added = stored - current
        if (added > 0) set(player, type, stored, persist)
        return added
    }

    /** Spends [amount] if available; returns false (and changes nothing) when short. */
    fun spend(player: Player, type: EnergyType, amount: Int): Boolean {
        if (amount <= 0) return true
        val current = get(player, type)
        if (current < amount) return false
        set(player, type, current - amount, persist = true)
        return true
    }

    private fun set(player: Player, type: EnergyType, value: Int, persist: Boolean) {
        val data = plugin.playerDataManager.get(player.uniqueId)
        when (type) {
            EnergyType.SUN -> data.sunEnergy = value
            EnergyType.MOON -> data.moonEnergy = value
        }
        if (persist) plugin.playerDataManager.save(player.uniqueId)
    }
}
