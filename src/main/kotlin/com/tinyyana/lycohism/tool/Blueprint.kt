package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.facility.Cost
import com.tinyyana.lycohism.util.Audit
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.VanillaItems
import com.tinyyana.lycohism.util.modifyMeta
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * 藍圖 — the player-facing half of the build system (PROJECT_PLAN §「v0.7 規格」§1). Each blueprint
 * targets one multiblock template (stored on the item). Right-click shows a ghost outline so players
 * who like placing by hand can; sneak-right-click builds it instantly, consuming the materials from
 * their inventory. Admins hand these out (or gate them as a perk) via `/lycohism blueprint <id>`.
 */
class Blueprint(private val plugin: Lycohism) {

    enum class Result { BUILT, MISSING_MATERIALS, INVALID }

    fun createItem(structureId: String): ItemStack {
        val structureName = Texts.line("content-names.$structureId", structureId)
        val materials = plugin.multiblockRegistry.get(structureId)?.materialCounts().orEmpty()
        return ItemStack(Material.PAPER).apply {
            modifyMeta { meta ->
                Messages.applyDisplayName(meta, Texts.render("items.blueprint.name", "structure" to structureName))
                val lore = buildList {
                    addAll(Texts.renderLines("items.blueprint.lore", "structure" to structureName))
                    val desc = Texts.lines("content-descriptions.$structureId")
                    if (desc.isNotEmpty()) {
                        add("")
                        addAll(desc)
                    }
                    add("")
                    add(Texts.line("items.blueprint.materials"))
                    materials.entries.sortedBy { it.key.name }.forEach { (material, count) ->
                        add(Texts.render("items.blueprint.material-line", "material" to VanillaItems.tag(material), "count" to count.toString()))
                    }
                }
                Messages.applyLore(meta, lore)
                meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
                meta.persistentDataContainer.set(Keys.blueprintTarget, PersistentDataType.STRING, structureId)
                meta.setEnchantmentGlintOverride(true)
            }
        }
    }

    private fun targetOf(item: ItemStack): String? =
        item.itemMeta?.persistentDataContainer?.get(Keys.blueprintTarget, PersistentDataType.STRING)

    fun materialSummary(item: ItemStack): String = targetOf(item)
        ?.let(plugin.multiblockRegistry::get)
        ?.materialCounts()
        ?.entries
        ?.sortedBy { it.key.name }
        ?.joinToString(Texts.line("terms.list-separator")) { "${VanillaItems.tag(it.key)}×${it.value}" }
        .orEmpty()

    /** Right-click: preview the structure where the player is looking. */
    fun preview(player: Player, item: ItemStack) {
        val multiblock = targetOf(item)?.let(plugin.multiblockRegistry::get) ?: return
        val target = player.getTargetBlockExact(6) ?: player.location.block.getRelative(BlockFace.DOWN)
        multiblock.showGhost(plugin, target.world, target.x, target.y, target.z, com.tinyyana.lycohism.multiblock.Rotation.NONE, player)
    }

    /** Sneak-right-click: build it, consuming the required materials from the inventory. */
    fun build(player: Player, item: ItemStack): Result {
        val structureId = targetOf(item) ?: return Result.INVALID
        val multiblock = plugin.multiblockRegistry.get(structureId) ?: return Result.INVALID
        val target = player.getTargetBlockExact(6) ?: player.location.block.getRelative(BlockFace.DOWN)
        val requirements = Cost.parse(multiblock.materialCounts().map { "${it.key.name}:${it.value}" }, plugin)
        if (player.gameMode != org.bukkit.GameMode.CREATIVE && !Cost.hasAll(player, requirements)) {
            return Result.MISSING_MATERIALS
        }
        if (player.gameMode != org.bukkit.GameMode.CREATIVE) Cost.consume(player, requirements)
        multiblock.place(target.world, target.x, target.y, target.z)
        plugin.structureLocator.record(structureId, target.location)
        // Building from a blueprint also activates the structure (register + floating label).
        com.tinyyana.lycohism.multiblock.StructureActivation.activate(plugin, player, structureId, target.world.getBlockAt(target.x, target.y, target.z))
        Audit.log(player, "blueprint-build", "$structureId at ${target.x},${target.y},${target.z}")
        return Result.BUILT
    }

    companion object {
        const val ID = "blueprint"
    }
}
