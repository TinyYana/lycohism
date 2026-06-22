package com.tinyyana.lycohism.expedition

import org.bukkit.Material
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import java.util.Random
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Keeps vanilla generation, then gives each expedition its own terrain silhouette and surface. */
class ExpeditionTerrainGenerator(private val style: ExpeditionTerrainStyle) : ChunkGenerator() {

    override fun shouldGenerateNoise() = true
    override fun shouldGenerateSurface() = true
    override fun shouldGenerateCaves() = true
    override fun shouldGenerateDecorations() = true
    override fun shouldGenerateMobs() = true
    override fun shouldGenerateStructures() = true

    override fun generateNoise(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunkData: ChunkData) {
        for (x in 0..15) for (z in 0..15) {
            val top = topSolid(chunkData, x, z) ?: continue
            val delta = terrainDelta(style, worldInfo.seed, (chunkX shl 4) + x, (chunkZ shl 4) + z)
            if (delta > 0) {
                for (y in top + 1..minOf(top + delta, chunkData.maxHeight - 1)) chunkData.setBlock(x, y, z, Material.STONE)
            } else if (delta < 0) {
                for (y in top downTo maxOf(top + delta + 1, chunkData.minHeight)) chunkData.setBlock(x, y, z, Material.AIR)
            }
        }
    }

    override fun generateSurface(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunkData: ChunkData) {
        for (x in 0..15) for (z in 0..15) {
            val top = topSolid(chunkData, x, z) ?: continue
            val wx = (chunkX shl 4) + x
            val wz = (chunkZ shl 4) + z
            val patch = terrainPatch(worldInfo.seed, wx, wz)
            when (style) {
                ExpeditionTerrainStyle.RAINFALL -> {
                    chunkData.setBlock(x, top, z, if (patch > 0.35) Material.MOSS_BLOCK else Material.MUD)
                    if (top > chunkData.minHeight && patch < -0.45) chunkData.setBlock(x, top - 1, z, Material.PACKED_MUD)
                }
                ExpeditionTerrainStyle.MOONLESS -> {
                    chunkData.setBlock(x, top, z, if (patch > 0.2) Material.SCULK else Material.POLISHED_DEEPSLATE)
                    if (top > chunkData.minHeight) chunkData.setBlock(x, top - 1, z, Material.DEEPSLATE)
                }
                ExpeditionTerrainStyle.ECLIPSE -> {
                    // Mostly light-level-0 black rock so the realm stays dark and spawnable; the glowing
                    // crying-obsidian and day-gold / moon-amethyst are rare accent veins, not the floor
                    // (v0.8.2 #2 — a crying-obsidian carpet was too bright for any mob to spawn).
                    val surface = when {
                        patch > 0.9 -> Material.CRYING_OBSIDIAN
                        patch > 0.8 -> Material.GOLD_BLOCK
                        patch > 0.7 -> Material.AMETHYST_BLOCK
                        patch > 0.0 -> Material.BLACKSTONE
                        else -> Material.POLISHED_BLACKSTONE
                    }
                    chunkData.setBlock(x, top, z, surface)
                    if (top > chunkData.minHeight) chunkData.setBlock(x, top - 1, z, Material.CRACKED_DEEPSLATE_BRICKS)
                }
                ExpeditionTerrainStyle.OCEAN -> {
                    // Reefs/islands rising out of vanilla ocean: sand, gravel and prismarine shoals.
                    val surface = when {
                        patch > 0.55 -> Material.PRISMARINE
                        patch > 0.0 -> Material.SAND
                        else -> Material.GRAVEL
                    }
                    chunkData.setBlock(x, top, z, surface)
                    if (top > chunkData.minHeight) chunkData.setBlock(x, top - 1, z, Material.SANDSTONE)
                }
                ExpeditionTerrainStyle.VANILLA -> Unit
            }
            // Underground theming (v0.8.2 #4): the world isn't just a re-skinned surface — scatter
            // style pockets/veins through the rock below it, skipping caves/air so digging down still
            // tells you which expedition you're in.
            if (style != ExpeditionTerrainStyle.VANILLA) {
                val bandTop = top - 3
                val bandBottom = maxOf(chunkData.minHeight + 4, bandTop - UNDERGROUND_DEPTH)
                for (y in bandTop downTo bandBottom) {
                    if (!chunkData.getType(x, y, z).isSolid) continue
                    val themed = undergroundBlock(style, worldInfo.seed, wx, y, wz) ?: continue
                    chunkData.setBlock(x, y, z, themed)
                }
            }
        }
    }

    private fun topSolid(data: ChunkData, x: Int, z: Int): Int? =
        (data.maxHeight - 1 downTo data.minHeight).firstOrNull { data.getType(x, it, z).isSolid }

