package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.gui.GreenhouseHolder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

/** Handles clicks inside the 溫室 GUI; cancels all item movement and routes button clicks. */
class GreenhouseListener(private val plugin: Lycohism) : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? GreenhouseHolder ?: return
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return
        if (event.rawSlot < 0 || event.rawSlot >= event.inventory.size) return // player inv / outside

        plugin.greenhouse.handleClick(player, holder, event.rawSlot)
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is GreenhouseHolder) event.isCancelled = true
    }
}
