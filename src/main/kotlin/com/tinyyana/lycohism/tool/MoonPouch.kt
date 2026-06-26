package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.modifyMeta
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/** v0.4 月紗袋 — automatically captures Lycohism phenomena and releases them on use. */
class MoonPouch(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var capacity = 256
    var cost: List<String> = listOf("moon_dew:4", "BUNDLE:1", "STRING:2")
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
        cost = node.getStringList("capture-cost").ifEmpty { cost }
        capacity = node.getInt("capacity", capacity).coerceAtLeast(64)
    }

    fun createItem(): ItemStack = ItemStack(Material.BUNDLE).apply {
        updateMeta(this, linkedMapOf())
    }

    fun isPouch(item: ItemStack?): Boolean = Items.idOf(item) == ID

    /** Stores up to the remaining capacity and returns the amount captured. */
    fun capture(pouch: ItemStack, phenomenonId: String, amount: Int): Int {
        if (plugin.phenomenonManager.get(phenomenonId) == null) return 0
        val contents = readContents(pouch)
        val captured = minOf(amount, capacity - contents.values.sum())
        if (captured <= 0) return 0
        contents[phenomenonId] = (contents[phenomenonId] ?: 0) + captured
        updateMeta(pouch, contents)
        return captured
    }

    /** Releases all captured phenomena. Returns false when the pouch is empty. */
    fun release(player: org.bukkit.entity.Player, pouch: ItemStack): Boolean {
        val contents = readContents(pouch)
        if (contents.isEmpty()) return false
        for ((id, amount) in contents) {
            val phenomenon = plugin.phenomenonManager.get(id) ?: continue
            var remaining = amount
            while (remaining > 0) {
                val stackSize = minOf(remaining, phenomenon.baseMaterial.maxStackSize)
                Items.give(player, plugin.phenomenonManager.createItem(phenomenon, stackSize))
                remaining -= stackSize
            }
        }
        updateMeta(pouch, linkedMapOf())
        player.inventory.setItemInMainHand(pouch)
        player.world.spawnParticle(Particle.ENCHANT, player.location.add(0.0, 1.0, 0.0), 30, 0.6, 0.8, 0.6, 0.1)
        player.playSound(player.location, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.5f, 1.5f)
        return true
    }

    private fun updateMeta(item: ItemStack, contents: LinkedHashMap<String, Int>) {
        item.modifyMeta { meta ->
            Messages.applyDisplayName(meta, displayName)
            val lore = loreLines.toMutableList()
            lore.add("")
            lore.add(Texts.render("messages.tools.moon-pouch-capacity", "stored" to contents.values.sum().toString(), "capacity" to capacity.toString()))
            contents.forEach { (id, amount) ->
                val label = plugin.phenomenonManager.get(id)?.displayName ?: id
                lore.add(Texts.render("messages.tools.moon-pouch-entry", "item" to label, "amount" to amount.toString()))
            }
            Messages.applyLore(meta, lore)
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.persistentDataContainer.set(Keys.pouchContents, PersistentDataType.STRING, encode(contents))
            meta.setEnchantmentGlintOverride(true)
        }
        // ponytail: vanilla bundle UI may flash on left-click; ToolUseListener already cancels right-click
    }

    private fun readContents(item: ItemStack): LinkedHashMap<String, Int> {
        val encoded = item.itemMeta?.persistentDataContainer?.get(Keys.pouchContents, PersistentDataType.STRING)
            ?: return linkedMapOf()
        return encoded.split(';').mapNotNull { token ->
            val parts = token.split('=', limit = 2)
            val amount = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            parts[0] to amount
        }.toMap(LinkedHashMap())
    }

    private fun encode(contents: Map<String, Int>): String =
        contents.entries.joinToString(";") { (id, amount) -> "$id=$amount" }

    companion object {
        const val ID = "moon_pouch"
        private const val FILE_NAME = "tools.yml"
    }
}
