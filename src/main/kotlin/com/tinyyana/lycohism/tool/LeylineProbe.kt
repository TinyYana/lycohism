package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.VanillaItems
import com.tinyyana.lycohism.util.modifyMeta
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

/** 地脈探針 — a short-range, bounded scan for nearby vanilla ore blocks. */
class LeylineProbe(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var horizontalRadius = 8
    private var verticalRadius = 12
    private var refreshIntervalTicks = 60
    private var targets: Set<Material> = DEFAULT_TARGETS
    private val readingCache = mutableMapOf<UUID, CachedReading>()

    var cost: List<String> = listOf("leyline_sand:3", "COPPER_INGOT:2", "REDSTONE:1")
        private set

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME)
            .getConfigurationSection("tools")
            ?.getConfigurationSection(ID) ?: return
        displayName = Texts.line("items.$ID.name")
        loreLines = Texts.lines("items.$ID.lore")
        cost = node.getStringList("probe-cost").ifEmpty { cost }
        horizontalRadius = node.getInt("horizontal-radius", horizontalRadius).coerceIn(2, 16)
        verticalRadius = node.getInt("vertical-radius", verticalRadius).coerceIn(2, 24)
        refreshIntervalTicks = node.getInt(
            "refresh-interval-ticks",
            node.getInt("cooldown-ticks", refreshIntervalTicks),
        ).coerceAtLeast(20)
        targets = node.getStringList("target-materials")
            .mapNotNull { name ->
                Material.matchMaterial(name).also { material ->
                    if (material == null) plugin.logger.warning("Leyline probe: unknown target material '$name'.")
                }
            }
            .filter { it.isBlock }
            .toSet()
            .ifEmpty { DEFAULT_TARGETS }
        readingCache.clear()
    }

    fun createItem(): ItemStack = ItemStack(Material.COMPASS).apply {
        modifyMeta { meta ->
            Messages.applyDisplayName(meta, displayName)
            Messages.applyLore(meta, loreLines)
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
    }

    /** Returns a cached action-bar reading, rescanning only at the configured interval. */
    fun reading(player: Player): String {
        val now = System.currentTimeMillis()
        readingCache[player.uniqueId]?.takeIf { it.expiresAt > now }?.let { return it.message }
        val message = scan(player)
        readingCache[player.uniqueId] = CachedReading(now + refreshIntervalTicks * 50L, message)
        return message
    }

    private fun scan(player: Player): String {
        val origin = player.location.block.location
        val world = player.world
        var nearestMaterial: Material? = null
        var nearestX = 0
        var nearestY = 0
        var nearestZ = 0
        var nearestDistanceSquared = Int.MAX_VALUE

        for (x in -horizontalRadius..horizontalRadius) {
            for (y in -verticalRadius..verticalRadius) {
                val blockY = origin.blockY + y
                if (blockY !in world.minHeight until world.maxHeight) continue
                for (z in -horizontalRadius..horizontalRadius) {
                    val material = world.getBlockAt(origin.blockX + x, blockY, origin.blockZ + z).type
                    if (material !in targets) continue
                    val distanceSquared = x * x + y * y + z * z
                    if (distanceSquared >= nearestDistanceSquared) continue
                    nearestMaterial = material
                    nearestX = x
                    nearestY = y
                    nearestZ = z
                    nearestDistanceSquared = distanceSquared
                }
            }
        }

        val material = nearestMaterial
        if (material == null) {
            return Texts.line("messages.tools.leyline-probe-none-status")
        }

        val direction = when {
            abs(nearestX) <= 2 && abs(nearestZ) <= 2 -> Texts.line("terms.direction.nearby")
            abs(nearestX) > abs(nearestZ) -> Texts.line(if (nearestX > 0) "terms.direction.east" else "terms.direction.west")
            else -> Texts.line(if (nearestZ > 0) "terms.direction.south" else "terms.direction.north")
        }
        val vertical = Texts.line(
            when {
                nearestY > 2 -> "terms.vertical.above"
                nearestY < -2 -> "terms.vertical.below"
                else -> "terms.vertical.level"
            },
        )
        val distance = ceil(sqrt(nearestDistanceSquared.toDouble())).toInt()
        return Texts.render(
            "messages.tools.leyline-probe-status",
            "item" to VanillaItems.tag(material),
            "direction" to direction,
            "vertical" to vertical,
            "distance" to distance.toString(),
        )
    }

    private data class CachedReading(val expiresAt: Long, val message: String)

    companion object {
        const val ID = "leyline_probe"
        private const val FILE_NAME = "tools.yml"
        private val DEFAULT_TARGETS = setOf(
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
        )
    }
}
