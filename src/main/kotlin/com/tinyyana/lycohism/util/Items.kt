package com.tinyyana.lycohism.util

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/** Helpers for identifying and handing out Lycohism custom items. */
object Items {

    /** Returns the Lycohism content id stored on [item], or null if it isn't ours. */
    fun idOf(item: ItemStack?): String? {
        val meta = item?.itemMeta ?: return null
        return meta.persistentDataContainer.get(Keys.itemId, PersistentDataType.STRING)
    }

    /** Adds [item] to the player's inventory, dropping any overflow at their feet. */
    fun give(player: Player, item: ItemStack) {
        val overflow = player.inventory.addItem(item)
        overflow.values.forEach { player.world.dropItemNaturally(player.location, it) }
    }
}
