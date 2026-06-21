package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.energy.EnergyType
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/** 日炎鎬 — 工房 Lv2 unlock. Right-click spends 日輝 for a burst of Haste (the new "ability"). */
class SolarPick(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var cost = 25
    private var durationTicks = 1200
    private var amplifier = 1
    var craftCost: List<String> = listOf("energy_crystal:1", "DIAMOND_PICKAXE:1", "GOLD_BLOCK:1")
        private set

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("tools")?.getConfigurationSection(ID)
        displayName = Texts.line("items.$ID.name")
        loreLines = Texts.lines("items.$ID.lore")
        if (node != null) {
            cost = node.getInt("sun-cost", cost).coerceAtLeast(0)
            durationTicks = node.getInt("effect-duration-ticks", durationTicks).coerceAtLeast(20)
            amplifier = node.getInt("haste-amplifier", amplifier).coerceIn(0, 4)
            craftCost = node.getStringList("cost").ifEmpty { craftCost }
        }
    }

    fun createItem(amount: Int = 1): ItemStack = ItemStack(Material.GOLDEN_PICKAXE, amount).apply {
        editMeta { meta ->
            meta.displayName(Messages.parse(displayName).decoration(TextDecoration.ITALIC, false))
            meta.lore(loreLines.map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
    }

    fun use(player: Player): Boolean {
        if (!plugin.energyManager.spend(player, EnergyType.SUN, cost)) return false
        com.tinyyana.lycohism.util.Audit.log(player, "energy-spend", "sun $cost (solar_pick)")
        player.addPotionEffect(PotionEffect(PotionEffectType.HASTE, durationTicks, amplifier, false, true, true))
        player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 1.5f)
        player.world.spawnParticle(Particle.END_ROD, player.location.add(0.0, 1.0, 0.0), 16, 0.4, 0.6, 0.4, 0.0)
        return true
    }

    companion object {
        const val ID = "solar_pick"
        private const val FILE_NAME = "tools.yml"
    }
}
