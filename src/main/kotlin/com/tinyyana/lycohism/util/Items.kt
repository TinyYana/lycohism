package com.tinyyana.lycohism.util

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

// ponytail: Paper's ItemStack.modifyMeta is not in Spigot; shim it with get/set.
fun ItemStack.modifyMeta(block: (ItemMeta) -> Unit): Boolean {
    val meta = itemMeta ?: return false
    block(meta)
    itemMeta = meta
    return true
}

fun <T : ItemMeta> ItemStack.modifyMeta(type: Class<T>, block: (T) -> Unit): Boolean {
    val meta = itemMeta
    if (!type.isInstance(meta)) return false
    @Suppress("UNCHECKED_CAST")
    block(meta as T)
    itemMeta = meta
    return true
}

// ponytail: Paper's Location.toCenterLocation is not in Spigot; shim it.
fun Location.toCenterLocation(): Location = clone().add(0.5, 0.5, 0.5)

// ponytail: Paper's World.getNearbyPlayers is not in Spigot; use getNearbyEntities filter.
fun World.getNearbyPlayers(location: Location, radius: Double): List<Player> =
    getNearbyEntities(location, radius, radius, radius).filterIsInstance<Player>()

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
