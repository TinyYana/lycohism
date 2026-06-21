package com.tinyyana.lycohism.world

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.energy.EnergyType
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import org.bukkit.Chunk
import org.bukkit.HeightMap
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Chest
import org.bukkit.block.CreatureSpawner
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkPopulateEvent
import org.bukkit.inventory.ItemStack
import java.util.Random
import kotlin.math.abs

/** Places compact Lycohism landmarks in newly generated main/expedition chunks. */
class StructureGenerator(private val plugin: Lycohism) : Listener {

    private data class Settings(val enabled: Boolean, val chance: Double, val minY: Int, val maxY: Int)
    private data class Site(val world: World, val x: Int, val y: Int, val z: Int, val surface: Map<Pair<Int, Int>, Int>)

    private var mossAltar = Settings(true, 0.0125, 60, 130)
    private var dewWell = Settings(true, 0.008, 60, 130)
    private var windCircle = Settings(true, 0.008, 70, 180)
    private var rainfallDungeon = Settings(true, 0.012, 55, 120)
    private var moondial = Settings(true, 0.01, 60, 140)
    private var sundial = Settings(true, 0.01, 60, 140)

    init {
        load()
    }

    fun load() {
        val root = ConfigFiles.load(plugin, FILE_NAME)
        mossAltar = settings(root, "moss-altar", mossAltar)
        dewWell = settings(root, "dew-well", dewWell)
        windCircle = settings(root, "wind-circle", windCircle)
        rainfallDungeon = settings(root, "rainfall-dungeon", rainfallDungeon)
        moondial = settings(root, "moondial", moondial)
        sundial = settings(root, "sundial", sundial)
    }

    @EventHandler
    fun onChunkPopulate(event: ChunkPopulateEvent) {
        val chunk = event.chunk
        val expedition = plugin.expeditionManager.expeditionAt(chunk.world)
        val mainWorld = chunk.world == plugin.server.worlds.firstOrNull()
        if (!mainWorld && expedition == null) return

        when {
            expedition?.id == "rainfall_forest" && roll(chunk, rainfallDungeon, DUNGEON_SALT) ->
                findSite(chunk, 4, rainfallDungeon, DUNGEON_SALT, 2)?.let(::rainfallDungeon)
            roll(chunk, mossAltar, ALTAR_SALT) ->
                findSite(chunk, 2, mossAltar, ALTAR_SALT, 2)?.let { mossAltar(it, expedition != null) }
            mainWorld && roll(chunk, dewWell, WELL_SALT) ->
                findSite(chunk, 3, dewWell, WELL_SALT, 2)?.let(::dewWell)
            roll(chunk, windCircle, CIRCLE_SALT) ->
                findSite(chunk, 3, windCircle, CIRCLE_SALT, 3)?.let(::windCircle)
            roll(chunk, moondial, MOONDIAL_SALT) ->
                findSite(chunk, 2, moondial, MOONDIAL_SALT, 2)?.let { moondial(it, expedition != null) }
            mainWorld && roll(chunk, sundial, SUNDIAL_SALT) ->
                findSite(chunk, 2, sundial, SUNDIAL_SALT, 2)?.let(::sundial)
        }
    }

    private fun mossAltar(site: Site, expedition: Boolean) {
        foundation(site, 2) { dx, dz -> if (abs(dx) == 2 || abs(dz) == 2) Material.MOSSY_STONE_BRICKS else Material.MOSS_BLOCK }
        listOf(-2 to -2, -2 to 2, 2 to -2, 2 to 2).forEach { (dx, dz) ->
            site.world.getBlockAt(site.x + dx, site.y + 1, site.z + dz).type = Material.MOSSY_COBBLESTONE_WALL
            site.world.getBlockAt(site.x + dx, site.y + 2, site.z + dz).type = Material.LANTERN
        }
        val rewardId = if (expedition) "moss_bloom" else "flower_vein"
        placeChest(site, 0, 1, 0, "structures.moss-altar.chest-name", listOfNotNull(
            phenomenon(rewardId, 3),
            ItemStack(Material.BONE_MEAL, 6),
        ))
    }

