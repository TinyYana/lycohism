package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
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
 * 苔露膏 — a 雨後森林 recovery consumable, crafted in the 溫室 from 苔華 (expedition-only).
 * Right-click grants gentle Regeneration over time, so it complements 雨紋繃帶's instant patch
 * rather than duplicating it. Its only material source is the expedition, which is the point.
 */
class MossBalm(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var durationTicks = 200
    private var amplifier = 1
    var cost: List<String> = listOf("moss_bloom:2", "GLISTERING_MELON_SLICE:1")
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
        durationTicks = node.getInt("effect-duration-ticks", durationTicks).coerceAtLeast(20)
        amplifier = node.getInt("regen-amplifier", amplifier).coerceIn(0, 4)
        cost = node.getStringList("cost").ifEmpty { cost }
    }

    fun createItem(amount: Int = 1): ItemStack = ItemStack(Material.SLIME_BALL, amount).apply {
        editMeta { meta ->
            meta.displayName(Messages.parse(displayName).decoration(TextDecoration.ITALIC, false))
            meta.lore(loreLines.map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
    }

    /** Applies sustained Regeneration and consumes one balm. */
    fun use(player: Player, item: ItemStack) {
        player.addPotionEffect(
            PotionEffect(PotionEffectType.REGENERATION, durationTicks, amplifier, false, true, true),
        )
        if (player.gameMode != org.bukkit.GameMode.CREATIVE) {
            if (item.amount <= 1) player.inventory.setItemInMainHand(null) else item.amount -= 1
        }
        player.playSound(player.location, Sound.BLOCK_HONEY_BLOCK_SLIDE, 0.6f, 1.4f)
        player.world.spawnParticle(Particle.HEART, player.location.add(0.0, 1.0, 0.0), 8, 0.45, 0.5, 0.45, 0.0)
    }

    companion object {
        const val ID = "moss_balm"
        private const val FILE_NAME = "tools.yml"
    }
}
