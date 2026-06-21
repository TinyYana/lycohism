package com.tinyyana.lycohism.facility

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.data.PlayerData
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.VanillaItems
import com.tinyyana.lycohism.util.Texts
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Parses and resolves material costs written as `id:amount` tokens.
 *
 * `id` is matched as a vanilla [Material] when possible; otherwise it is treated as
 * a Lycohism item id (matched via PDC), so costs can mix vanilla and custom items.
 */
object Cost {

    /** One required ingredient: how to recognise it, how many, and a display label. */
    data class Requirement(
        val label: String,
        val amount: Int,
        val contentId: String?,
        val material: Material?,
        val matches: (ItemStack) -> Boolean,
    )

    fun parse(tokens: List<String>, plugin: Lycohism): List<Requirement> =
        tokens.mapNotNull { parseOne(it, plugin) }

    private fun parseOne(token: String, plugin: Lycohism): Requirement? {
        val parts = token.split(":")
        val id = parts[0].trim()
        val amount = parts.getOrNull(1)?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1

        val material = Material.matchMaterial(id)
        if (material != null) {
            return Requirement(VanillaItems.tag(material), amount, null, material) {
                it.type == material && Items.idOf(it) == null
            }
        }

        // Lycohism custom item id (e.g. morning_dew). Label from its display name if known.
        val label = Texts.line("content-names.$id", plugin.phenomenonManager.get(id)?.displayName ?: id)
        return Requirement(label, amount, id, null) { Items.idOf(it) == id }
    }

    fun isKnown(data: PlayerData, requirement: Requirement): Boolean =
        requirement.contentId?.let(data.discoveries::contains) ?: true

    fun isRecipeUnlocked(data: PlayerData, requirements: List<Requirement>): Boolean =
        requirements.all { isKnown(data, it) }

    /** Total amount of items matching [requirement] currently in the player's inventory. */
    fun count(player: Player, requirement: Requirement): Int =
        player.inventory.contents.asSequence()
            .filterNotNull()
            .filter { requirement.matches(it) }
            .sumOf { it.amount }

    fun hasAll(player: Player, requirements: List<Requirement>): Boolean =
        requirements.all { count(player, it) >= it.amount }

    /** Removes the required amounts from the player's inventory. Call only after [hasAll]. */
    fun consume(player: Player, requirements: List<Requirement>) {
        for (requirement in requirements) {
            var remaining = requirement.amount
            val contents = player.inventory.contents
            for (i in contents.indices) {
                if (remaining <= 0) break
                val stack = contents[i] ?: continue
                if (!requirement.matches(stack)) continue

                val take = minOf(remaining, stack.amount)
                stack.amount -= take
                remaining -= take
                if (stack.amount <= 0) player.inventory.setItem(i, null)
            }
        }
    }
}
