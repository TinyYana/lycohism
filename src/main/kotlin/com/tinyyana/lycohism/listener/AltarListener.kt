package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.multiblock.AltarManager
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import org.bukkit.Material
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
        val held = player.inventory.itemInMainHand
        if (held.type.isAir) {
            Messages.actionBar(player, Texts.line("messages.altar.hold-catalyst"))
            return
        }
        val key = when (plugin.altarManager.craft(player, block, held)) {
            AltarManager.Result.CRAFTED -> "messages.altar.crafted"
            AltarManager.Result.NO_RECIPE -> "messages.altar.no-recipe"
            AltarManager.Result.MISSING_INGREDIENTS -> "messages.altar.missing-ingredients"
            AltarManager.Result.MISSING_ENERGY -> "messages.altar.missing-energy"
            AltarManager.Result.INVALID -> "messages.altar.no-recipe"
        }
        Messages.actionBar(player, Texts.line(key))
    }
}
