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

/**
 * 輝能之鏡 — the first 輝能 sink, so the energy pool is not a dead collectible. Right-click spends
 * 日輝 for a daylight blessing (Fire Resistance + Strength); sneak right-click spends 月輝 for a
 * night blessing (Night Vision + Slow Falling). Crafting needs 月輝核心 from the permanent-night
 * expedition, so the moon side can only come from going there.
 */
class RadiantFocus(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var sunCost = 30
    private var moonCost = 30
    private var durationTicks = 9600 // 8 min — the main blessings (night vision / fire res / strength)
    private var slowFallTicks = 1200 // slow falling stays the short exception (powerful for mobility)
    var cost: List<String> = listOf("energy_crystal:1", "moon_core:1", "GOLD_INGOT:2")
        private set

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("tools")?.getConfigurationSection(ID)
        displayName = Texts.line("items.$ID.name")
        loreLines = Texts.lines("items.$ID.lore")
        if (node != null) {
            sunCost = node.getInt("sun-cost", sunCost).coerceAtLeast(0)
            moonCost = node.getInt("moon-cost", moonCost).coerceAtLeast(0)
            durationTicks = node.getInt("effect-duration-ticks", durationTicks).coerceAtLeast(20)
            slowFallTicks = node.getInt("slow-fall-ticks", slowFallTicks).coerceAtLeast(20)
            cost = node.getStringList("cost").ifEmpty { cost }
        }
    }

    fun createItem(amount: Int = 1): ItemStack = ItemStack(Material.CLOCK, amount).apply {
        editMeta { meta ->
            meta.displayName(Messages.parse(displayName).decoration(TextDecoration.ITALIC, false))
            meta.lore(loreLines.map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
    }

    /** Returns true if the ability fired (energy was available and spent). */
    fun use(player: Player, moon: Boolean): Boolean {
        val type = if (moon) EnergyType.MOON else EnergyType.SUN
        val price = if (moon) moonCost else sunCost
        if (!plugin.energyManager.spend(player, type, price)) return false
        com.tinyyana.lycohism.util.Audit.log(player, "energy-spend", "${type.id} $price (radiant_focus)")
        if (moon) {
            player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, durationTicks, 0, false, true, true))
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, slowFallTicks, 0, false, true, true))
            player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 0.7f)
            player.world.spawnParticle(Particle.WITCH, player.location.add(0.0, 1.0, 0.0), 20, 0.4, 0.7, 0.4, 0.0)
        } else {
            player.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, durationTicks, 0, false, true, true))
            player.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, durationTicks, 0, false, true, true))
            player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 1.3f)
            player.world.spawnParticle(Particle.END_ROD, player.location.add(0.0, 1.0, 0.0), 20, 0.4, 0.7, 0.4, 0.0)
        }
        return true
    }

    companion object {
        const val ID = "radiant_focus"
        private const val FILE_NAME = "tools.yml"
    }
}
