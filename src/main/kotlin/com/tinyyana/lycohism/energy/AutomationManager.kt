package com.tinyyana.lycohism.energy

import com.tinyyana.lycohism.Lycohism
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.UUID

/**
 * 自動調律機 (attunement_engine) — v0.9.1 automation (TinyYana 拍板: 加工/培育處理 + 接輝能網路 + 多方塊).
 *
 * A rigid multiblock claimed by right-clicking its 高爐 controller. Each processing cycle it pulls one
 * batch of raw material from the container sitting directly above the controller, spends 日輝/月輝 from
 * the owner's nearest connected 輝能核心, and returns the processed output — the base now does the 工房
 * material-processing the player used to click through by hand. This gives the energy network a steady
 * sink beyond facility upgrades and the boss summon.
 *
 * Lean MVP (ponytail): no per-machine recipe GUI — it auto-runs whatever stocked input matches a recipe.
 * Recipes are data-driven in config.yml; persistence mirrors [NexusManager] (in-memory + slow autosave).
 */
class AutomationManager(private val plugin: Lycohism) {

    data class Engine(val world: String, val x: Int, val y: Int, val z: Int, val owner: UUID)

    private val engines = mutableListOf<Engine>()
    private val file = File(plugin.dataFolder, FILE_NAME)
    private var dirty = false
    private var cyclesSinceSave = 0
    private var cyclesSinceProcess = 0

    private var processEvery = 5
    private var recipes: List<AutomationRecipe> = emptyList()

    init { load() }

    fun load() {
        val cfg = plugin.config.getConfigurationSection("automation")
        processEvery = (cfg?.getInt("process-every-cycles", processEvery) ?: processEvery).coerceAtLeast(1)
        recipes = (cfg?.getStringList("recipes") ?: emptyList()).mapNotNull { token ->
            parseAutomationRecipe(token).also { if (it == null) plugin.logger.warning("Automation: invalid recipe '$token'; ignoring.") }
        }
        engines.clear()
        if (!file.exists()) return
        YamlConfiguration.loadConfiguration(file).getStringList("engines").forEach { decode(it)?.let(engines::add) }
    }

    // ---- Registration (from controller right-click / build) ----------------

    /** Registers an 自動調律機 at the controller [block] owned by [owner]; false if one is already there. */
    fun register(block: Block, owner: UUID): Boolean {
        if (exists(block)) return false
        engines += Engine(block.world.name, block.x, block.y, block.z, owner)
        markDirty()
        return true
    }

    fun exists(block: Block): Boolean =
        engines.any { it.world == block.world.name && it.x == block.x && it.y == block.y && it.z == block.z }

    fun all(): List<Engine> = engines.toList()

    /** Removes a registration when its structure is dismantled (called from pruneBroken). */
    fun removeAt(world: String, x: Int, y: Int, z: Int): Boolean =
        engines.removeAll { it.world == world && it.x == x && it.y == y && it.z == z }.also { if (it) markDirty() }

    // ---- Processing tick (driven by EnergyService) -------------------------

    fun tick() {
        if (++cyclesSinceProcess >= processEvery) {
            cyclesSinceProcess = 0
            if (recipes.isNotEmpty()) engines.toList().forEach(::process)
        }
        if (++cyclesSinceSave >= SAVE_EVERY_CYCLES) {
            cyclesSinceSave = 0
            if (dirty) save()
        }
    }

    private fun process(engine: Engine) {
        val world = plugin.server.getWorld(engine.world) ?: return
        if (!world.isChunkLoaded(engine.x shr 4, engine.z shr 4)) return
        val container = world.getBlockAt(engine.x, engine.y + 1, engine.z).state as? Container ?: return
        val inv = container.inventory
        val recipe = selectRecipe(recipes) { inv.contains(it) } ?: return
        val nexus = plugin.nexusManager.accessibleNexusAt(engine.world, engine.x + 0.5, engine.y + 0.5, engine.z + 0.5, engine.owner) ?: return
        if (!plugin.nexusManager.spend(nexus, recipe.type, recipe.cost)) return
        inv.removeItem(ItemStack(recipe.input, 1))
        val overflow = inv.addItem(ItemStack(recipe.output, recipe.amount))
        val drop = container.location.add(0.5, 1.0, 0.5)
        overflow.values.forEach { world.dropItemNaturally(drop, it) }
        world.spawnParticle(if (recipe.type == EnergyType.SUN) Particle.WAX_ON else Particle.WAX_OFF, drop, 6, 0.25, 0.25, 0.25, 0.0)
    }

    // ---- Persistence -------------------------------------------------------

    private fun markDirty() { dirty = true }

    fun save() {
        val yaml = YamlConfiguration()
        yaml.set("engines", engines.map { "${it.world};${it.x};${it.y};${it.z};${it.owner}" })
        runCatching { plugin.dataFolder.mkdirs(); yaml.save(file) }
            .onFailure { plugin.logger.warning("Could not save $FILE_NAME: ${it.message}") }
        dirty = false
    }

    private fun decode(encoded: String): Engine? {
        val p = encoded.split(";")
        if (p.size != 5) return null
        return Engine(
            p[0],
            p[1].toIntOrNull() ?: return null,
            p[2].toIntOrNull() ?: return null,
            p[3].toIntOrNull() ?: return null,
            runCatching { UUID.fromString(p[4]) }.getOrNull() ?: return null,
        )
    }

    private companion object {
        const val FILE_NAME = "automations.yml"
        const val SAVE_EVERY_CYCLES = 30
    }
}

/** One automation conversion. Pure data so parsing/selection is unit-testable without Bukkit. */
data class AutomationRecipe(val input: Material, val output: Material, val amount: Int, val type: EnergyType, val cost: Int)

/** Bukkit-free intermediate so the string parsing can be unit-tested without the Material registry. */
data class RecipeSpec(val input: String, val output: String, val amount: Int, val type: EnergyType, val cost: Int)

/** Parses "INPUT>OUTPUT[:n];TYPE;cost" into raw fields (no Material lookup), or null when malformed. */
fun parseRecipeSpec(token: String): RecipeSpec? {
    val parts = token.split(";")
    if (parts.size != 3) return null
    val io = parts[0].split(">")
    if (io.size != 2) return null
    val input = io[0].trim().ifEmpty { return null }
    val outTokens = io[1].trim().split(":")
    val output = outTokens[0].trim().ifEmpty { return null }
    val amount = outTokens.getOrNull(1)?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val type = runCatching { EnergyType.valueOf(parts[1].trim().uppercase()) }.getOrNull() ?: return null
    val cost = parts[2].trim().toIntOrNull()?.coerceAtLeast(0) ?: return null
    return RecipeSpec(input, output, amount, type, cost)
}

/** Resolves a recipe token to materials, or null when malformed or naming an unknown material. */
fun parseAutomationRecipe(token: String): AutomationRecipe? {
    val spec = parseRecipeSpec(token) ?: return null
    val input = Material.matchMaterial(spec.input) ?: return null
    val output = Material.matchMaterial(spec.output) ?: return null
    return AutomationRecipe(input, output, spec.amount, spec.type, spec.cost)
}

/** First recipe whose input the machine currently stocks; pure so it is unit-tested directly. */
fun selectRecipe(recipes: List<AutomationRecipe>, has: (Material) -> Boolean): AutomationRecipe? =
    recipes.firstOrNull { has(it.input) }
