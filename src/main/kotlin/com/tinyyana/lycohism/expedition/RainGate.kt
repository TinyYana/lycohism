package com.tinyyana.lycohism.expedition

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.modifyMeta
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * 雨痕門符 — v0.5 expedition gate. Right-clicked in the main world it opens the way to the
 * 雨後森林; right-clicked inside an expedition world it returns the player home. Reusable, so
 * it stays a deliberate travel item rather than a consumable. Balance lives in tools.yml.
 */
class RainGate(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    var cost: List<String> = listOf("rain_breath:8", "moon_dew:2", "ENDER_PEARL:1", "PAPER:1")
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

    fun createItem(): ItemStack = ItemStack(Material.PAPER).apply {
        modifyMeta { meta ->
            Messages.applyDisplayName(meta, displayName)
            Messages.applyLore(meta, loreLines)
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
    }

    companion object {
        const val ID = "rain_gate"
        private const val FILE_NAME = "tools.yml"
    }
}