    private companion object { const val UNDERGROUND_DEPTH = 44 }
}

enum class ExpeditionTerrainStyle { VANILLA, RAINFALL, MOONLESS, ECLIPSE, OCEAN }

internal fun terrainDelta(style: ExpeditionTerrainStyle, seed: Long, x: Int, z: Int): Int {
    if (style == ExpeditionTerrainStyle.VANILLA) return 0
    val phase = (seed xor (seed ushr 32)).toInt()
    val broad = sin((x + phase % 997) / 19.0) + cos((z - phase % 733) / 23.0)
    val cross = sin((x + z + phase % 431) / 41.0) * 0.8
    return when (style) {
        ExpeditionTerrainStyle.RAINFALL -> ((broad + cross) * 1.6).roundToInt().coerceIn(-2, 4)
        ExpeditionTerrainStyle.MOONLESS -> (((broad - cross) * 1.8).roundToInt() / 2 * 2).coerceIn(-4, 6)
        // 暮蝕之境: a ridged silhouette — narrow spires where ridges fold, deep rifts between them.
        // Distinct profile from the永夜荒原's gentle stepped flats (item v0.8.1 #1).
        ExpeditionTerrainStyle.ECLIPSE -> {
            val ridge = kotlin.math.abs(sin((x + phase % 311) / 11.0) + sin((z - phase % 509) / 13.0))
            val spire = if (ridge > 1.35) ((ridge - 1.35) * 26).roundToInt() else 0
            val rift = if (cross < -0.55) -6 else 0
            (spire + rift).coerceIn(-9, 16)
        }
        // Ocean: only ever raise reefs/seamounts above the vanilla seabed — never carve (carving
        // below sea level would leave air pockets under the water).
        ExpeditionTerrainStyle.OCEAN -> ((broad + cross) * 2.0).roundToInt().coerceIn(0, 8)
        ExpeditionTerrainStyle.VANILLA -> 0
    }
}

private fun terrainPatch(seed: Long, x: Int, z: Int): Double {
    var h = seed xor (x.toLong() * 341873128712L) xor (z.toLong() * 132897987541L)
    h = (h xor (h ushr 33)) * -49064778989728563L
    h = (h xor (h ushr 33)) * -4265267296055464877L
    return ((h xor (h ushr 33)) and 0xffffL) / 32767.5 - 1.0
}

/** Probability that a given underground block is replaced with a themed one (the rest stay vanilla). */
private const val THEMED_DENSITY = 0.12

/** A themed pocket block for [style] at this position, or null to leave the vanilla rock. */
internal fun undergroundBlock(style: ExpeditionTerrainStyle, seed: Long, x: Int, y: Int, z: Int): Material? {
    if (deepHash(seed, x, y, z) > THEMED_DENSITY) return null
    val pick = deepHash(seed xor 0x9E3779B97F4A7C15uL.toLong(), x, y, z)
    return when (style) {
        ExpeditionTerrainStyle.RAINFALL -> when {
            pick < 0.5 -> Material.MUD
            pick < 0.85 -> Material.CLAY
            else -> Material.MOSS_BLOCK
        }
        ExpeditionTerrainStyle.MOONLESS -> when {
            pick < 0.6 -> Material.SCULK
            pick < 0.9 -> Material.COBBLED_DEEPSLATE
            pick < 0.97 -> Material.AMETHYST_BLOCK
            else -> Material.SCULK_CATALYST
        }
        ExpeditionTerrainStyle.ECLIPSE -> when {
            pick < 0.55 -> Material.BLACKSTONE
            pick < 0.85 -> Material.AMETHYST_BLOCK
            pick < 0.97 -> Material.BUDDING_AMETHYST
            else -> Material.CRYING_OBSIDIAN
        }
        ExpeditionTerrainStyle.OCEAN -> when {
            pick < 0.5 -> Material.PRISMARINE
            pick < 0.8 -> Material.DARK_PRISMARINE
            pick < 0.95 -> Material.PRISMARINE_BRICKS
            else -> Material.SEA_LANTERN
        }
        ExpeditionTerrainStyle.VANILLA -> null
    }
}

/** Deterministic 3D hash in 0..1, used for underground pocket placement. */
private fun deepHash(seed: Long, x: Int, y: Int, z: Int): Double {
    var h = seed xor (x.toLong() * 341873128712L) xor (y.toLong() * 132897987541L) xor (z.toLong() * 1099511628211L)
    h = (h xor (h ushr 33)) * -49064778989728563L
    h = (h xor (h ushr 33)) * -4265267296055464877L
    return ((h xor (h ushr 33)) and 0xffffL) / 65535.0
}
