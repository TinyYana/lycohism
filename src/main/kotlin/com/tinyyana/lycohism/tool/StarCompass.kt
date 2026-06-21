package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.atan2

/** 星圖羅盤 — 書房 Lv2 unlock. Right-click reads the nearest 輝能塔's direction and distance. */
class StarCompass(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    var craftCost: List<String> = listOf("wind_trace:4", "moon_dew:2", "COMPASS:1")
        private set

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("tools")?.getConfigurationSection(ID)
        displayName = Texts.line("items.$ID.name")
        loreLines = Texts.lines("items.$ID.lore")
        if (node != null) craftCost = node.getStringList("cost").ifEmpty { craftCost }
    }

    fun createItem(amount: Int = 1): ItemStack = ItemStack(Material.RECOVERY_COMPASS, amount).apply {
        editMeta { meta ->
            meta.displayName(Messages.parse(displayName).decoration(TextDecoration.ITALIC, false))
            meta.lore(loreLines.map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
    }

    fun reading(player: Player): String {
        val loc = player.location
        val nearest = plugin.energyTowers.all()
            .filter { it.world == player.world.name }
            .minByOrNull { horizontal2(it.x, it.z, loc.x, loc.z) } ?: return Texts.line("messages.tools.compass-none")
        val dx = nearest.x + 0.5 - loc.x
        val dz = nearest.z + 0.5 - loc.z
        val distance = Math.sqrt(dx * dx + dz * dz).toInt()
        return Texts.render(
            "messages.tools.compass-reading",
            "name" to Texts.line("content-names.${if (nearest.type == com.tinyyana.lycohism.energy.EnergyType.SUN) "sun_tower" else "moon_tower"}"),
            "arrow" to arrow(dx, dz, loc.yaw),
            "distance" to distance.toString(),
        )
    }

    /** A relative 8-way arrow pointing from the player's facing toward the target. */
    private fun arrow(dx: Double, dz: Double, yaw: Float): String {
        val target = Math.toDegrees(atan2(-dx, dz))
        var diff = target - yaw
        diff = ((diff % 360) + 360) % 360
        return ARROWS[((diff + 22.5) / 45).toInt() % 8]
    }

    private fun horizontal2(ax: Int, az: Int, x: Double, z: Double): Double {
        val dx = ax + 0.5 - x; val dz = az + 0.5 - z
        return dx * dx + dz * dz
    }

    companion object {
        const val ID = "star_compass"
        private const val FILE_NAME = "tools.yml"
        private val ARROWS = listOf("↑", "↗", "→", "↘", "↓", "↙", "←", "↖")
    }
}
