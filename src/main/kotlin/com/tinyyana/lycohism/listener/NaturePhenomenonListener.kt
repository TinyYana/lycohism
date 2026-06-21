package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import kotlin.random.Random

/**
 * Drops natural phenomena (晨露 …) when a player harvests a matching source block
 * under the right time / weather / world conditions. Vanilla drops are untouched.
 */
class NaturePhenomenonListener(private val plugin: Lycohism) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (player.gameMode == GameMode.CREATIVE) return

        val phenomenon = plugin.phenomenonManager.match(event.block, player) ?: return
        if (Random.nextDouble() >= phenomenon.chance) return

        val item = plugin.phenomenonManager.createItem(phenomenon)
        event.block.world.dropItemNaturally(event.block.location.toCenterLocation(), item)

        val firstDiscovery = plugin.playerDataManager.discover(player.uniqueId, phenomenon.id)
        if (firstDiscovery) plugin.dewLight.syncRecipe(player)
        if (firstDiscovery && phenomenon.firstHint.isNotEmpty()) {
            Messages.send(player, phenomenon.firstHint)
            // Hand the player the guidance book the first time they discover anything.
            if (plugin.tuningManual.grantIfAbsent(player)) {
                Messages.send(player, Texts.line("messages.discovery.manual-granted"))
            }
        }
        plugin.debugLog("${player.name} collected phenomenon '${phenomenon.id}'")
    }
}
