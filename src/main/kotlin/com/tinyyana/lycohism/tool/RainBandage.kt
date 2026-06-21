package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/** A small, consumable v0.2 recovery tool crafted from 雨息 in the 工房. */
class RainBandage(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var healAmount = 4.0
    var cost: List<String> = listOf("rain_breath:1", "PAPER:2", "STRING:1")
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
        healAmount = node.getDouble("heal-amount", healAmount).coerceAtLeast(0.5)
        cost = node.getStringList("cost").ifEmpty { cost }
    }

    fun createItem(amount: Int = 1): ItemStack {
        val item = ItemStack(Material.PAPER, amount)
        item.editMeta { meta ->
            meta.displayName(Messages.parse(displayName).decoration(TextDecoration.ITALIC, false))
            meta.lore(loreLines.map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
        return item
    }

    /** Heals and consumes one bandage. Returns false when the player is already healthy. */
    fun use(player: Player, item: ItemStack): Boolean {
        val maxHealth = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        if (player.health >= maxHealth) return false

        player.health = minOf(maxHealth, player.health + healAmount)
        item.amount -= 1
        player.inventory.setItemInMainHand(item.takeUnless { it.amount <= 0 })
        player.playSound(player.location, Sound.ENTITY_PLAYER_SPLASH, 0.5f, 1.4f)
        player.world.spawnParticle(Particle.SPLASH, player.location.add(0.0, 1.0, 0.0), 18, 0.4, 0.6, 0.4, 0.1)
        return true
    }

    companion object {
        const val ID = "rain_bandage"
        private const val FILE_NAME = "tools.yml"
    }
}