    private fun dewWell(site: Site) {
        foundation(site, 3) { dx, dz -> if (abs(dx) == 3 || abs(dz) == 3) Material.MOSSY_COBBLESTONE else Material.STONE_BRICKS }
        for (dx in -2..2) for (dz in -2..2) {
            val block = site.world.getBlockAt(site.x + dx, site.y + 1, site.z + dz)
            block.type = if (abs(dx) <= 1 && abs(dz) <= 1) Material.WATER else Material.MOSSY_STONE_BRICKS
        }
        listOf(-2 to -2, -2 to 2, 2 to -2, 2 to 2).forEach { (dx, dz) ->
            site.world.getBlockAt(site.x + dx, site.y + 2, site.z + dz).type = Material.COBBLESTONE_WALL
            site.world.getBlockAt(site.x + dx, site.y + 3, site.z + dz).type = Material.LANTERN
        }
        placeChest(site, 0, 1, 3, "structures.dew-well.chest-name", listOfNotNull(
            phenomenon("morning_dew", 4),
            ItemStack(Material.GLASS_BOTTLE, 3),
        ))
    }

    private fun windCircle(site: Site) {
        foundation(site, 3) { _, _ -> Material.COARSE_DIRT }
        val stones = listOf(-3 to 0, -2 to -2, 0 to -3, 2 to -2, 3 to 0, 2 to 2, 0 to 3, -2 to 2)
        stones.forEach { (dx, dz) ->
            site.world.getBlockAt(site.x + dx, site.y + 1, site.z + dz).type = Material.POLISHED_ANDESITE
            site.world.getBlockAt(site.x + dx, site.y + 2, site.z + dz).type = Material.CHISELED_STONE_BRICKS
        }
        site.world.getBlockAt(site.x, site.y + 1, site.z).type = Material.LODESTONE
        placeChest(site, 0, 1, 2, "structures.wind-circle.chest-name", listOfNotNull(
            phenomenon("wind_trace", 3),
            ItemStack(Material.FEATHER, 6),
        ))
    }

    private fun rainfallDungeon(site: Site) {
        foundation(site, 4) { dx, dz -> if ((dx + dz) % 3 == 0) Material.MOSSY_STONE_BRICKS else Material.STONE_BRICKS }
        for (dx in -3..3) for (dz in -3..3) for (dy in 1..3) {
            site.world.getBlockAt(site.x + dx, site.y + dy, site.z + dz).type = Material.AIR
        }
        for (dx in -4..4) for (dz in -4..4) {
            val edge = abs(dx) == 4 || abs(dz) == 4
            if (edge) for (dy in 1..3) {
                val entrance = dz == 4 && abs(dx) <= 1 && dy <= 2
                if (!entrance) site.world.getBlockAt(site.x + dx, site.y + dy, site.z + dz).type = Material.MOSSY_STONE_BRICKS
            }
            site.world.getBlockAt(site.x + dx, site.y + 4, site.z + dz).type = Material.STONE_BRICKS
        }
        site.world.getBlockAt(site.x - 2, site.y + 2, site.z + 3).type = Material.COBWEB
        site.world.getBlockAt(site.x + 2, site.y + 1, site.z - 2).type = Material.COBWEB

        site.world.getBlockAt(site.x, site.y + 1, site.z).type = Material.SPAWNER
        (site.world.getBlockAt(site.x, site.y + 1, site.z).state as CreatureSpawner).apply {
            spawnedType = EntityType.DROWNED
            minSpawnDelay = 400
            maxSpawnDelay = 800
            spawnCount = 1
            maxNearbyEntities = 2
            requiredPlayerRange = 8
            spawnRange = 2
            update(true, false)
        }
        placeChest(site, 0, 1, -3, "structures.rainfall-dungeon.chest-name", listOfNotNull(
            phenomenon("moss_bloom", 5),
            plugin.mossBalm.createItem(1),
            plugin.rainBandage.createItem(1),
        ))
    }

    /**
     * 月晷 — the tall (18-block) 月輝塔, placed from the shared rigid multiblock template so the
     * natural landmark and the player-buildable tower are the same definition. Registers as a 月輝
     * tower (aura + nearby charge boost) and leaves a reward chest at its base.
     */
    private fun moondial(site: Site, expedition: Boolean) {
        plugin.multiblockRegistry.get("moon_tower")?.place(site.world, site.x, site.y, site.z)
        plugin.energyTowers.record(site.world.name, site.x, site.y + 15, site.z, EnergyType.MOON)
        val rewards = if (expedition) {
            listOfNotNull(phenomenon("moon_core", 2), ItemStack(Material.AMETHYST_SHARD, 4))
        } else {
            listOf(ItemStack(Material.AMETHYST_SHARD, 6), ItemStack(Material.AMETHYST_CLUSTER, 1))
        }
        placeChest(site, 2, 1, 0, "structures.moondial.chest-name", rewards)
    }

