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

/**
 * 蓄能晶 — a portable 輝能 battery. v0.6.1: the player charges themselves passively (see
 * EnergyService); when their pool is full the surplus auto-overflows into a crystal carried in
 * the inventory. The crystal is the overflow reserve and a manual top-up (right-click pours it
 * back into the pool). Permanent-night expeditions are still the practical place to fill 月輝.
 */
class EnergyCrystal(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var capacity = 100
    var cost: List<String> = listOf("moon_dew:3", "AMETHYST_SHARD:4", "GLASS:1")
        private set

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("tools")?.getConfigurationSection(ID)
        displayName = Texts.line("items.$ID.name")
        loreLines = Texts.lines("items.$ID.lore")
        if (node != null) {
            capacity = node.getInt("capacity", capacity).coerceAtLeast(1)
            cost = node.getStringList("cost").ifEmpty { cost }
        }
    }

    fun createItem(amount: Int = 1): ItemStack = ItemStack(Material.AMETHYST_SHARD, amount).apply {
        editMeta { meta ->
            meta.displayName(Messages.parse(displayName).decoration(TextDecoration.ITALIC, false))
            meta.lore(loreLines.map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
    }

    fun stored(item: ItemStack, type: EnergyType): Int =
        item.itemMeta?.persistentDataContainer?.get(key(type), PersistentDataType.INTEGER) ?: 0

    fun freeSpace(item: ItemStack, type: EnergyType): Int = (capacity - stored(item, type)).coerceAtLeast(0)

    private fun setStored(item: ItemStack, type: EnergyType, value: Int) {
        item.editMeta { it.persistentDataContainer.set(key(type), PersistentDataType.INTEGER, value.coerceIn(0, capacity)) }
    }

    /** Adds overflow charge to the crystal; returns the amount actually stored. */
    fun addCharge(item: ItemStack, type: EnergyType, amount: Int): Int {
        if (amount <= 0) return 0
        val current = stored(item, type)
        val next = (current + amount).coerceAtMost(capacity)
        val added = next - current
        if (added > 0) setStored(item, type, next)
        return added
    }

    /** Pours the crystal's charge back into the player pool; returns total moved. */
    fun bank(player: Player, item: ItemStack): Int {
        var moved = 0
        EnergyType.entries.forEach { type ->
            val have = stored(item, type)
            if (have <= 0) return@forEach
            val added = plugin.energyManager.add(player, type, have)
            if (added > 0) {
                setStored(item, type, have - added)
                moved += added
            }
        }
        if (moved > 0) {
            player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.3f)
            player.world.spawnParticle(Particle.GLOW, player.location.add(0.0, 1.0, 0.0), 12, 0.3, 0.5, 0.3, 0.0)
        }
        return moved
    }

    fun reading(item: ItemStack): String = Texts.render(
        "messages.tools.crystal-status",
        "sun" to stored(item, EnergyType.SUN).toString(),
        "moon" to stored(item, EnergyType.MOON).toString(),
        "cap" to capacity.toString(),
    )

    private fun key(type: EnergyType) = when (type) {
        EnergyType.SUN -> Keys.crystalSun
        EnergyType.MOON -> Keys.crystalMoon
    }

    companion object {
        const val ID = "energy_crystal"
        private const val FILE_NAME = "tools.yml"
    }
}
