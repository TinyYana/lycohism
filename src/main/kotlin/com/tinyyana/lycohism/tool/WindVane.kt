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

/** v0.4 風標 — reads current natural conditions instead of duplicating compass behavior. */
class WindVane(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    var cost: List<String> = listOf("moon_dew:1", "wind_trace:2", "CLOCK:1", "COPPER_INGOT:2")
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
        cost = node.getStringList("forecast-cost").ifEmpty { cost }
    }

    fun createItem(): ItemStack = ItemStack(Material.CLOCK).apply {
        editMeta { meta ->
            meta.displayName(Messages.parse(displayName).decoration(TextDecoration.ITALIC, false))
            meta.lore(loreLines.map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
    }

    fun reading(player: Player): String {
        val world = player.world
        val available = plugin.phenomenonManager.available(player.location, player)
        val weatherKey = when {
            world.isThundering -> "thunder"
            world.hasStorm() -> "rain"
            else -> "clear"
        }
        val periodKey = when (world.time) {
            in 0L..2999L -> "morning"
            in 3000L..11999L -> "day"
            in 12000L..13999L -> "dusk"
            else -> "night"
        }
        val moonPhase = ((world.fullTime / 24000L) % 8L).toInt()
        val phenomena = available.joinToString("、") { it.displayName }
            .ifEmpty { Texts.line("terms.none") }
        return Texts.render(
            "messages.tools.wind-vane-status",
            "period" to Texts.line("terms.period.$periodKey"),
            "weather" to Texts.line("terms.weather.$weatherKey"),
            "moon" to Texts.line("terms.moon-phase.$moonPhase"),
            "phenomena" to phenomena,
        )
    }

    companion object {
        const val ID = "wind_vane"
        private const val FILE_NAME = "tools.yml"
    }
}
