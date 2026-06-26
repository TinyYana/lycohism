package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Core player lifecycle hooks. For the skeleton this just loads/saves player data;
 * gameplay listeners (晨露 collection, etc.) live in their own listener classes.
 */
class PlayerListener(private val plugin: Lycohism) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val data = plugin.playerDataManager.load(event.player.uniqueId)
        plugin.playerDataManager.rememberInventoryMaterials(event.player)
        plugin.dewLight.syncRecipe(event.player)
        plugin.progressionManager.syncAdvancements(event.player)
        if (!data.seenWelcome) {
            data.seenWelcome = true
            plugin.playerDataManager.save(event.player.uniqueId)
            // Delay one tick so the player's client is fully ready to receive chat.
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                Texts.lines("messages.welcome").forEach { line ->
                    Messages.send(event.player, line)
                }
            }, 20L)
        }
        plugin.debugLog("Loaded data for ${event.player.name}")
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.energyService.clear(event.player)
        plugin.playerDataManager.saveAndUnload(event.player.uniqueId)
        plugin.debugLog("Saved data for ${event.player.name}")
    }
}
