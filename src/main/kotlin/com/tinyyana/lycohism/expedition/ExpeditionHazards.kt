package com.tinyyana.lycohism.expedition

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.scheduler.BukkitTask
import kotlin.random.Random

/**
 * Light, atmospheric danger for expedition worlds. On a slow timer it occasionally spawns a
 * single themed mob near players standing in an expedition that defines a hazard, capped so the
 * world stays explorable rather than a combat gauntlet (PROJECT_PLAN §19.5). All tuning lives in
 * each expedition's `hazard` block, so this class never hard-codes balance.
 */
class ExpeditionHazards(private val plugin: Lycohism) {

    private var task: BukkitTask? = null

    fun start() {
        stop()
        task = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { tick() },
            SCAN_INTERVAL_TICKS,
            SCAN_INTERVAL_TICKS,
        )
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private fun tick() {
        for (player in plugin.server.onlinePlayers) {
            val expedition = plugin.expeditionManager.expeditionAt(player.world) ?: continue
            val hazard = expedition.hazard ?: continue
            trySpawn(player, expedition, hazard)
        }
    }

    private fun trySpawn(player: Player, expedition: Expedition, hazard: ExpeditionHazard) {
        if (hazard.nightOnly && !isNight(player)) return
        if (Random.nextDouble() > hazard.chance) return
        if (countNearby(player, expedition, hazard) >= hazard.maxNearby) return
        val location = findSpawn(player, hazard) ?: return
        spawnMob(location, expedition, hazard)
    }

    /** Counts our own tagged hazard mobs near the player so natural mobs don't suppress spawns. */
    private fun countNearby(player: Player, expedition: Expedition, hazard: ExpeditionHazard): Int {
        val r = hazard.radius.toDouble()
        return player.getNearbyEntities(r, r, r).count { entity ->
            entity is LivingEntity &&
                entity.persistentDataContainer.get(Keys.expeditionMob, PersistentDataType.STRING) == expedition.id
        }
    }

    /** Picks a surface spot in a ring around the player, skipping canopy and unloaded chunks. */
    private fun findSpawn(player: Player, hazard: ExpeditionHazard): Location? {
        val world = player.world
        repeat(SPAWN_ATTEMPTS) {
            val angle = Random.nextDouble(0.0, TWO_PI)
            val dist = Random.nextDouble(MIN_SPAWN_DISTANCE.toDouble(), hazard.radius.toDouble())
            val bx = player.location.blockX + (Math.cos(angle) * dist).toInt()
            val bz = player.location.blockZ + (Math.sin(angle) * dist).toInt()
            if (!world.isChunkLoaded(bx shr 4, bz shr 4)) return@repeat

            val top = world.getHighestBlockAt(bx, bz)
            val name = top.type.name
            if ("LEAVES" in name || "LOG" in name) return@repeat // don't drop mobs into tree canopy

            val location = top.location.add(0.5, 1.0, 0.5)
            if (location.distanceSquared(player.location) < (MIN_SPAWN_DISTANCE * MIN_SPAWN_DISTANCE).toDouble()) {
                return@repeat
            }
            return location
        }
        return null
    }

    private fun spawnMob(location: Location, expedition: Expedition, hazard: ExpeditionHazard) {
        val world = location.world ?: return
        val entity = world.spawnEntity(location, hazard.mobType) as? LivingEntity ?: return
        entity.customName(
            Messages.parse(Texts.line(hazard.displayNameKey)).decoration(TextDecoration.ITALIC, false),
        )
        entity.isCustomNameVisible = false
        entity.removeWhenFarAway = true
        entity.persistentDataContainer.set(Keys.expeditionMob, PersistentDataType.STRING, expedition.id)
        hazard.effects.forEach { (type, amplifier) ->
            entity.addPotionEffect(PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier, false, false, false))
        }
        if (hazard.scale != 1.0) applyScale(entity, hazard.scale)
        plugin.debugLog(
            "Spawned hazard ${hazard.mobType} for '${expedition.id}' at " +
                "${location.blockX},${location.blockY},${location.blockZ}",
        )
    }

    /** Sets the entity's body scale via the generic.scale attribute; no-op if unsupported. */
    private fun applyScale(entity: LivingEntity, scale: Double) {
        runCatching {
            val attribute = org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft("scale")) ?: return
            val instance = entity.getAttribute(attribute) ?: run {
                entity.registerAttribute(attribute)
                entity.getAttribute(attribute)
            }
            instance?.baseValue = scale
        }.onFailure { plugin.debugLog("Could not scale hazard mob: ${it.message}") }
    }

    private fun isNight(player: Player): Boolean = player.world.time in NIGHT_START..NIGHT_END

    private companion object {
        const val SCAN_INTERVAL_TICKS = 200L // ~10s between scans; tune presence via hazard.chance
        const val SPAWN_ATTEMPTS = 8
        const val MIN_SPAWN_DISTANCE = 12
        const val NIGHT_START = 13000L
        const val NIGHT_END = 23000L
        const val TWO_PI = Math.PI * 2.0
    }
}
