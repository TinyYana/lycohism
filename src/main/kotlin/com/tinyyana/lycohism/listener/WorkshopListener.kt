package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.gui.WorkshopHolder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

/** Handles clicks inside the 工房 GUI; cancels all item movement and routes button clicks. */
class WorkshopListener(private val plugin: Lycohism) : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? WorkshopHolder ?: return
        // Cancel every click while a workshop menu is open (incl. shift-clicks from the
        // player inventory) so items can never be moved in or out.
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return
        if (event.rawSlot < 0 || event.rawSlot >= event.inventory.size) return // player inv / outside

        plugin.workshop.handleClick(player, holder, event.rawSlot)
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is WorkshopHolder) event.isCancelled = true
    }
}
