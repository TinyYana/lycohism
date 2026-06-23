package com.tinyyana.lycohism.multiblock

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.energy.EnergyType
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Turns a placed energy structure "live": registers it (tower / relay / nexus) and floats a name
 * label above it so players can tell what they built (TinyYana v0.7.1 #5). Shared by the controller
 * right-click, `/lycohism build` and the blueprint item so that *placing* a structure also activates
 * it — previously `/build` only stamped blocks, so towers never produced and the nexus stayed empty.
 */
object StructureActivation {

    /** Registers [id] for [player] at [block] and labels it. Returns true if newly activated. */
    fun activate(plugin: Lycohism, player: org.bukkit.entity.Player, id: String, block: Block): Boolean = when (id) {
        "sun_tower", "moon_tower" -> {
            val type = if (id == "sun_tower") EnergyType.SUN else EnergyType.MOON
            val cy = block.y + TOWER_CORE_HEIGHT
            if (plugin.energyTowers.exists(block.world.name, block.x, cy, block.z)) {
                false
            } else {
                plugin.energyTowers.record(block.world.name, block.x, cy, block.z, type)
                plugin.playerDataManager.discover(player.uniqueId, id)
                label(block, id)
                plugin.structureLocator.record(id, block.location)
                true
            }
        }

        "energy_nexus" -> {
            if (plugin.nexusManager.existingNexus(block) != null) {
                false
            } else {
                plugin.nexusManager.claimNexus(player, block)
                plugin.playerDataManager.discover(player.uniqueId, "energy_nexus")
                label(block, id)
                plugin.structureLocator.record(id, block.location)
                true
            }
        }

        "energy_relay" -> {
            if (!plugin.nexusManager.registerRelay(block)) {
                false
            } else {
                plugin.playerDataManager.discover(player.uniqueId, "energy_relay")
                label(block, id)
                plugin.structureLocator.record(id, block.location)
                true
            }
        }

        "energy_altar", "ember_forge" -> {
            plugin.playerDataManager.discover(player.uniqueId, id)
            label(block, id)
            plugin.structureLocator.record(id, block.location)
            true
        }

        "infernal_relay" -> {
            plugin.playerDataManager.discover(player.uniqueId, "infernal_relay")
            label(block, id)
            plugin.structureLocator.record(id, block.location)
            true
        }

        "attunement_engine" -> {
            if (!plugin.automationManager.register(block, player.uniqueId)) {
                false
            } else {
                plugin.playerDataManager.discover(player.uniqueId, "attunement_engine")
                label(block, id)
                plugin.structureLocator.record(id, block.location)
                true
            }
        }

        "seedling_cultivator" -> {
            if (!plugin.automationManager.register(block, player.uniqueId)) {
                false
            } else {
                plugin.playerDataManager.discover(player.uniqueId, "seedling_cultivator")
                label(block, "seedling_cultivator")
                plugin.structureLocator.record("seedling_cultivator", block.location)
                true
            }
        }

        "phenomenon_condenser" -> {
            if (!plugin.automationManager.register(block, player.uniqueId)) {
                false
            } else {
                plugin.playerDataManager.discover(player.uniqueId, "phenomenon_condenser")
                label(block, "phenomenon_condenser")
                plugin.structureLocator.record("phenomenon_condenser", block.location)
                true
            }
        }

        else -> false
    }

    /** Floats a billboarded name label above the structure's controller. */
    fun label(block: Block, id: String) =
        spawn(block, id)

    private fun spawn(block: Block, id: String) {
        val world = block.world
        val name = labelText(id)
        world.spawn(Location(world, block.x + 0.5, block.y + labelHeight(id), block.z + 0.5), TextDisplay::class.java) { display ->
            display.text(name)
            display.billboard = Display.Billboard.CENTER
            display.isSeeThrough = true
            display.brightness = Display.Brightness(15, 15)
            display.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, LABEL_ID)
            display.persistentDataContainer.set(
                Keys.structureLabelAnchor,
                PersistentDataType.STRING,
                "$id;${block.x};${block.y};${block.z}",
            )
        }
    }

    /**
     * Drops any tower / nexus / relay registration whose structure no longer validates after a block
     * near [around] broke, so a dismantled structure stops producing & transmitting energy (and its
     * particle beams/aura die with it). Works off the live registries, not labels, so it also covers
     * naturally-generated towers that never had a label. Cheap: a box pre-filter, then [Multiblock]
     * re-validation only for the handful of structures next to the break.
     */
    fun pruneBroken(plugin: Lycohism, around: Location) {
        val world = around.world ?: return
        val bx = around.blockX; val by = around.blockY; val bz = around.blockZ

        // Towers are registered at the crown (base + TOWER_CORE_HEIGHT); re-check at the base controller.
        plugin.energyTowers.all()
            .filter { it.world == world.name && near(bx, by, bz, it.x, it.y, it.z) }
            .forEach { tower ->
                val id = if (tower.type == EnergyType.SUN) "sun_tower" else "moon_tower"
                if (plugin.multiblockRegistry.get(id)?.detectRotation(world, tower.x, tower.y - TOWER_CORE_HEIGHT, tower.z) == null) {
                    plugin.energyTowers.remove(tower.world, tower.x, tower.y, tower.z)
                }
            }

        plugin.nexusManager.allNexuses()
            .filter { it.world == world.name && near(bx, by, bz, it.x, it.y, it.z) }
            .forEach { nexus ->
                if (plugin.multiblockRegistry.get("energy_nexus")?.detectRotation(world, nexus.x, nexus.y, nexus.z) == null) {
                    plugin.nexusManager.removeNexus(nexus)
                }
            }

        plugin.nexusManager.allRelays()
            .filter { it.world == world.name && near(bx, by, bz, it.x, it.y, it.z) }
            .forEach { node ->
                if (plugin.multiblockRegistry.get("energy_relay")?.detectRotation(world, node.x, node.y, node.z) == null) {
                    plugin.nexusManager.removeRelay(node)
                }
            }

        plugin.automationManager.all()
            .filter { it.world == world.name && near(bx, by, bz, it.x, it.y, it.z) }
            .forEach { engine ->
                val structureId = when (world.getBlockAt(engine.x, engine.y, engine.z).type) {
                    Material.BLAST_FURNACE -> "attunement_engine"
                    Material.COMPOSTER -> "seedling_cultivator"
                    Material.LECTERN -> "phenomenon_condenser"
                    else -> null
                }
                if (structureId == null || plugin.multiblockRegistry.get(structureId)?.detectRotation(world, engine.x, engine.y, engine.z) == null) {
                    plugin.automationManager.removeAt(engine.world, engine.x, engine.y, engine.z)
                }
            }
    }

    /** Generous box around a structure's anchor: ±2 horizontally (tower footprint), ±18 vertically. */
    private fun near(bx: Int, by: Int, bz: Int, x: Int, y: Int, z: Int): Boolean =
        kotlin.math.abs(bx - x) <= 2 && kotlin.math.abs(bz - z) <= 2 && kotlin.math.abs(by - y) <= 18

    /** Removes labels whose tagged multiblock no longer matches after a nearby block breaks. */
    fun removeBrokenLabels(plugin: Lycohism, around: Location) {
        val world = around.world ?: return
        world.getNearbyEntities(around, 3.0, 24.0, 3.0).filterIsInstance<TextDisplay>().forEach { display ->
            if (display.persistentDataContainer.get(Keys.itemId, PersistentDataType.STRING) != LABEL_ID) return@forEach
            val anchor = labelAnchor(plugin, display) ?: return@forEach
            val structure = plugin.multiblockRegistry.get(anchor.id) ?: return@forEach
            if (structure.detectRotation(world, anchor.x, anchor.y, anchor.z) == null) {
                display.remove()
            } else if (!display.persistentDataContainer.has(Keys.structureLabelAnchor)) {
                // Migrate labels created before v0.7.2 when a nearby block change gives us a safe check.
                display.persistentDataContainer.set(
                    Keys.structureLabelAnchor,
                    PersistentDataType.STRING,
                    "${anchor.id};${anchor.x};${anchor.y};${anchor.z}",
                )
            }
        }
    }

    private fun labelAnchor(plugin: Lycohism, display: TextDisplay): LabelAnchor? {
        display.persistentDataContainer.get(Keys.structureLabelAnchor, PersistentDataType.STRING)?.let { encoded ->
            val parts = encoded.split(';')
            if (parts.size == 4) {
                val id = parts[0]
                val x = parts[1].toIntOrNull()
                val y = parts[2].toIntOrNull()
                val z = parts[3].toIntOrNull()
                if (x != null && y != null && z != null) return LabelAnchor(id, x, y, z)
            }
        }
        val id = plugin.multiblockRegistry.ids().firstOrNull { display.text() == labelText(it) } ?: return null
        val location = display.location
        return LabelAnchor(
            id,
            floor(location.x).toInt(),
            (location.y - labelHeight(id)).roundToInt(),
            floor(location.z).toInt(),
        )
    }

    private fun labelText(id: String) =
        Messages.parse(Texts.line("content-names.$id", id)).decoration(TextDecoration.ITALIC, false)

    private fun labelHeight(id: String): Double = when (id) {
        "sun_tower", "moon_tower" -> 19.5
        "energy_relay" -> 5.5
        else -> 2.5 // nexus / altar
    }

    const val LABEL_ID = "structure_label"
    const val TOWER_CORE_HEIGHT = 15

    private data class LabelAnchor(val id: String, val x: Int, val y: Int, val z: Int)
}
