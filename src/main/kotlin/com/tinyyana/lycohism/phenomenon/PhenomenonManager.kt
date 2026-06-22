package com.tinyyana.lycohism.phenomenon

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Advancements
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.Location
import org.bukkit.persistence.PersistentDataType

/**
 * Loads natural phenomena from phenomena.yml, matches them against in-world actions,
 * and builds their drop items. Data-driven so new phenomena need only a YAML entry.
 */
class PhenomenonManager(private val plugin: Lycohism) {

    private val phenomena = LinkedHashMap<String, Phenomenon>()

    init {
        reload()
    }

    fun reload() {
        phenomena.clear()
        val yaml = ConfigFiles.load(plugin, FILE_NAME)
        val section = yaml.getConfigurationSection("phenomena")
        if (section == null) {
            plugin.logger.warning("$FILE_NAME has no 'phenomena' section; no phenomena loaded.")
            return
        }
        for (id in section.getKeys(false)) {
            val node = section.getConfigurationSection(id) ?: continue
            parse(id, node)?.let { phenomena[id] = it }
        }
        plugin.logger.info("Loaded ${phenomena.size} phenomena.")
    }

    /** Returns the phenomenon registered under [id], or null if unknown. */
    fun get(id: String): Phenomenon? = phenomena[id]

    /** All loaded phenomena, e.g. for scanning mob-kill drops. */
    fun all(): Collection<Phenomenon> = phenomena.values

    /**
     * Finds the first phenomenon whose conditions the broken [block] satisfies for [player].
     * Advancement gates only apply when a phenomenon declares them.
     */
    fun match(block: Block, player: Player): Phenomenon? =
        phenomena.values.firstOrNull { p ->
            block.type in p.sourceBlocks &&
                conditionsMatch(p, block.location) &&
                Advancements.hasAll(player, p.requiresAdvancements)
        }

    /**
     * Phenomena whose environmental conditions are active at [location] for [player],
     * regardless of source block. Advancement gates only apply when declared.
     */
    fun available(location: Location, player: Player): List<Phenomenon> =
        phenomena.values.filter {
            conditionsMatch(it, location) && Advancements.hasAll(player, it.requiresAdvancements)
        }

    fun createItem(phenomenon: Phenomenon, amount: Int = 1): ItemStack {
        val item = ItemStack(phenomenon.baseMaterial, amount)
        item.editMeta { meta ->
            val localizedName = Texts.line("content-names.${phenomenon.id}", phenomenon.displayName)
            meta.displayName(Messages.parse(localizedName).decoration(TextDecoration.ITALIC, false))
            if (phenomenon.lore.isNotEmpty()) {
                meta.lore(phenomenon.lore.map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
            }
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, phenomenon.id)
        }
        return item
    }

    private fun parse(id: String, node: ConfigurationSection): Phenomenon? {
        val baseMaterial = node.getString("item-material")
            ?.let { material(it, id, "item-material") }
            ?: run {
                plugin.logger.warning("Phenomenon '$id' is missing a valid item-material; skipping.")
                return null
            }

        // Empty source-blocks is allowed: some phenomena (e.g. 蝕輝結晶) have no natural break source and
        // are only ever granted by code (BOSS drop) or mob-drops. They must still load so get()/createItem
        // work — previously the skip here meant the eclipse boss silently dropped nothing.
        val sourceBlocks = node.getStringList("source-blocks")
            .mapNotNull { material(it, id, "source-blocks") }
            .toSet()
        if (sourceBlocks.isEmpty() && !node.isConfigurationSection("mob-drops") && node.getDouble("chance", 0.2) > 0.0) {
            plugin.logger.warning("Phenomenon '$id' has no source-blocks or mob-drops yet a non-zero chance; it can never drop naturally.")
        }

        val weather = enumOrDefault(node.getString("weather"), WeatherCondition.ANY, id, "weather")
        val environment = enumOrDefault(node.getString("environment"), World.Environment.NORMAL, id, "environment")

        val minY = node.getInt("height.min", Int.MIN_VALUE)
        val maxY = node.getInt("height.max", Int.MAX_VALUE)
        if (minY > maxY) {
            plugin.logger.warning("Phenomenon '$id': height.min cannot exceed height.max; skipping.")
            return null
        }

        return Phenomenon(
            id = id,
            displayName = node.getString("name", id)!!,
            baseMaterial = baseMaterial,
            lore = node.getStringList("lore"),
            sourceBlocks = sourceBlocks,
            timeStart = node.getLong("time.start", 0),
            timeEnd = node.getLong("time.end", 24000),
            minY = minY,
            maxY = maxY,
            moonPhases = node.getIntegerList("moon-phases")
                .filter { it in 0..7 }
                .toSet(),
            weather = weather,
            environment = environment,
            chance = node.getDouble("chance", 0.2).coerceIn(0.0, 1.0),
            firstHint = node.getString("first-hint", "")!!,
            requiresAdvancements = parseAdvancements(id, node),
            worlds = node.getStringList("worlds").map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
            mobDrops = parseMobDrops(id, node),
        )
    }

