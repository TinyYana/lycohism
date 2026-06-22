package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/** Extends the clicked block outward in one straight, inventory-backed line. */
class BuildingWand(private val plugin: Lycohism) {

    private var length = 5
    private var tier2Length = 8
    private val pending = mutableMapOf<UUID, Pair<String, Long>>()

    enum class Mode { LINE, WALL, FLOOR, COLUMN }
    data class Result(val preview: Boolean, val amount: Int, val mode: Mode)

    init { load() }

    fun load() {
        length = ConfigFiles.load(plugin, "tools.yml").getInt("tools.$ID.length", 5).coerceIn(1, 16)
        tier2Length = ConfigFiles.load(plugin, "tools.yml").getInt("tools.$ID.tier-2-length", 8).coerceIn(length, 32)
    }

    fun createItem(tier2: Boolean = false): ItemStack = ItemStack(if (tier2) Material.BREEZE_ROD else Material.BLAZE_ROD).apply {
        editMeta { meta ->
            val id = if (tier2) TIER_2_ID else ID
            meta.displayName(Messages.parse(Texts.line("items.$id.name")).decoration(TextDecoration.ITALIC, false))
            meta.lore(Texts.lines("items.$id.lore").map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, id)
            if (tier2) meta.persistentDataContainer.set(Keys.wandMode, PersistentDataType.STRING, Mode.LINE.name)
            meta.setEnchantmentGlintOverride(true)
        }
    }

    fun cycleMode(item: ItemStack): Mode {
        if (Items.idOf(item) != TIER_2_ID) return Mode.LINE
        val current = mode(item)
        val next = Mode.entries[(current.ordinal + 1) % Mode.entries.size]
        item.editMeta { it.persistentDataContainer.set(Keys.wandMode, PersistentDataType.STRING, next.name) }
        return next
    }

    fun use(player: Player, source: Block, face: BlockFace, item: ItemStack): Result {
        val material = source.type
        val mode = mode(item)
        if (!material.isBlock || !material.isItem || !material.isSolid) return Result(false, 0, mode)
        val openTargets = targets(source, face, mode, Items.idOf(item) == TIER_2_ID).filter(Block::isEmpty)
        val targets = if (player.gameMode == GameMode.CREATIVE) openTargets else openTargets.take(available(player, material))
        if (targets.isEmpty()) return Result(false, 0, mode)
        val key = "${source.world.uid};${source.x};${source.y};${source.z};${face.name};${mode.name};${material.name}"
        val now = System.currentTimeMillis()
        if (pending[player.uniqueId]?.let { it.first == key && now <= it.second } != true) {
            pending[player.uniqueId] = key to now + CONFIRM_MILLIS
            preview(player, targets, material)
            return Result(true, targets.size, mode)
        }
        pending.remove(player.uniqueId)
        var placed = 0
        for (target in targets) {
            if (player.gameMode != GameMode.CREATIVE && !consumeOne(player, material)) break
            target.type = material
            placed++
        }
        if (placed > 0) source.world.playSound(source.location, Sound.BLOCK_SCAFFOLDING_PLACE, 0.7f, 1.2f)
        return Result(false, placed, mode)
    }

    private fun mode(item: ItemStack): Mode = if (Items.idOf(item) != TIER_2_ID) Mode.LINE else runCatching {
        Mode.valueOf(item.itemMeta?.persistentDataContainer?.get(Keys.wandMode, PersistentDataType.STRING) ?: Mode.LINE.name)
    }.getOrDefault(Mode.LINE)

    private fun targets(source: Block, face: BlockFace, mode: Mode, tier2: Boolean): List<Block> {
        val base = source.getRelative(face)
        return wandOffsets(mode.name, face.name, if (tier2) tier2Length else length)
            .map { (x, y, z) -> base.getRelative(x, y, z) }.distinct()
    }

    private fun preview(player: Player, targets: List<Block>, material: Material) {
        targets.forEach { player.sendBlockChange(it.location, material.createBlockData()) }
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (player.isOnline) targets.forEach { player.sendBlockChange(it.location, it.blockData) }
        }, PREVIEW_TICKS)
    }

    private fun consumeOne(player: Player, material: Material): Boolean {
        val contents = player.inventory.contents
        val index = contents.indexOfFirst { it?.type == material && Items.idOf(it) == null }
        if (index < 0) return false
        val stack = contents[index]!!
        if (stack.amount <= 1) player.inventory.setItem(index, null) else stack.amount--
        return true
    }

    private fun available(player: Player, material: Material): Int = player.inventory.contents
        .filter { it?.type == material && Items.idOf(it) == null }
        .sumOf { it!!.amount }

    companion object {
        const val ID = "building_wand"
        const val TIER_2_ID = "building_wand_tier_2"
        private const val CONFIRM_MILLIS = 5_000L
        private const val PREVIEW_TICKS = 60L
    }
}

internal fun wandOffsets(mode: String, face: String, length: Int): List<Triple<Int, Int, Int>> = when (mode) {
    "WALL" -> hammerPlaneOffsets(face)
    "FLOOR" -> buildList { for (x in -2..2) for (z in -2..2) add(Triple(x, 0, z)) }
    "COLUMN" -> (0 until length).map { Triple(0, it, 0) }
    else -> {
        val direction = when (face) {
            "DOWN" -> Triple(0, -1, 0)
            "NORTH" -> Triple(0, 0, -1)
            "SOUTH" -> Triple(0, 0, 1)
            "WEST" -> Triple(-1, 0, 0)
            "EAST" -> Triple(1, 0, 0)
            else -> Triple(0, 1, 0)
        }
        (0 until length).map { Triple(direction.first * it, direction.second * it, direction.third * it) }
    }
}