    /**
     * 日晷 — the sun counterpart: bright sandstone tiers, a glowstone-lit spire crowned with gold
     * and an end-rod ray. A 日輝 tower (overworld only). Its chest seeds a 蓄能晶 so players naturally
     * meet the 輝能 system out in the world.
     */
    private fun sundial(site: Site) {
        plugin.multiblockRegistry.get("sun_tower")?.place(site.world, site.x, site.y, site.z)
        plugin.energyTowers.record(site.world.name, site.x, site.y + 15, site.z, EnergyType.SUN)
        placeChest(site, 2, 1, 0, "structures.sundial.chest-name", listOf(
            plugin.energyCrystal.createItem(1),
            ItemStack(Material.GLOWSTONE_DUST, 4),
        ))
    }

    private fun placeChest(site: Site, dx: Int, dy: Int, dz: Int, namePath: String, rewards: List<ItemStack>) {
        val block = site.world.getBlockAt(site.x + dx, site.y + dy, site.z + dz)
        block.type = Material.CHEST
        val chest = block.state as Chest
        chest.customName(Messages.parse(Texts.line(namePath)))
        val inventory = chest.snapshotInventory
        rewards.forEach { inventory.addItem(it) }
        chest.update(true, false)
    }

    private fun phenomenon(id: String, amount: Int): ItemStack? =
        plugin.phenomenonManager.get(id)?.let { plugin.phenomenonManager.createItem(it, amount) }

    private fun foundation(site: Site, radius: Int, top: (Int, Int) -> Material) {
        for (dx in -radius..radius) for (dz in -radius..radius) {
            val surfaceY = site.surface.getValue(dx to dz)
            for (fillY in surfaceY..site.y) {
                site.world.getBlockAt(site.x + dx, fillY, site.z + dz).type = Material.MOSSY_COBBLESTONE
            }
            site.world.getBlockAt(site.x + dx, site.y, site.z + dz).type = top(dx, dz)
        }
    }

    private fun findSite(chunk: Chunk, radius: Int, settings: Settings, salt: Long, maxSlope: Int): Site? {
        val random = Random(StructureRoll.seed(chunk.world.seed xor salt, chunk.x, chunk.z))
        val span = 14 - radius * 2
        // A single sample almost always failed in wet/sloped expedition terrain (water surfaces and
        // hilly ground), so landmarks "almost never" appeared. Try several deterministic candidates.
        repeat(SITE_ATTEMPTS) {
            val centerX = (chunk.x shl 4) + radius + 1 + random.nextInt(span)
            val centerZ = (chunk.z shl 4) + radius + 1 + random.nextInt(span)
            val surface = buildMap {
                for (dx in -radius..radius) for (dz in -radius..radius) {
                    put(dx to dz, chunk.world.getHighestBlockYAt(centerX + dx, centerZ + dz, HeightMap.MOTION_BLOCKING_NO_LEAVES))
                }
            }
            val y = surface.values.maxOrNull() ?: return@repeat
            if (y !in settings.minY..settings.maxY || y - (surface.values.minOrNull() ?: y) > maxSlope) return@repeat
            if (surface.any { (offset, surfaceY) ->
                    !chunk.world.getBlockAt(centerX + offset.first, surfaceY, centerZ + offset.second).type.isSolid
                }) return@repeat
            return Site(chunk.world, centerX, y, centerZ, surface)
        }
        return null
    }

    private fun roll(chunk: Chunk, settings: Settings, salt: Long): Boolean =
        settings.enabled && StructureRoll.shouldGenerate(chunk.world.seed xor salt, chunk.x, chunk.z, settings.chance)

    private fun settings(root: org.bukkit.configuration.file.YamlConfiguration, id: String, fallback: Settings): Settings {
        val node = root.getConfigurationSection("structures.$id") ?: return fallback
        return Settings(
            node.getBoolean("enabled", fallback.enabled),
            node.getDouble("chance-per-chunk", fallback.chance).coerceIn(0.0, 1.0),
            node.getInt("min-y", fallback.minY),
            node.getInt("max-y", fallback.maxY).coerceAtLeast(node.getInt("min-y", fallback.minY)),
        )
    }

    private companion object {
        const val FILE_NAME = "structures.yml"
        const val SITE_ATTEMPTS = 8
        const val ALTAR_SALT = 101L
        const val WELL_SALT = 211L
        const val CIRCLE_SALT = 307L
        const val DUNGEON_SALT = 401L
        const val MOONDIAL_SALT = 509L
        const val SUNDIAL_SALT = 613L
    }
}
