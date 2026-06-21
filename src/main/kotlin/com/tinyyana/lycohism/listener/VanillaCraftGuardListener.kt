package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.CraftItemEvent
import java.util.UUID

/** Prevents an accidental vanilla recipe from consuming Lycohism materials. */
class VanillaCraftGuardListener(private val plugin: Lycohism) : Listener {

    private val pending = mutableMapOf<UUID, Pair<String, Long>>()

    @EventHandler
    fun onCraft(event: CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return
        val data = plugin.playerDataManager.get(player.uniqueId)
        if (!data.confirmVanillaCrafts || Items.idOf(event.recipe.result) != null) return
        if (event.inventory.matrix.none { Items.idOf(it) != null }) return
        val recipeKey = (event.recipe as? Keyed)?.key ?: return
        if (recipeKey.namespace != NamespacedKey.MINECRAFT) return

        if (event.click == ClickType.SHIFT_RIGHT) {
            plugin.playerDataManager.update(player.uniqueId) { it.confirmVanillaCrafts = false }
            pending.remove(player.uniqueId)
            Messages.send(player, Texts.line("messages.crafting.confirm-disabled"))
            // Settled "won't ask again" chord — a low, final note.
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 0.8f)
            return
        }

        val now = System.currentTimeMillis()
        val ingredients = event.inventory.matrix.mapNotNull(Items::idOf).sorted().joinToString(",")
        val key = "$recipeKey:$ingredients"
        if (confirmationMatches(pending[player.uniqueId], key, now)) {
            pending.remove(player.uniqueId)
            // Confirmed — let the craft through with a bright affirmative ping.
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f)
            return
        }

        event.isCancelled = true
        pending[player.uniqueId] = key to now + CONFIRM_MILLIS
        Messages.send(player, Texts.line("messages.crafting.confirm-vanilla"))
        // Soft low caution ping so the cancelled craft has a felt response.
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 0.6f)
    }

    private companion object {
        const val CONFIRM_MILLIS = 10_000L
    }
}

internal fun confirmationMatches(pending: Pair<String, Long>?, key: String, now: Long): Boolean =
    pending?.let { it.first == key && now <= it.second } == true
