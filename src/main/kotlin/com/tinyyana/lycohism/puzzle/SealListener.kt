package com.tinyyana.lycohism.puzzle

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent

/** Intercepts controller right-clicks (unlock attempts) and chest opens (seal blocking). */
class SealListener(private val plugin: Lycohism) : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (!plugin.sealManager.isSealController(block)) return
        event.isCancelled = true
        val key = when (plugin.sealManager.tryUnlock(event.player, block)) {
            SealManager.UnlockResult.UNLOCKED -> "messages.puzzle.seal.unlocked"
            SealManager.UnlockResult.ALREADY_OPEN -> "messages.puzzle.seal.already-open"
            SealManager.UnlockResult.MISSING_DISCOVERY -> "messages.puzzle.seal.hint-discovery"
            SealManager.UnlockResult.MISSING_ITEM -> "messages.puzzle.seal.hint-item"
            SealManager.UnlockResult.NOT_A_SEAL -> return
        }
        Messages.send(event.player, Texts.line(key))
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!plugin.sealManager.isShrineBlockProtectedFor(event.player, event.block)) return
        event.isCancelled = true
        Messages.send(event.player, Texts.line("messages.puzzle.seal.protected-break"))
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        val block = event.inventory.location?.block ?: return
        if (!plugin.sealManager.isSealedChestFor(player, block)) return
        event.isCancelled = true
        Messages.send(player, Texts.line("messages.puzzle.seal.sealed"))
    }
}
