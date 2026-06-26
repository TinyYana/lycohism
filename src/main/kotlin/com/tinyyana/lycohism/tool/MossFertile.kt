package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.modifyMeta
import com.tinyyana.lycohism.util.toCenterLocation
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * 苔肥 — an agriculture consumable crafted in the 溫室 from 苔華 (expedition-only). Right-click a
 * crop, sapling or other growable block to bone-meal a small square around it at once, using
 * vanilla growth rules. Distinct from 花脈剪 (single crop, mature + harvest + replant): 苔肥 is
 * an area kick-start, and its only material source is the expedition.
 */
class MossFertile(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var radius = 1
    var cost: List<String> = listOf("moss_bloom:1", "BONE_MEAL:3")
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
        radius = node.getInt("radius", radius).coerceIn(0, 3)
        cost = node.getStringList("cost").ifEmpty { cost }
    }

    fun createItem(amount: Int = 1): ItemStack = ItemStack(Material.BONE_MEAL, amount).apply {
        modifyMeta { meta ->
            Messages.applyDisplayName(meta, displayName)
            Messages.applyLore(meta, loreLines)
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
    }

    /**
     * Bone-meals the square of columns around [origin]. Returns true (and consumes one 苔肥) when
     * at least one block actually grew, so it never wastes the item on bare ground.
     */
    fun use(player: Player, origin: Block, item: ItemStack): Boolean {
        var grew = false
        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                val block = origin.world.getBlockAt(origin.x + dx, origin.y, origin.z + dz)
                if (block.applyBoneMeal(BlockFace.UP)) grew = true
            }
        }
        if (!grew) return false

        if (player.gameMode != org.bukkit.GameMode.CREATIVE) {
            if (item.amount <= 1) player.inventory.setItemInMainHand(null) else item.amount -= 1
        }
        player.playSound(origin.location, Sound.ITEM_BONE_MEAL_USE, 0.8f, 1.2f)
        origin.world.spawnParticle(Particle.HAPPY_VILLAGER, origin.location.toCenterLocation(), 22, radius + 0.3, 0.6, radius + 0.3, 0.0)
        return true
    }

    companion object {
        const val ID = "moss_fertile"
        private const val FILE_NAME = "tools.yml"
    }
}
