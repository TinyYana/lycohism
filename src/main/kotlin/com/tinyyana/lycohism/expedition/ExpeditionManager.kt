package com.tinyyana.lycohism.expedition

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Advancements
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.title.Title
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.block.Biome
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.time.Duration

/**
 * Loads expeditions from expeditions.yml and owns their separate worlds: lazy creation,
 * weather theming, entry and return. The player's origin is stored on their own PDC so the
 * gate returns them home even across relogs. Data-driven; new expeditions need only YAML.
 */
class ExpeditionManager(private val plugin: Lycohism) {

    private val expeditions = LinkedHashMap<String, Expedition>()

    init {
        reload()
    }

    fun reload() {
        expeditions.clear()
        val section = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("expeditions")
        if (section == null) {
            plugin.logger.warning("$FILE_NAME has no 'expeditions' section; no expeditions loaded.")
            return
        }
        for (id in section.getKeys(false)) {
            val node = section.getConfigurationSection(id) ?: continue
            expeditions[id] = parse(id, node)
        }
        plugin.logger.info("Loaded ${expeditions.size} expeditions.")
    }

    fun get(id: String): Expedition? = expeditions[id]

    /** All configured expeditions, in declaration order — used by the 書房 chooser. */
    fun all(): List<Expedition> = expeditions.values.toList()

    /** The first expedition; the legacy gate item and admin shortcut still use this. */
    fun primary(): Expedition? = expeditions.values.firstOrNull()

    /** The expedition whose world a player currently stands in, or null in the main world. */
    fun expeditionAt(world: World): Expedition? =
        expeditions.values.firstOrNull { it.worldName == world.name }

    /**
     * Gate behaviour: return home when already inside an expedition world, otherwise enter the
     * primary expedition. Returns false (with a player message) when nothing is configured.
     */
    fun useGate(player: Player, bypassUnlock: Boolean = false): Boolean {
        val here = expeditionAt(player.world)
        if (here != null) {
            leave(player)
            return true
        }
        val target = primary() ?: run {
            Messages.send(player, Texts.line("messages.expedition.none-configured"))
            return false
        }
        if (!bypassUnlock && !isUnlocked(player, target)) {
            Messages.send(player, Texts.line("messages.expedition.locked"))
            return false
        }
        enter(player, target)
        return true
    }

    /** Gate behaviour for a specific expedition (the 書房 lists each one). */
    fun useGateFor(player: Player, expedition: Expedition, bypassUnlock: Boolean = false): Boolean {
        if (expeditionAt(player.world) != null) {
            leave(player)
            return true
        }
        if (!bypassUnlock && !isUnlocked(player, expedition)) {
            Messages.send(player, Texts.line("messages.expedition.locked"))
            return false
        }
        enter(player, expedition)
        return true
    }

    /**
     * Whether [player] has met the vanilla-advancement gate for [expedition]. Used both to allow
     * entry and to show the gate's craft button locked in the 書房 until the milestone is met.
     */
    fun isUnlocked(player: Player, expedition: Expedition): Boolean =
        Advancements.hasAll(player, expedition.requiresAdvancements)

    /** Convenience: is the single v0.5 expedition unlocked for this player? */
    fun isPrimaryUnlocked(player: Player): Boolean {
        val target = primary() ?: return false
        return isUnlocked(player, target)
    }

