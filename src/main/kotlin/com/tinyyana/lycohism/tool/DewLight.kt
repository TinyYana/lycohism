package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.modifyMeta
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * 露光瓶 — the first life tool. Crafted from 晨露; right-click grants brief Night
 * Vision so the player can see in the dark. It is consumed on use and returns its
 * empty bottle, keeping the effect useful without replacing permanent lighting.
 */
class DewLight(private val plugin: Lycohism) {

    private var displayName = ID
    private var loreLines: List<String> = emptyList()
    private var dewCost = 1
    private var lightDurationTicks = 2400
    var cost: List<String> = listOf("morning_dew:1", "GLASS_BOTTLE:1")
        private set

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME)
            .getConfigurationSection("tools")
            ?.getConfigurationSection("dew_light") ?: return

        displayName = Texts.line("items.$ID.name")
        loreLines = Texts.lines("items.$ID.lore")
        dewCost = node.getInt("dew-cost", dewCost).coerceAtLeast(1)
        lightDurationTicks = node.getInt("effect-duration-ticks", lightDurationTicks).coerceAtLeast(20)
        cost = node.getStringList("cost").ifEmpty { listOf("morning_dew:$dewCost", "GLASS_BOTTLE:1") }
    }

    fun isDewLight(item: ItemStack?): Boolean {
        val meta = item?.itemMeta ?: return false
        return meta.persistentDataContainer.get(Keys.itemId, PersistentDataType.STRING) == ID
    }

    fun createItem(amount: Int = 1): ItemStack {
        val item = ItemStack(Material.GLASS_BOTTLE, amount)
        item.modifyMeta { meta ->
            Messages.applyDisplayName(meta, displayName)
            Messages.applyLore(meta, loreLines)
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
        return item
    }

    /** Activates and consumes one bottle. */
    fun use(player: Player, item: ItemStack) {
        player.addPotionEffect(
            PotionEffect(PotionEffectType.NIGHT_VISION, lightDurationTicks, 0, false, false, true),
        )
        if (player.gameMode != org.bukkit.GameMode.CREATIVE) {
            if (item.amount <= 1) player.inventory.setItemInMainHand(null)
            else item.amount -= 1
            Items.give(player, ItemStack(Material.GLASS_BOTTLE))
        }
        player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.4f)
        player.world.spawnParticle(Particle.END_ROD, player.location.add(0.0, 1.0, 0.0), 18, 0.45, 0.7, 0.45, 0.02)
    }

    /** Registers (or refreshes) the crafting recipe: glass bottle + 晨露 → 露光瓶. */
    fun registerRecipe() {
        val key = recipeKey()
        plugin.server.removeRecipe(key)

        val dew = plugin.phenomenonManager.get(DEW_PHENOMENON_ID)
            ?.let { plugin.phenomenonManager.createItem(it) }
        if (dew == null) {
            plugin.logger.warning("Cannot register $ID recipe: phenomenon '$DEW_PHENOMENON_ID' is missing.")
            return
        }

        val recipe = ShapelessRecipe(key, createItem())
        recipe.addIngredient(RecipeChoice.MaterialChoice(Material.GLASS_BOTTLE))
        repeat(dewCost) { recipe.addIngredient(RecipeChoice.ExactChoice(dew)) }
        plugin.server.addRecipe(recipe)
        plugin.server.onlinePlayers.forEach(::syncRecipe)
    }

    /** Keeps the vanilla recipe book hidden until the player has encountered 晨露. */
    fun syncRecipe(player: Player) {
        if (plugin.playerDataManager.get(player.uniqueId).hasDiscovered(DEW_PHENOMENON_ID)) {
            player.discoverRecipe(recipeKey())
        } else {
            player.undiscoverRecipe(recipeKey())
        }
    }

    private fun recipeKey() = NamespacedKey(plugin, ID)

    companion object {
        const val ID = "dew_light"
        private const val DEW_PHENOMENON_ID = "morning_dew"
        private const val FILE_NAME = "tools.yml"
    }
}
