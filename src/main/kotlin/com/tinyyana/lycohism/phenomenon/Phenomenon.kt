package com.tinyyana.lycohism.phenomenon

import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.EntityType

/**
 * A single natural phenomenon definition, loaded from phenomena.yml.
 *
 * A phenomenon is obtained by performing a vanilla action (breaking a source block)
 * while time / weather / world conditions are met, hooking Lycohism onto normal play.
 */
data class Phenomenon(
    val id: String,
    /** MiniMessage display name of the dropped item. */
    val displayName: String,
    /** Vanilla material used as the item's appearance (identity is via PDC tag). */
    val baseMaterial: Material,
    /** MiniMessage lore lines for the dropped item. */
    val lore: List<String>,
    /** Breaking any of these blocks can yield the phenomenon. */
    val sourceBlocks: Set<Material>,
    /** Time-of-day window in ticks (0..24000); wraps midnight when start > end. */
    val timeStart: Long,
    val timeEnd: Long,
    /** Optional vertical range; defaults allow the full world height. */
    val minY: Int,
    val maxY: Int,
    /** Optional Minecraft moon phases (0 = full moon); empty means any phase. */
    val moonPhases: Set<Int>,
    val weather: WeatherCondition,
    val environment: World.Environment,
    /** Drop chance per matching block break, 0.0..1.0. */
    val chance: Double,
    /** Shown once, the first time a player collects this phenomenon. */
    val firstHint: String,
    /** Optional vanilla advancement keys the player must have completed; empty means no gate. */
    val requiresAdvancements: Set<String>,
    /** Optional world-name allow-list; empty means any world. Used to keep expedition-only materials in their world. */
    val worlds: Set<String>,
    /** Optional mob-kill drops: entity type -> drop chance (0..1). Gated by [requiresAdvancements]. */
    val mobDrops: Map<EntityType, Double>,
)

enum class WeatherCondition { ANY, CLEAR, RAIN, THUNDER }
