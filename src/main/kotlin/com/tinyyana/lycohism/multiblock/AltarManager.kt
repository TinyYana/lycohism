package com.tinyyana.lycohism.multiblock

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.energy.EnergyType
import com.tinyyana.lycohism.util.Audit
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Items
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 輝能祭壇 crafting (PROJECT_PLAN §「v0.7 規格」§5). Hold the catalyst, drop the ingredients around a
 * built altar, then right-click the centre: the dropped items converge with particles into a result,
 * spending the player's 輝能 pool. Data-driven via altars.yml. This is where high-tier items are made.
 *
 * ponytail: ingredients remain ordinary dropped Item entities until crafting; short-lived
 * ItemDisplays make the successful pedestal presentation visible without persistent altar state.
 */
class AltarManager(private val plugin: Lycohism) {

    data class Recipe(
        val id: String,
        val altarId: String,
        val catalyst: String,
        val ingredients: Map<String, Int>,
        val sunCost: Int,
        val moonCost: Int,
        val resultId: String,
        val resultAmount: Int,
    )

    private val recipes = mutableListOf<Recipe>()

    init {
        reload()
    }

    fun reload() {
        recipes.clear()
        val section = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("recipes") ?: return
        for (id in section.getKeys(false)) {
            val node = section.getConfigurationSection(id) ?: continue
            val altarId = node.getString("altar", "energy_altar")?.trim() ?: "energy_altar"
            val catalyst = node.getString("catalyst")?.trim() ?: continue
            val ingredients = node.getStringList("ingredients").mapNotNull { token ->
                val parts = token.split(":")
                val key = parts.getOrNull(0)?.trim() ?: return@mapNotNull null
                val count = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1
                key to count
            }.toMap()
            val result = node.getString("result", "")!!.split(":")
            val resultId = result.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() } ?: continue
            recipes += Recipe(
                id = id,
                altarId = altarId,
                catalyst = catalyst,
                ingredients = ingredients,
                sunCost = node.getInt("energy-sun", 0).coerceAtLeast(0),
                moonCost = node.getInt("energy-moon", 0).coerceAtLeast(0),
                resultId = resultId,
                resultAmount = result.getOrNull(1)?.trim()?.toIntOrNull() ?: 1,
            )
        }
        plugin.logger.info("Loaded ${recipes.size} altar recipes.")
    }

    fun recipes(): List<Recipe> = recipes.toList()
    fun recipes(altarId: String): List<Recipe> = recipes.filter { it.altarId == altarId }

    enum class Result { CRAFTED, NO_RECIPE, MISSING_INGREDIENTS, MISSING_ENERGY, INVALID }

    /** Attempts a craft at the altar centred on [block], with [held] as the catalyst. */
    fun craft(player: Player, block: Block, held: ItemStack, altarId: String = "energy_altar"): Result {
        if (plugin.multiblockRegistry.get(altarId)?.detectRotation(block.world, block.x, block.y, block.z) == null) {
            return Result.INVALID
        }
        val catalystKey = key(held)
        val recipe = recipes.firstOrNull { it.altarId == altarId && it.catalyst == catalystKey } ?: return Result.NO_RECIPE

        val centre = block.location.add(0.5, 0.5, 0.5)
        val dropped = centre.world.getNearbyEntities(centre, RADIUS, RADIUS, RADIUS).filterIsInstance<Item>()
        val pool = HashMap<String, Int>()
        dropped.forEach { pool.merge(key(it.itemStack), it.itemStack.amount, Int::plus) }
        if (recipe.ingredients.any { (k, c) -> (pool[k] ?: 0) < c }) return Result.MISSING_INGREDIENTS

        if (plugin.energyManager.get(player, EnergyType.SUN) < recipe.sunCost ||
            plugin.energyManager.get(player, EnergyType.MOON) < recipe.moonCost
        ) return Result.MISSING_ENERGY

        val result = plugin.adminPanel.buildItem(recipe.resultId, recipe.resultAmount) ?: return Result.INVALID

        // Convergence flourish: stream particles from each ingredient into the centre before it's
        // consumed, so the craft reads as the ring of materials being drawn in.
        showPedestalIngredients(recipe, dropped, centre)
        dropped.forEach { converge(it.location, centre) }

        // Consume: catalyst (1), dropped ingredients, energy.
        if (player.gameMode != org.bukkit.GameMode.CREATIVE) {
            if (held.amount <= 1) player.inventory.setItemInMainHand(null) else held.amount -= 1
        }
        recipe.ingredients.forEach { (k, c) -> consumeDropped(dropped, k, c) }
        plugin.energyManager.spend(player, EnergyType.SUN, recipe.sunCost)
        plugin.energyManager.spend(player, EnergyType.MOON, recipe.moonCost)

        centre.world.spawnParticle(Particle.END_ROD, centre, 40, 0.8, 0.8, 0.8, 0.02)
        centre.world.spawnParticle(Particle.WITCH, centre, 30, 0.6, 0.6, 0.6, 0.01)
        centre.world.playSound(centre, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f)
        centre.world.dropItem(centre.clone().add(0.0, 1.0, 0.0), result)
        plugin.playerDataManager.discover(player.uniqueId, recipe.resultId)
        Audit.log(player, "altar-craft", "${recipe.resultId} x${recipe.resultAmount}")
        return Result.CRAFTED
    }

    private fun consumeDropped(dropped: List<Item>, targetKey: String, amount: Int) {
        var remaining = amount
        for (entity in dropped) {
            if (remaining <= 0) break
            if (key(entity.itemStack) != targetKey) continue
            val stack = entity.itemStack
            val take = minOf(remaining, stack.amount)
            remaining -= take
            if (stack.amount <= take) {
                entity.remove()
            } else {
                stack.amount -= take
                entity.itemStack = stack
            }
        }
    }

    /** Streams end-rod particles from [from] up into the altar [centre]. */
    private fun converge(from: org.bukkit.Location, centre: org.bukkit.Location) {
        val world = centre.world
        val dx = centre.x - from.x; val dy = centre.y + 1.0 - from.y; val dz = centre.z - from.z
        val steps = maxOf(2, Math.sqrt(dx * dx + dy * dy + dz * dz).toInt() * 2)
        for (i in 0..steps) {
            val t = i.toDouble() / steps
            world.spawnParticle(Particle.END_ROD, from.x + dx * t, from.y + dy * t, from.z + dz * t, 1, 0.02, 0.02, 0.02, 0.0)
        }
    }

    private fun showPedestalIngredients(recipe: Recipe, dropped: List<Item>, centre: org.bukkit.Location) {
        recipe.ingredients.keys.mapNotNull { ingredient ->
            dropped.firstOrNull { key(it.itemStack) == ingredient }?.itemStack?.clone()?.apply { amount = 1 }
        }.take(PEDESTAL_OFFSETS.size).forEachIndexed { index, stack ->
            val (dx, dz) = PEDESTAL_OFFSETS[index]
            val display = centre.world.spawn(
                centre.clone().add(dx, -0.25, dz),
                ItemDisplay::class.java,
            ) { entity ->
                entity.setItemStack(stack)
                entity.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.GROUND
            }
            plugin.server.scheduler.runTaskLater(plugin, Runnable(display::remove), 20L)
        }
    }

    /** A dropped item's recipe key: its Lycohism content id, or else its vanilla material name. */
    private fun key(item: ItemStack): String = Items.idOf(item) ?: item.type.name

    private companion object {
        const val FILE_NAME = "altars.yml"
        const val RADIUS = 2.5
        val PEDESTAL_OFFSETS = listOf(-1.0 to -1.0, 1.0 to -1.0, 1.0 to 1.0, -1.0 to 1.0)
    }
}
