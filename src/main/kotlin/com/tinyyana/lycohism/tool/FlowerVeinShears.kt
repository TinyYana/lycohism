package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType

/**
 * 花脈剪 — v0.3 plant tool crafted in the 溫室. Right-click a mature crop to harvest and
 * replant it in one motion; right-click an immature crop to mature it immediately.
 */
class FlowerVeinShears(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var baseMaterial = Material.SHEARS
    private var growthDurabilityCost = 2
    var cost: List<String> = listOf("flower_vein:2", "IRON_INGOT:1")
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
        growthDurabilityCost = node.getInt("growth-durability-cost", growthDurabilityCost).coerceAtLeast(1)
        node.getString("base-material")?.let { Material.matchMaterial(it) }?.let { baseMaterial = it }
    }

    fun isShears(item: ItemStack?): Boolean = Items.idOf(item) == ID

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
     * Acts on [block] if it is a crop. Returns true if it did anything (so the caller can
     * cancel the interaction). Mature crops are harvested + replanted; others show growth.
     */
    fun handle(player: Player, block: Block, item: ItemStack): Boolean {
        val crop = block.blockData as? Ageable ?: return false

        if (crop.age < crop.maximumAge) {
            crop.age = crop.maximumAge
            block.blockData = crop
            block.world.playSound(block.location, Sound.ITEM_BONE_MEAL_USE, 0.8f, 1.2f)
            block.world.spawnParticle(Particle.HAPPY_VILLAGER, block.location.toCenterLocation(), 14, 0.35, 0.45, 0.35, 0.0)
            damage(player, item, growthDurabilityCost)
            Messages.actionBar(player, Texts.line("messages.tools.flower-shears-grown"))
            return true
        }

        val drops = block.getDrops(item)
        crop.age = 0
        block.blockData = crop
        val center = block.location.toCenterLocation()
        drops.forEach { block.world.dropItemNaturally(center, it) }
        block.world.playSound(block.location, Sound.BLOCK_CROP_BREAK, 0.7f, 1.2f)
        block.world.spawnParticle(Particle.CHERRY_LEAVES, center, 10, 0.35, 0.45, 0.35, 0.0)
        damage(player, item, 1)
        Messages.actionBar(player, Texts.line("messages.tools.flower-shears-harvested"))
        return true
    }

    private fun damage(player: Player, item: ItemStack, amount: Int) {
        val max = item.type.maxDurability.toInt()
        if (max <= 0) return

        val meta = item.itemMeta as? Damageable ?: return
        val newDamage = meta.damage + amount
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
        const val ID = "flower_vein_shears"
        private const val FILE_NAME = "tools.yml"
    }
}
