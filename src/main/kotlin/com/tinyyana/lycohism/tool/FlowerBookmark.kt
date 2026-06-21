package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 花籤 — remembers one place, then points the way back. Sneak + right-click saves the
 * current location onto the item; right-click shows a direction arrow and distance.
 */
class FlowerBookmark(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    var cost: List<String> = listOf("morning_dew:2", "PAPER:1")
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
        cost = node.getStringList("cost").ifEmpty { cost }
    }

    fun isBookmark(item: ItemStack?): Boolean = Items.idOf(item) == ID

    fun createItem(): ItemStack {
        val item = ItemStack(Material.PAPER)
        item.editMeta { meta ->
            meta.displayName(Messages.parse(displayName).decoration(TextDecoration.ITALIC, false))
            meta.lore(loreLines.map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
        return item
    }

    /** Saves the player's current location onto the held bookmark. */
    fun remember(player: Player, item: ItemStack) {
        val loc = player.location
        item.editMeta { meta ->
            val pdc = meta.persistentDataContainer
            pdc.set(Keys.bookmarkWorld, PersistentDataType.STRING, loc.world.name)
            pdc.set(Keys.bookmarkX, PersistentDataType.INTEGER, loc.blockX)
            pdc.set(Keys.bookmarkY, PersistentDataType.INTEGER, loc.blockY)
            pdc.set(Keys.bookmarkZ, PersistentDataType.INTEGER, loc.blockZ)
        }
        player.inventory.setItemInMainHand(item)
        player.world.spawnParticle(Particle.CHERRY_LEAVES, player.location.add(0.0, 1.0, 0.0), 12, 0.4, 0.6, 0.4, 0.0)
        player.playSound(player.location, Sound.BLOCK_CHERRY_WOOD_HIT, 0.6f, 1.4f)
        Messages.actionBar(player, Texts.line("messages.tools.bookmark-remembered"))
    }

    /** Shows the direction and distance to the remembered location. */
    fun guide(player: Player, item: ItemStack) {
        val pdc = item.itemMeta.persistentDataContainer
        val worldName = pdc.get(Keys.bookmarkWorld, PersistentDataType.STRING)
        if (worldName == null) {
            Messages.actionBar(player, Texts.line("messages.tools.bookmark-empty"))
            return
        }
        if (worldName != player.world.name) {
            Messages.actionBar(player, Texts.line("messages.tools.bookmark-other-world"))
            return
        }

        val target = Location(
            player.world,
            pdc.get(Keys.bookmarkX, PersistentDataType.INTEGER)!!.toDouble(),
            pdc.get(Keys.bookmarkY, PersistentDataType.INTEGER)!!.toDouble(),
            pdc.get(Keys.bookmarkZ, PersistentDataType.INTEGER)!!.toDouble(),
        )
        val dx = target.x - player.location.x
        val dz = target.z - player.location.z
        val distance = sqrt(dx * dx + dz * dz).roundToInt()
        if (distance <= 1) {
            Messages.actionBar(player, Texts.line("messages.tools.bookmark-arrived"))
            return
        }

        Messages.actionBar(player, Texts.render("messages.tools.bookmark-guide", "arrow" to arrow(player, dx, dz), "distance" to distance.toString()))
        player.playSound(player.location, Sound.ITEM_SPYGLASS_USE, 0.35f, 1.6f)
    }

    /** Picks an 8-way arrow pointing from the player's facing toward the target. */
    private fun arrow(player: Player, dx: Double, dz: Double): String {
        val bearing = Math.toDegrees(atan2(-dx, dz))
        var relative = bearing - player.location.yaw
        relative = ((relative % 360) + 540) % 360 - 180 // normalise to (-180, 180]
        val index = (((relative / 45).roundToInt() % 8) + 8) % 8
        return ARROWS[index]
    }

    companion object {
        const val ID = "flower_bookmark"
        private const val FILE_NAME = "tools.yml"
        // Clockwise from straight ahead.
        private val ARROWS = listOf("↑", "↗", "→", "↘", "↓", "↙", "←", "↖")
    }
}
