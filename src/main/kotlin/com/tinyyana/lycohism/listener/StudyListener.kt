package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.gui.StudyHolder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

/** Keeps 書房 GUI items immovable and routes its three record buttons. */
class StudyListener(private val plugin: Lycohism) : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? StudyHolder ?: return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (event.rawSlot !in 0 until event.inventory.size) return
        plugin.study.handleClick(player, holder, event.rawSlot)
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is StudyHolder) event.isCancelled = true
    }
}