    fun enter(player: Player, expedition: Expedition) {
        val world = ensureWorld(expedition)
        // Only remember where the player came from when they start in a non-expedition world,
        // so entering straight from another expedition (or re-entering) can never overwrite the
        // real home origin and trap the player in expedition worlds.
        if (expeditionAt(player.world) == null) {
            saveOrigin(player, player.location)
        }
        player.world.spawnParticle(Particle.PORTAL, player.location.add(0.0, 1.0, 0.0), 40, 0.6, 1.0, 0.6, 0.2)
        player.playSound(player.location, Sound.BLOCK_PORTAL_TRIGGER, 0.35f, 1.35f)
        player.teleport(safeSpawn(world))
        player.world.spawnParticle(Particle.RAIN, player.location.add(0.0, 1.0, 0.0), 30, 1.0, 1.0, 1.0, 0.0)
        player.showTitle(
            Title.title(
                Messages.parse(Texts.line("messages.expedition.title.${expedition.id}", expedition.displayName)),
                Messages.parse(Texts.line("messages.expedition.subtitle.${expedition.id}", "")),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1)),
            ),
        )
        player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.8f)
        player.playSound(player.location, Sound.BLOCK_BEACON_AMBIENT, 0.2f, 1.6f)
        Messages.send(player, Texts.render("messages.expedition.entered", "name" to expedition.displayName))
        val firstVisit = plugin.playerDataManager.discover(player.uniqueId, expedition.id)
        if (firstVisit && expedition.firstHint.isNotEmpty()) {
            Messages.send(player, expedition.firstHint)
        }
        plugin.debugLog("${player.name} entered expedition '${expedition.id}'")
    }

    /** Sends the player back to their stored origin, or the main world spawn as a fallback. */
    fun leave(player: Player) {
        val origin = readOrigin(player) ?: mainWorldSpawn()
        player.teleport(origin)
        player.world.spawnParticle(Particle.PORTAL, player.location.add(0.0, 1.0, 0.0), 40, 0.6, 1.0, 0.6, 0.2)
        player.playSound(player.location, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 0.8f)
        clearOrigin(player)
        Messages.send(player, Texts.line("messages.expedition.returned"))
        plugin.debugLog("${player.name} left an expedition world")
    }

    // ---- World lifecycle ---------------------------------------------------

    private fun ensureWorld(expedition: Expedition): World {
        val existing = plugin.server.getWorld(expedition.worldName)
        val world = existing ?: createWorld(expedition)
        if (expedition.alwaysRaining) lockRain(world)
        if (expedition.alwaysNight) lockNight(world)
        return world
    }

    /** Locks the world to permanent night for the 永夜荒原 atmosphere (and round-the-clock 月輝). */
    @Suppress("DEPRECATION") // GameRule.DO_DAYLIGHT_CYCLE field is deprecated upstream but still the supported handle.
    private fun lockNight(world: World) {
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        world.time = 18000
    }

    private fun createWorld(expedition: Expedition): World {
        val creator = WorldCreator(expedition.worldName).environment(World.Environment.NORMAL)
        if (expedition.biomes.isNotEmpty()) {
            creator.biomeProvider(RainfallBiomeProvider(expedition.biomes))
        }
        return creator.createWorld()
            ?: error("Could not create expedition world '${expedition.worldName}'")
    }

    /** Locks the world to permanent rain so the "雨後森林" atmosphere never cycles away. */
    @Suppress("DEPRECATION") // GameRule.DO_WEATHER_CYCLE field is deprecated upstream but still the supported handle.
    private fun lockRain(world: World) {
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        world.setStorm(true)
        world.isThundering = false
        world.weatherDuration = Int.MAX_VALUE
    }

    private fun safeSpawn(world: World): Location {
        val spawn = world.spawnLocation
        val ground = world.getHighestBlockAt(spawn)
        return ground.location.add(0.5, 1.0, 0.5)
    }

    private fun mainWorldSpawn(): Location =
        plugin.server.worlds.first().spawnLocation

    // ---- Origin storage (player PDC) ---------------------------------------

    private fun saveOrigin(player: Player, location: Location) {
        val world = location.world
        val encoded = listOf(
            world.name,
            location.x, location.y, location.z,
            location.yaw, location.pitch,
        ).joinToString(";")
        player.persistentDataContainer.set(Keys.expeditionOrigin, PersistentDataType.STRING, encoded)
    }

    private fun readOrigin(player: Player): Location? {
        val encoded = player.persistentDataContainer.get(Keys.expeditionOrigin, PersistentDataType.STRING) ?: return null
        val parts = encoded.split(";")
        if (parts.size != 6) return null
        val world = plugin.server.getWorld(parts[0]) ?: return null
        val x = parts[1].toDoubleOrNull() ?: return null
        val y = parts[2].toDoubleOrNull() ?: return null
        val z = parts[3].toDoubleOrNull() ?: return null
        val yaw = parts[4].toFloatOrNull() ?: 0f
        val pitch = parts[5].toFloatOrNull() ?: 0f
        return Location(world, x, y, z, yaw, pitch)
    }

    private fun clearOrigin(player: Player) {
        player.persistentDataContainer.remove(Keys.expeditionOrigin)
    }

    private fun parse(id: String, node: ConfigurationSection): Expedition = Expedition(
        id = id,
        displayName = Texts.line("content-names.$id", node.getString("name", id)!!),
        worldName = node.getString("world-name", "lycohism_$id")!!,
        alwaysRaining = node.getBoolean("always-raining", false),
        alwaysNight = node.getBoolean("always-night", false),
        energyMultiplier = node.getDouble("energy-multiplier", 1.0).coerceIn(0.0, 10.0),
        biomes = parseBiomes(id, node),
        requiresAdvancements = parseAdvancements(id, node),
        hazard = parseHazard(id, node),
        firstHint = Texts.line("messages.expedition.first-hint.$id", ""),
    )

    private fun parseBiomes(id: String, node: ConfigurationSection): List<Biome> =
        node.getStringList("biomes").mapNotNull { name ->
            resolveBiome(name).also { biome ->
                if (biome == null) plugin.logger.warning("Expedition '$id': unknown biome '$name'; ignoring.")
            }
        }

    /** Reads requires-advancements, dropping malformed keys with a warning (fail-closed). */
    private fun parseAdvancements(id: String, node: ConfigurationSection): Set<String> =
        node.getStringList("requires-advancements")
            .map { it.trim() }
            .filter { key ->
                Advancements.isValidKey(key).also { valid ->
                    if (!valid) plugin.logger.warning("Expedition '$id': invalid advancement key '$key'; ignoring.")
                }
            }
            .toSet()

    /** Reads the optional hazard block; null (disabled) on missing section or unknown mob. */
    private fun parseHazard(id: String, node: ConfigurationSection): ExpeditionHazard? {
        val hazard = node.getConfigurationSection("hazard") ?: return null
        if (!hazard.getBoolean("enabled", true)) return null
        val mobName = hazard.getString("mob", "DROWNED")!!.trim().uppercase()
        val mobType = runCatching { EntityType.valueOf(mobName) }.getOrNull()
        if (mobType == null || !mobType.isSpawnable || !mobType.isAlive) {
            plugin.logger.warning("Expedition '$id': hazard mob '$mobName' is not a spawnable living entity; hazard disabled.")
            return null
        }
        return ExpeditionHazard(
            mobType = mobType,
            displayNameKey = hazard.getString("display-name-key", "content-names.rain_wraith")!!,
            chance = hazard.getDouble("chance", 0.3).coerceIn(0.0, 1.0),
            maxNearby = hazard.getInt("max-nearby", 2).coerceAtLeast(1),
            radius = hazard.getInt("radius", 24).coerceIn(8, 64),
            nightOnly = hazard.getBoolean("night-only", true),
            effects = parseEffects(id, hazard),
            scale = hazard.getDouble("scale", 1.0).coerceIn(0.25, 4.0),
        )
    }

    /** Reads the optional hazard `effects` section: POTION_EFFECT -> amplifier. Unknown types dropped. */
    private fun parseEffects(id: String, hazard: ConfigurationSection): Map<org.bukkit.potion.PotionEffectType, Int> {
        val section = hazard.getConfigurationSection("effects") ?: return emptyMap()
        val effects = LinkedHashMap<org.bukkit.potion.PotionEffectType, Int>()
        for (key in section.getKeys(false)) {
            val type = org.bukkit.Registry.EFFECT.get(org.bukkit.NamespacedKey.minecraft(key.trim().lowercase()))
            if (type == null) {
                plugin.logger.warning("Expedition '$id': unknown potion effect '$key' in hazard effects; ignoring.")
                continue
            }
            effects[type] = section.getInt(key).coerceIn(0, 9)
        }
        return effects
    }

    @Suppress("DEPRECATION") // Registry.BIOME is deprecated upstream (RegistryAccess) but still the supported lookup.
    private fun resolveBiome(name: String): Biome? {
        val key = NamespacedKey.fromString(name.trim().lowercase()) ?: return null
        return runCatching { Registry.BIOME.get(key) }.getOrNull()
    }

    companion object {
        private const val FILE_NAME = "expeditions.yml"
    }
}
