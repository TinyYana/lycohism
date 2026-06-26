package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.energy.EnergyType
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.modifyMeta
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/** 月華灑 — 溫室 Lv2 unlock. Right-click spends 月輝 to bone-meal a wide area (the new ability). */
class LunarSpore(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var cost = 25
    private var radius = 2
    var craftCost: List<String> = listOf("moss_bloom:2", "energy_crystal:1", "GLOW_BERRIES:8")
        private set

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("tools")?.getConfigurationSection(ID)
        displayName = Texts.line("items.$ID.name")
        loreLines = Texts.lines("items.$ID.lore")
        if (node != null) {
            cost = node.getInt("moon-cost", cost).coerceAtLeast(0)
            radius = node.getInt("radius", radius).coerceIn(1, 4)
            craftCost = node.getStringList("cost").ifEmpty { craftCost }
        }
    }

    fun createItem(amount: Int = 1): ItemStack = ItemStack(Material.PINK_PETALS, amount).apply {
        modifyMeta { meta ->
            Messages.applyDisplayName(meta, displayName)
            Messages.applyLore(meta, loreLines)
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
    }

    /** Returns true when energy was spent (something grew); false when nothing grew or no energy. */
    fun use(player: Player, clicked: Block): Boolean {
        if (plugin.energyManager.get(player, EnergyType.MOON) < cost) return false
        var grew = 0
        for (dx in -radius..radius) for (dz in -radius..radius) {
            val block = clicked.world.getBlockAt(clicked.x + dx, clicked.y, clicked.z + dz)
            if (block.applyBoneMeal(BlockFace.UP)) grew++
        }
        if (grew == 0) return false
        plugin.energyManager.spend(player, EnergyType.MOON, cost)
        com.tinyyana.lycohism.util.Audit.log(player, "energy-spend", "moon $cost (lunar_spore)")
        clicked.world.spawnParticle(Particle.WITCH, clicked.location.add(0.5, 1.0, 0.5), 24, radius.toDouble(), 0.4, radius.toDouble(), 0.0)
        player.playSound(clicked.location, Sound.ITEM_BONE_MEAL_USE, 0.8f, 1.2f)
        return true
    }

    companion object {
        const val ID = "lunar_spore"
        private const val FILE_NAME = "tools.yml"
    }
}
