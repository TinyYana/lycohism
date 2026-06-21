package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.energy.EnergyType
import com.tinyyana.lycohism.energy.NexusManager
import com.tinyyana.lycohism.gui.NexusHolder
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot

/**
 * Activates v0.7 energy-network structures by right-clicking their controller block: claims a 輝能核心,
 * registers a 能量中繼器, or registers a player-built 日輝塔/月輝塔. Validation reuses the [Multiblock]
 * shape+count check, so only correctly-built structures activate. Also keeps the nexus view read-only.
 */
class MultiblockListener(private val plugin: Lycohism) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val location = event.block.location.toCenterLocation()
        plugin.server.scheduler.runTask(plugin, Runnable {
            com.tinyyana.lycohism.multiblock.StructureActivation.removeBrokenLabels(plugin, location)
        })
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return
        val block = event.clickedBlock ?: return
        val player = event.player
        for (multiblock in plugin.multiblockRegistry.all()) {
            if (multiblock.id == "energy_altar") continue // the altar has its own right-click handler
            if (block.type != multiblock.controller) continue
            if (multiblock.detectRotation(block.world, block.x, block.y, block.z) == null) continue
            event.isCancelled = true
            handle(multiblock.id, player, block)
            return
        }
    }

    private fun handle(id: String, player: Player, block: Block) {
        when (id) {
            "energy_nexus" -> {
                val existing = plugin.nexusManager.existingNexus(block)
                if (existing != null) {
                    if (existing.isAllowed(player.uniqueId)) {
                        plugin.nexusManager.open(player, existing)
                    } else {
                        Messages.send(player, Texts.line("messages.nexus.not-allowed"))
                    }
                    return
                }
                plugin.nexusManager.claimNexus(player, block)
                plugin.playerDataManager.discover(player.uniqueId, "energy_nexus")
                com.tinyyana.lycohism.multiblock.StructureActivation.label(block, "energy_nexus")
                com.tinyyana.lycohism.util.Audit.log(player, "nexus-claim", "at ${block.x},${block.y},${block.z}")
                Messages.send(player, Texts.line("messages.nexus.claimed"))
                player.playSound(player.location, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.4f)
            }

            "energy_relay" -> {
                val registered = plugin.nexusManager.registerRelay(block)
                Messages.send(player, Texts.line(if (registered) "messages.nexus.relay-registered" else "messages.nexus.relay-exists"))
                if (registered) {
                    plugin.playerDataManager.discover(player.uniqueId, "energy_relay")
                    com.tinyyana.lycohism.multiblock.StructureActivation.label(block, "energy_relay")
                    player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f)
                }
            }

            "sun_tower", "moon_tower" -> {
                val type = if (id == "sun_tower") EnergyType.SUN else EnergyType.MOON
                val tx = block.x
                val ty = block.y + TOWER_CORE_HEIGHT
                val tz = block.z
                if (plugin.energyTowers.exists(block.world.name, tx, ty, tz)) {
                    Messages.send(player, Texts.line("messages.nexus.tower-exists"))
                    return
                }
                plugin.energyTowers.record(block.world.name, tx, ty, tz, type)
                plugin.playerDataManager.discover(player.uniqueId, id)
                com.tinyyana.lycohism.multiblock.StructureActivation.label(block, id)
                Messages.send(player, Texts.line("messages.nexus.tower-registered"))
                player.playSound(player.location, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.3f)
            }
        }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.inventory.holder is NexusHolder) event.isCancelled = true
    }

    private companion object {
        /** Crown offset above a tower's base controller, where its aura/production point sits. */
        const val TOWER_CORE_HEIGHT = 15
    }
}
