package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Items
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent

/** Routes natural-phenomenon pickups into the first 月紗袋 with free capacity. */
class MoonPouchListener(private val plugin: Lycohism) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val dropped = event.item.itemStack
        val id = Items.idOf(dropped)
        if (id == null) {
            plugin.playerDataManager.discoverMaterial(player.uniqueId, dropped.type)
            return
        }
        if (plugin.phenomenonManager.get(id) == null) return
        if (plugin.playerDataManager.discover(player.uniqueId, id)) {
            plugin.dewLight.syncRecipe(player)
        }

        val inventory = player.inventory
        for (slot in inventory.contents.indices) {
            val pouch = inventory.getItem(slot) ?: continue
            if (!plugin.moonPouch.isPouch(pouch)) continue
            val captured = plugin.moonPouch.capture(pouch, id, dropped.amount)
            if (captured <= 0) continue

            inventory.setItem(slot, pouch)
            val remaining = dropped.amount - captured
            if (remaining <= 0) {
                event.isCancelled = true
                event.item.remove()
            } else {
                dropped.amount = remaining
            }
            return
        }
    }
}
