package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.ConfigFiles
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * Opens base facilities by interacting with their block in the world, so players never
 * have to type a command to play. Trigger is sneak + empty main hand + right-click on the
 * facility block; the empty-hand guard means normal building/placing is never hijacked.
 * Blocks are configurable in facilities.yml under `access`.
 */
class FacilityAccessListener(private val plugin: Lycohism) : Listener {

    private var requireSneak = true
    private var workshopBlock: Material = Material.CRAFTING_TABLE
    private var studyBlock: Material = Material.BOOKSHELF
    private var greenhouseBlock: Material = Material.FLOWER_POT

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("access") ?: return
        requireSneak = node.getBoolean("require-sneak", true)
        workshopBlock = material(node.getString("workshop"), workshopBlock)
        studyBlock = material(node.getString("study"), studyBlock)
        greenhouseBlock = material(node.getString("greenhouse"), greenhouseBlock)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return
        if (event.item != null) return // empty hand only — never steal a place/use action

        val player = event.player
        if (requireSneak && !player.isSneaking) return
        val block = event.clickedBlock ?: return

        when (block.type) {
            workshopBlock -> open(event) { plugin.workshop.open(player, upgradedStructure(block, "workshop")) }
            studyBlock -> open(event) { plugin.study.open(player, upgradedStructure(block, "study")) }
            greenhouseBlock -> open(event) { plugin.greenhouse.open(player, upgradedStructure(block, "greenhouse")) }
            else -> return
        }
    }

    /** True when [block] is the controller of a complete 升級 structure — gates Lv2 access (v0.7.4 #3). */
    private fun upgradedStructure(block: org.bukkit.block.Block, facility: String): Boolean {
        val multiblock = plugin.multiblockRegistry.get(com.tinyyana.lycohism.facility.FacilityUpgrade.structureId(facility)) ?: return false
        val complete = multiblock.detectRotation(block.world, block.x, block.y, block.z) != null
        if (complete) plugin.structureLocator.record(multiblock.id, block.location)
        return complete
    }

    private inline fun open(event: PlayerInteractEvent, action: () -> Unit) {
        event.isCancelled = true
        action()
    }

    private fun material(name: String?, fallback: Material): Material =
        name?.let { Material.matchMaterial(it) } ?: fallback

    companion object {
        private const val FILE_NAME = "facilities.yml"
    }
}
