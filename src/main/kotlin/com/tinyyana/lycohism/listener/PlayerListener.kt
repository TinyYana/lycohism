package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
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
        plugin.playerDataManager.load(event.player.uniqueId)
        plugin.playerDataManager.rememberInventoryMaterials(event.player)
        plugin.dewLight.syncRecipe(event.player)
        plugin.progressionManager.syncAdvancements(event.player)
        plugin.debugLog("Loaded data for ${event.player.name}")
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.energyService.clear(event.player)
        plugin.playerDataManager.saveAndUnload(event.player.uniqueId)
        plugin.debugLog("Saved data for ${event.player.name}")
    }
}
