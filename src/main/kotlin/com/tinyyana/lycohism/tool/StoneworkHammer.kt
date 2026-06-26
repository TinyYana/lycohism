package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.modifyMeta
import com.tinyyana.lycohism.util.toCenterLocation
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/**
 * 石工槌 — makes stone shaping convenient. Right-click a stone-family block to cycle it
 * in place through building variants. Each use costs vanilla durability (the limit).
 */
class StoneworkHammer(private val plugin: Lycohism) {

    enum class Result { PREVIEW, CHANGED, INVALID }

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var baseMaterial = Material.IRON_PICKAXE
    private var cycle = listOf(Material.COBBLESTONE, Material.STONE, Material.STONE_BRICKS, Material.CHISELED_STONE_BRICKS)
    var cost: List<String> = listOf("morning_dew:2", "COBBLESTONE:3", "STICK:2")
        private set
    var reinforcedCost: List<String> = listOf("stonework_hammer:1", "radiant_ore:4", "IRON_INGOT:4")
        private set
    private val pending = mutableMapOf<UUID, Pair<String, Long>>()

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME)
            .getConfigurationSection("tools")
            ?.getConfigurationSection(ID) ?: return

        displayName = Texts.line("items.$ID.name")
        loreLines = Texts.lines("items.$ID.lore")
        cost = node.getStringList("cost").ifEmpty { cost }
        reinforcedCost = node.getStringList("reinforced-cost").ifEmpty { reinforcedCost }
        node.getString("base-material")?.let { Material.matchMaterial(it) }?.let { baseMaterial = it }
        val parsedCycle = node.getStringList("cycle").mapNotNull { Material.matchMaterial(it) }
        if (parsedCycle.size >= 2) cycle = parsedCycle
    }

    fun isHammer(item: ItemStack?): Boolean = Items.idOf(item) in setOf(ID, REINFORCED_ID)

    fun createItem(reinforced: Boolean = false): ItemStack {
        val item = ItemStack(baseMaterial)
        item.modifyMeta { meta ->
            val id = if (reinforced) REINFORCED_ID else ID
            Messages.applyDisplayName(meta, Texts.line("items.$id.name", displayName))
            Messages.applyLore(meta, Texts.lines("items.$id.lore").ifEmpty { loreLines })
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, id)
            meta.setEnchantmentGlintOverride(true)
        }
        return item
    }

    /**
     * Cycles [block] to the next variant if it is in the configured cycle.
     * Returns true if it acted (so the caller can cancel the interaction).
     */
    fun tryCycle(player: Player, block: Block, face: BlockFace, item: ItemStack): Result {
        val original = block.type
        val current = cycle.indexOf(original)
        if (current < 0) return Result.INVALID

        val targets = (if (Items.idOf(item) == REINFORCED_ID) area(block, face) else sequenceOf(block))
            .filter { it.type == original }.toList()
        val next = cycle[(current + 1) % cycle.size]
        val key = "${block.world.uid};${block.x};${block.y};${block.z};${face.name};${Items.idOf(item)};${next.name}"
        val now = System.currentTimeMillis()
        if (pending[player.uniqueId]?.let { it.first == key && now <= it.second } != true) {
            pending[player.uniqueId] = key to now + CONFIRM_MILLIS
            targets.forEach { player.sendBlockChange(it.location, next.createBlockData()) }
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                if (player.isOnline) targets.forEach { player.sendBlockChange(it.location, it.blockData) }
            }, PREVIEW_TICKS)
            return Result.PREVIEW
        }
        pending.remove(player.uniqueId)
        targets.forEach { it.type = next }
        block.world.playSound(block.location, Sound.ITEM_AXE_STRIP, 0.7f, 1.2f)
        block.world.spawnParticle(Particle.SCRAPE, block.location.toCenterLocation(), if (Items.idOf(item) == REINFORCED_ID) 30 else 12, 0.8, 0.5, 0.8, 0.0)
        damage(player, item)
        return Result.CHANGED
    }

    private fun area(block: Block, face: BlockFace): Sequence<Block> =
        hammerPlaneOffsets(face.name).asSequence().map { (x, y, z) -> block.getRelative(x, y, z) }

    private fun damage(player: Player, item: ItemStack) {
        val max = item.type.maxDurability.toInt()
        if (max <= 0) return // base material isn't damageable; treat as unbreakable

        val meta = item.itemMeta as? Damageable ?: return
        val newDamage = meta.damage + 1
        if (newDamage >= max) {
            player.inventory.setItemInMainHand(null)
            player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
            return
        }
        meta.damage = newDamage
        item.itemMeta = meta
        player.inventory.setItemInMainHand(item)
    }

    companion object {
        const val ID = "stonework_hammer"
        const val REINFORCED_ID = "stonework_hammer_tier_2"
        private const val FILE_NAME = "tools.yml"
        private const val CONFIRM_MILLIS = 5_000L
        private const val PREVIEW_TICKS = 60L
    }
}

internal fun hammerPlaneOffsets(face: String): List<Triple<Int, Int, Int>> = buildList {
    for (a in -1..1) for (b in -1..1) add(when (face) {
        "UP", "DOWN" -> Triple(a, 0, b)
        "EAST", "WEST" -> Triple(0, a, b)
        else -> Triple(a, b, 0)
    })
}
