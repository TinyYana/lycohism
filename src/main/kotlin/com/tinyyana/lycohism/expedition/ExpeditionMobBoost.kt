package com.tinyyana.lycohism.expedition

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Keys
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * v0.8.1 #2 — these late-game expedition worlds are "boss-tier" dimensions, so every naturally
 * spawned creature in them is reinforced (more health, harder hits, a touch faster and bigger).
 * Config-driven via `expedition-mob-boost`. Lycohism's own spawns (hazard mobs, the eclipse boss
 * and its adds) come in as CUSTOM and are skipped — they carry their own tuning.
 */
class ExpeditionMobBoost(private val plugin: Lycohism) : Listener {

    private var worlds: Set<String> = emptySet()
    private var healthMult = 2.0
    private var speedMult = 1.1
    private var scale = 1.15
    private var strength = 1

    init { load() }

    fun load() {
        val cfg = plugin.config.getConfigurationSection("expedition-mob-boost")
        worlds = (cfg?.getStringList("worlds") ?: DEFAULT_WORLDS).toSet()
        healthMult = (cfg?.getDouble("health-mult", 2.0) ?: 2.0).coerceIn(1.0, 10.0)
        speedMult = (cfg?.getDouble("speed-mult", 1.1) ?: 1.1).coerceIn(1.0, 2.0)
        scale = (cfg?.getDouble("scale", 1.15) ?: 1.15).coerceIn(0.5, 3.0)
        strength = (cfg?.getInt("strength-amplifier", 1) ?: 1).coerceIn(0, 4)
    }

    @EventHandler(ignoreCancelled = true)
    fun onSpawn(event: CreatureSpawnEvent) {
        if (event.location.world?.name !in worlds) return
        if (event.spawnReason !in BOOSTED_REASONS) return // skip CUSTOM/egg/breeding (our spawns & tame mobs)
        val entity = event.entity
        if (entity is Player) return
        if (entity.persistentDataContainer.has(Keys.bossTag)) return // never re-buff the boss / its adds
        boost(entity)
    }

    private fun boost(entity: LivingEntity) {
        entity.getAttribute(Attribute.MAX_HEALTH)?.let {
            it.baseValue *= healthMult
            entity.health = it.value
        }
        entity.getAttribute(Attribute.MOVEMENT_SPEED)?.let { it.baseValue *= speedMult }
        if (strength > 0) {
            entity.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, strength - 1, false, false, false))
        }
        if (scale != 1.0) applyScale(entity, scale)
    }

    /** Sets body scale via the generic.scale attribute; no-op on versions that lack it. */
    private fun applyScale(entity: LivingEntity, value: Double) {
        runCatching {
            val attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("scale")) ?: return
            val instance = entity.getAttribute(attribute) ?: run {
                entity.registerAttribute(attribute)
                entity.getAttribute(attribute)
            }
            instance?.baseValue = value
        }.onFailure { plugin.debugLog("Could not scale boosted mob: ${it.message}") }
    }

    private companion object {
        val DEFAULT_WORLDS = listOf("lycohism_moonless_waste", "lycohism_eclipse_realm", "lycohism_tidal_depths")
        val BOOSTED_REASONS = setOf(
            CreatureSpawnEvent.SpawnReason.NATURAL,
            CreatureSpawnEvent.SpawnReason.SPAWNER,
            CreatureSpawnEvent.SpawnReason.REINFORCEMENTS,
            CreatureSpawnEvent.SpawnReason.PATROL,
            CreatureSpawnEvent.SpawnReason.JOCKEY,
            CreatureSpawnEvent.SpawnReason.RAID,
            CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION,
        )
    }
}
