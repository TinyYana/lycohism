package com.tinyyana.lycohism.expedition

import org.bukkit.block.Biome
import org.bukkit.entity.EntityType
import org.bukkit.potion.PotionEffectType

/**
 * A single expedition destination, loaded from expeditions.yml.
 *
 * v0.5 introduces the first one (雨後森林): a separate, persistent world the player reaches
 * from the main world, collects a themed material in, and returns from. Data-driven so later
 * expeditions need only a YAML entry plus their own gate item / material.
 */
data class Expedition(
    val id: String,
    /** MiniMessage display name, used in messages. */
    val displayName: String,
    /** Bukkit world name; created lazily on first entry and kept on disk afterwards. */
    val worldName: String,
    /** When true the world is locked to permanent rain for the "雨後" atmosphere. */
    val alwaysRaining: Boolean,
    /** When true the world is locked to permanent night — the 永夜荒原's defining trait and the
     * practical reason to go: the only place 月輝 charges around the clock. */
    val alwaysNight: Boolean,
    /** Passive 輝能 accrual multiplier in this world (1.0 = normal). 永夜荒原 doubles 月輝 to make
     * the trip worth it. */
    val energyMultiplier: Double,
    /** Biomes to force on the world for atmosphere; empty keeps vanilla biome placement. */
    val biomes: List<Biome>,
    /**
     * Vanilla advancements the player must have completed before they may enter (or craft the
     * gate to) this expedition. Empty means no gate. Pushes the expedition later in progression
     * and ties it to milestones like entering the Nether. See util/Advancements.
     */
    val requiresAdvancements: Set<String>,
    /** Optional light ambient threat for the world; null disables themed spawns. */
    val hazard: ExpeditionHazard?,
    /** Shown once, the first time a player sets foot in the expedition. */
    val firstHint: String,
)

/**
 * A light, atmospheric hazard for an expedition world: occasional themed mobs so the world
 * does not feel completely safe. Deliberately gentle (night-gated, capped, low chance) — the
 * first expedition leans exploration, not a high-pressure dungeon (PROJECT_PLAN §19.5).
 */
data class ExpeditionHazard(
    /** Vanilla entity type spawned as the themed mob. */
    val mobType: EntityType,
    /** lang.yml key for the mob's display name (e.g. content-names.rain_wraith). */
    val displayNameKey: String,
    /** Probability per scan cycle, per eligible player, that a mob is spawned. */
    val chance: Double,
    /** Don't spawn more than this many tagged hazard mobs already near the player. */
    val maxNearby: Int,
    /** Horizontal radius (blocks) around the player to spawn / count within. */
    val radius: Int,
    /** When true, only spawn at night so the world stays calm by day. */
    val nightOnly: Boolean,
    /** Permanent potion effects applied on spawn (type -> amplifier) — fiercer themed mobs. */
    val effects: Map<PotionEffectType, Int>,
    /** Body scale via the generic.scale attribute; 1.0 = vanilla. >1 bigger, <1 smaller. */
    val scale: Double,
)
