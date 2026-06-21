package com.tinyyana.lycohism.util

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

/**
 * Reads vanilla advancement completion so Lycohism content can optionally gate on
 * proof of normal progress (e.g. "minecraft:story/enter_the_nether").
 *
 * Per the v0.4 design note this is only the reliable read path; enforcement stays
 * opt-in per data entry. A malformed or unknown key fails closed (treated as not
 * completed) so a typo never silently hands out gated content.
 */
object Advancements {

    /** True when [player] has fully completed every advancement in [keys]; empty means no gate. */
    fun hasAll(player: Player, keys: Collection<String>): Boolean =
        keys.all { isDone(player, it) }

    /** True when [player] has fully completed the advancement named by [key]. */
    fun isDone(player: Player, key: String): Boolean {
        val namespaced = NamespacedKey.fromString(key.trim()) ?: return false
        val advancement = Bukkit.getAdvancement(namespaced) ?: return false
        return player.getAdvancementProgress(advancement).isDone
    }

    /** Validates that [key] is a well-formed namespaced key (does not check existence). */
    fun isValidKey(key: String): Boolean = NamespacedKey.fromString(key.trim()) != null
}
