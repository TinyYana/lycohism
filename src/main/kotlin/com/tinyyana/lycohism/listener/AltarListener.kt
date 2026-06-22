package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.multiblock.AltarManager
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.VanillaItems
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot

/** Right-click a built 輝能祭壇 centre while holding a catalyst to craft (see [AltarManager]). */
class AltarListener(private val plugin: Lycohism) : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.ENCHANTING_TABLE) return
        // Only intercept correctly-built altars; a plain enchanting table keeps its vanilla menu.
        if (plugin.multiblockRegistry.get("energy_altar")?.detectRotation(block.world, block.x, block.y, block.z) == null) return

        event.isCancelled = true
        val player = event.player
        // Touching a built altar reveals it (codex + progression "輝能祭壇").
        plugin.playerDataManager.discover(player.uniqueId, "energy_altar")
        plugin.structureLocator.record("energy_altar", block.location)
        val held = player.inventory.itemInMainHand
        if (held.type.isAir) {
            // Empty hand = "how do I use this": show the steps and every known recipe.
            showGuide(player)
            return
        }
        when (plugin.altarManager.craft(player, block, held)) {
            AltarManager.Result.CRAFTED -> Messages.actionBar(player, Texts.line("messages.altar.crafted"))
            AltarManager.Result.MISSING_INGREDIENTS -> Messages.actionBar(player, Texts.line("messages.altar.missing-ingredients"))
            AltarManager.Result.MISSING_ENERGY -> Messages.actionBar(player, Texts.line("messages.altar.missing-energy"))
            // Held item isn't a catalyst: say so AND list what counts as one (v0.7.5 #6).
            AltarManager.Result.NO_RECIPE, AltarManager.Result.INVALID -> {
                Messages.actionBar(player, Texts.line("messages.altar.no-recipe"))
                val catalysts = plugin.altarManager.recipes().map { it.catalyst }.distinct().joinToString("、", transform = ::name)
                Messages.send(player, Texts.render("messages.altar.catalysts", "catalysts" to catalysts))
            }
        }
    }

    /** Prints the how-to plus a one-line summary of every altar recipe (catalyst → result). */
    private fun showGuide(player: Player) {
        Texts.lines("messages.altar.guide").forEach { Messages.send(player, it) }
        for (recipe in plugin.altarManager.recipes()) {
            val ingredients = recipe.ingredients.entries.joinToString("、") { (k, c) -> "${name(k)}×$c" }
            val energy = buildString {
                if (recipe.sunCost > 0) append(" ☀${recipe.sunCost}")
                if (recipe.moonCost > 0) append(" ☾${recipe.moonCost}")
            }
            Messages.send(
                player,
                Texts.render(
                    "messages.altar.guide-recipe",
                    "result" to name(recipe.resultId),
                    "catalyst" to name(recipe.catalyst),
                    "ingredients" to ingredients,
                    "energy" to energy,
                ),
            )
        }
    }

    /** Display name for a recipe key: its Lycohism content name, else the vanilla item name. */
    private fun name(key: String): String {
        val content = Texts.line("content-names.$key", "")
        if (content.isNotEmpty()) return content
        return Material.matchMaterial(key)?.let { VanillaItems.tag(it) } ?: key
    }
}