    /** Reads the optional `mob-drops` section: ENTITY_TYPE -> chance. Unknown types are dropped. */
    private fun parseMobDrops(id: String, node: ConfigurationSection): Map<EntityType, Double> {
        val section = node.getConfigurationSection("mob-drops") ?: return emptyMap()
        val drops = LinkedHashMap<EntityType, Double>()
        for (key in section.getKeys(false)) {
            val type = runCatching { EntityType.valueOf(key.trim().uppercase()) }.getOrNull()
            if (type == null) {
                plugin.logger.warning("Phenomenon '$id': unknown mob type '$key' in mob-drops; ignoring.")
                continue
            }
            drops[type] = section.getDouble(key).coerceIn(0.0, 1.0)
        }
        return drops
    }

    private fun parseAdvancements(id: String, node: ConfigurationSection): Set<String> =
        node.getStringList("requires-advancements")
            .map { it.trim() }
            .filter { key ->
                Advancements.isValidKey(key).also { valid ->
                    if (!valid) plugin.logger.warning("Phenomenon '$id': invalid advancement key '$key'; ignoring.")
                }
            }
            .toSet()

    private fun material(name: String, id: String, field: String): Material? {
        val matched = Material.matchMaterial(name)
        if (matched == null) plugin.logger.warning("Phenomenon '$id': unknown material '$name' in $field.")
        return matched
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T, id: String, field: String): T {
        if (value == null) return default
        return runCatching { enumValueOf<T>(value.uppercase()) }.getOrElse {
            plugin.logger.warning("Phenomenon '$id': invalid $field '$value'; using $default.")
            default
        }
    }

    private fun isTimeInWindow(time: Long, start: Long, end: Long): Boolean =
        if (start <= end) time in start..end else time >= start || time <= end

    private fun weatherMatches(world: World, weather: WeatherCondition): Boolean = when (weather) {
        WeatherCondition.ANY -> true
        WeatherCondition.CLEAR -> !world.hasStorm()
        // Thunder is still rain; THUNDER remains available for phenomena that
        // specifically require the stronger condition.
        WeatherCondition.RAIN -> world.hasStorm()
        WeatherCondition.THUNDER -> world.isThundering
    }

    private fun moonPhaseMatches(world: World, moonPhases: Set<Int>): Boolean =
        moonPhases.isEmpty() || ((world.fullTime / 24000L) % 8L).toInt() in moonPhases

    private fun conditionsMatch(phenomenon: Phenomenon, location: Location): Boolean {
        val world = location.world
        return world.environment == phenomenon.environment &&
            (phenomenon.worlds.isEmpty() || world.name in phenomenon.worlds) &&
            location.blockY in phenomenon.minY..phenomenon.maxY &&
            isTimeInWindow(world.time, phenomenon.timeStart, phenomenon.timeEnd) &&
            moonPhaseMatches(world, phenomenon.moonPhases) &&
            weatherMatches(world, phenomenon.weather)
    }

    companion object {
        private const val FILE_NAME = "phenomena.yml"
    }
}
