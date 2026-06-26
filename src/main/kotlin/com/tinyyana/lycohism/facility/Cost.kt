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

    /**
     * One required ingredient.
     *
     * [labelFor] resolves the display name in the given player locale (Bukkit `player.locale`,
     * e.g. "zh_TW", "en_US", "ja_JP"). zh_* → ZH lang file; everything else → EN.
     * [label] is a convenience shorthand that uses the plugin's configured language.
     */
    data class Requirement(
        val amount: Int,
        val contentId: String?,   // null for vanilla materials
        val material: Material?,  // null for Lycohism custom items
        val fallbackLabel: String,
        val matches: (ItemStack) -> Boolean,
    ) {
        val label: String get() = labelFor("")

        fun labelFor(playerLocale: String): String {
            val lang = if (playerLocale.isEmpty()) Texts.activeLanguage
                       else Texts.langCodeFor(playerLocale)
            return when {
                material != null -> VanillaItems.tag(material, lang)
                contentId != null -> Texts.lineInLang("content-names.$contentId", lang, fallbackLabel)
                else -> fallbackLabel
            }
        }
    }

    fun parse(tokens: List<String>, plugin: Lycohism): List<Requirement> =
        tokens.mapNotNull { parseOne(it, plugin) }

    private fun parseOne(token: String, plugin: Lycohism): Requirement? {
        val parts = token.split(":")
        val id = parts[0].trim()
        val amount = parts.getOrNull(1)?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1

        val material = Material.matchMaterial(id)
        if (material != null) {
            return Requirement(amount, null, material, "") {
                it.type == material && Items.idOf(it) == null
            }
        }

        // Lycohism custom item id (e.g. morning_dew).
        val fallback = plugin.phenomenonManager.get(id)?.displayName ?: id
        return Requirement(amount, id, null, fallback) { Items.idOf(it) == id }
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
