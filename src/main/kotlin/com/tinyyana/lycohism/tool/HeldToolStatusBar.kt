package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

/** Combines passive main/off-hand tool readings into one action bar. */
class HeldToolStatusBar(private val plugin: Lycohism) {

    private var task: BukkitTask? = null

    fun start() {
        stop()
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            plugin.server.onlinePlayers.forEach(::showNow)
        }, UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS)
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    fun showNow(player: Player) {
        val main = player.inventory.itemInMainHand
        val off = player.inventory.itemInOffHand
        val heldIds = setOf(Items.idOf(main), Items.idOf(off))
        val readings = buildList {
            if (WindVane.ID in heldIds) {
                add(plugin.windVane.reading(player))
                plugin.playerDataManager.discover(player.uniqueId, WindVane.ID)
            }
            if (LeylineProbe.ID in heldIds) {
                add(plugin.leylineProbe.reading(player))
                plugin.playerDataManager.discover(player.uniqueId, LeylineProbe.ID)
            }
            // The pool itself is shown by the always-on boss bar (EnergyService); holding a 蓄能晶
            // adds its own reserve readout so you can see how much overflow it's banked.
            val crystal = listOf(main, off).firstOrNull { Items.idOf(it) == EnergyCrystal.ID }
            if (crystal != null) {
                add(plugin.energyCrystal.reading(crystal))
                plugin.playerDataManager.discover(player.uniqueId, EnergyCrystal.ID)
            }
        }
        if (readings.isEmpty()) return
        Messages.actionBar(player, readings.joinToString(Texts.line("messages.tools.held-status-separator")))
    }

    companion object {
        private const val UPDATE_INTERVAL_TICKS = 20L
    }
}
