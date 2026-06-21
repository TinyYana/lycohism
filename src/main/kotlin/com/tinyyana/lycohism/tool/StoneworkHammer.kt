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
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType

/**
 * 石工槌 — makes stone shaping convenient. Right-click a stone-family block to cycle it
 * in place through building variants. Each use costs vanilla durability (the limit).
 */
class StoneworkHammer(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var baseMaterial = Material.IRON_PICKAXE
    private var cycle = listOf(Material.COBBLESTONE, Material.STONE, Material.STONE_BRICKS, Material.CHISELED_STONE_BRICKS)
    var cost: List<String> = listOf("morning_dew:2", "COBBLESTONE:3", "STICK:2")
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
        node.getString("base-material")?.let { Material.matchMaterial(it) }?.let { baseMaterial = it }
        val parsedCycle = node.getStringList("cycle").mapNotNull { Material.matchMaterial(it) }
        if (parsedCycle.size >= 2) cycle = parsedCycle
    }

    fun isHammer(item: ItemStack?): Boolean = Items.idOf(item) == ID

    fun createItem(): ItemStack {
        val item = ItemStack(baseMaterial)
        item.editMeta { meta ->
            meta.displayName(Messages.parse(displayName).decoration(TextDecoration.ITALIC, false))
            meta.lore(loreLines.map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
        return item
    }

    /**
     * Cycles [block] to the next variant if it is in the configured cycle.
     * Returns true if it acted (so the caller can cancel the interaction).
     */
    fun tryCycle(player: Player, block: Block, item: ItemStack): Boolean {
        val current = cycle.indexOf(block.type)
        if (current < 0) return false

        block.type = cycle[(current + 1) % cycle.size]
        block.world.playSound(block.location, Sound.ITEM_AXE_STRIP, 0.7f, 1.2f)
        block.world.spawnParticle(Particle.SCRAPE, block.location.toCenterLocation(), 12, 0.35, 0.35, 0.35, 0.0)
        damage(player, item)
        return true
    }

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
        private const val FILE_NAME = "tools.yml"
    }
}
