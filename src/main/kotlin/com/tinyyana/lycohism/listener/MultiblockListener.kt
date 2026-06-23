package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
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
import java.util.UUID

/**
 * Activates v0.7 energy-network structures by right-clicking their controller block: claims a 輝能核心,
 * registers a 能量中繼器, or registers a player-built 日輝塔/月輝塔. Validation reuses the [Multiblock]
 * shape+count check, so only correctly-built structures activate. Also keeps the nexus view read-only.
 */
class MultiblockListener(private val plugin: Lycohism) : Listener {

    private val pendingBreaks = mutableMapOf<UUID, Pair<String, Long>>()

    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        val data = plugin.playerDataManager.get(player.uniqueId)
        if (data.confirmStructureBreaks) {
            val match = plugin.multiblockRegistry.all().firstNotNullOfOrNull { structure ->
                structure.anchorContaining(event.block)?.let { structure.id to it }
            }
            if (match != null) {
                val (id, anchor) = match
                val key = "$id;${anchor.world.uid};${anchor.x};${anchor.y};${anchor.z}"
                if (player.isSneaking) {
                    plugin.playerDataManager.update(player.uniqueId) { it.confirmStructureBreaks = false }
                    pendingBreaks.remove(player.uniqueId)
                    Messages.send(player, Texts.line("messages.structure-break.confirm-disabled"))
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 0.8f)
                } else if (!confirmationMatches(pendingBreaks[player.uniqueId], key, System.currentTimeMillis())) {
                    event.isCancelled = true
                    pendingBreaks[player.uniqueId] = key to System.currentTimeMillis() + CONFIRM_MILLIS
                    Messages.send(player, Texts.render("messages.structure-break.confirm", "structure" to Texts.line("content-names.$id", id)))
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 0.6f)
                    return
                } else {
                    pendingBreaks.remove(player.uniqueId)
                }
            }
        }
        val location = event.block.location.toCenterLocation()
        plugin.server.scheduler.runTask(plugin, Runnable {
            com.tinyyana.lycohism.multiblock.StructureActivation.removeBrokenLabels(plugin, location)
            com.tinyyana.lycohism.multiblock.StructureActivation.pruneBroken(plugin, location)
        })
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return
        val block = event.clickedBlock ?: return
        val player = event.player
        for (multiblock in plugin.multiblockRegistry.all()) {
            if (multiblock.id == "energy_altar") continue // the altar has its own right-click handler (AltarListener)
            if (multiblock.id == "ember_forge") continue // handled by AltarListener
            if (multiblock.id == "eclipse_dial") continue // the 日月儀 has its own handler (EclipseBoss)
            if (multiblock.id in plugin.multiblockRegistry.FACILITY_TIER_2) continue // facility tiers open via FacilityAccessListener
            // v0.7.5 #1: towers only produce when generated naturally / by admin / blueprint — a hand-built
            // tower can't be right-click-activated. (Player-made production is a later milestone.)
            if (multiblock.id == "sun_tower" || multiblock.id == "moon_tower") continue
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
                plugin.structureLocator.record("energy_nexus", block.location)
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
                    plugin.structureLocator.record("energy_relay", block.location)
                    player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f)
                }
            }

            "attunement_engine" -> {
                if (!plugin.automationManager.register(block, player.uniqueId)) {
                    Texts.lines("messages.automation.engine-status").forEach { Messages.send(player, it) }
                    return
                }
                Messages.send(player, Texts.line("messages.automation.engine-registered"))
                plugin.playerDataManager.discover(player.uniqueId, "attunement_engine")
                com.tinyyana.lycohism.multiblock.StructureActivation.label(block, "attunement_engine")
                plugin.structureLocator.record("attunement_engine", block.location)
                com.tinyyana.lycohism.util.Audit.log(player, "automation-register", "at ${block.x},${block.y},${block.z}")
                player.playSound(player.location, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.2f)
            }

            "seedling_cultivator" -> {
                if (!plugin.automationManager.register(block, player.uniqueId)) {
                    Texts.lines("messages.automation.cultivator-status").forEach { Messages.send(player, it) }
                    return
                }
                Messages.send(player, Texts.line("messages.automation.cultivator-registered"))
                plugin.playerDataManager.discover(player.uniqueId, "seedling_cultivator")
                com.tinyyana.lycohism.multiblock.StructureActivation.label(block, "seedling_cultivator")
                plugin.structureLocator.record("seedling_cultivator", block.location)
                com.tinyyana.lycohism.util.Audit.log(player, "automation-register", "seedling_cultivator at ${block.x},${block.y},${block.z}")
                player.playSound(player.location, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.1f)
            }

            "phenomenon_condenser" -> {
                if (!plugin.automationManager.register(block, player.uniqueId)) {
                    Texts.lines("messages.automation.condenser-status").forEach { Messages.send(player, it) }
                    return
                }
                Messages.send(player, Texts.line("messages.automation.condenser-registered"))
                plugin.playerDataManager.discover(player.uniqueId, "phenomenon_condenser")
                com.tinyyana.lycohism.multiblock.StructureActivation.label(block, "phenomenon_condenser")
                plugin.structureLocator.record("phenomenon_condenser", block.location)
                com.tinyyana.lycohism.util.Audit.log(player, "automation-register", "phenomenon_condenser at ${block.x},${block.y},${block.z}")
                player.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.2f)
            }

            "infernal_relay" -> {
                val isNew = "infernal_relay" !in plugin.playerDataManager.get(player.uniqueId).discoveries
                plugin.playerDataManager.discover(player.uniqueId, "infernal_relay")
                com.tinyyana.lycohism.multiblock.StructureActivation.label(block, "infernal_relay")
                plugin.structureLocator.record("infernal_relay", block.location)
                Messages.send(player, Texts.line(if (isNew) "messages.infernal-relay.claimed" else "messages.infernal-relay.exists"))
                player.playSound(player.location, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.7f)
            }
        }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.inventory.holder is NexusHolder) event.isCancelled = true
    }

    private companion object { const val CONFIRM_MILLIS = 10_000L }
}
