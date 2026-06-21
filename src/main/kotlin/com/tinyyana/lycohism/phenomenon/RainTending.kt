package com.tinyyana.lycohism.phenomenon

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import org.bukkit.Sound
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** Lets raw 雨息 gently advance a vanilla crop instead of adding an automation system. */
class RainTending(private val plugin: Lycohism) {

    private var growthStages = 2

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME)
            .getConfigurationSection("phenomena.$RAIN_BREATH_ID") ?: return
        growthStages = node.getInt("crop-growth-stages", growthStages).coerceAtLeast(1)
    }

    /** Returns true only when a crop grew and one 雨息 was consumed. */
    fun tend(player: Player, block: Block, item: ItemStack): Boolean {
        if (Items.idOf(item) != RAIN_BREATH_ID) return false
        val crop = block.blockData as? Ageable ?: return false
        if (crop.age >= crop.maximumAge) {
            Messages.actionBar(player, Texts.line("messages.tools.rain-tending-mature"))
            return false
        }

        crop.age = minOf(crop.maximumAge, crop.age + growthStages)
        block.blockData = crop
        item.amount -= 1
        player.inventory.setItemInMainHand(item.takeUnless { it.amount <= 0 })
        block.world.playSound(block.location, Sound.ITEM_BONE_MEAL_USE, 0.7f, 1.2f)
        block.world.spawnParticle(Particle.FALLING_WATER, block.location.toCenterLocation(), 16, 0.35, 0.45, 0.35, 0.0)
        Messages.actionBar(player, Texts.line("messages.tools.rain-tending-grown"))
        return true
    }

    companion object {
        const val RAIN_BREATH_ID = "rain_breath"
        const val DISCOVERY_ID = "rain_tending"
        private const val FILE_NAME = "phenomena.yml"
    }
}
