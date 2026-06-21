package com.tinyyana.lycohism.expedition

import org.bukkit.block.Biome
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.WorldInfo

/**
 * Forces an expedition world onto a chosen set of biomes while keeping vanilla terrain and
 * decoration. This is what gives 雨後森林 its atmosphere — mangrove/swamp foliage, wet water
 * colour, fog and mob spawns — without shipping a datapack or a full custom terrain generator.
 *
 * Biomes are assigned by a jittered-Voronoi scheme: the world is divided into cells, each cell
 * gets a pseudo-random "site" jittered inside it, and every column takes the biome of its
 * nearest site. Because the sites are offset rather than grid-aligned, the borders curve and
 * meander instead of forming the axis-aligned square patches a plain floorDiv would give —
 * regions still read as large coherent areas, but the seams look organic. Single-biome lists
 * stay uniform and skip the search entirely.
 */
class RainfallBiomeProvider(private val biomes: List<Biome>) : BiomeProvider() {

    init {
        require(biomes.isNotEmpty()) { "RainfallBiomeProvider needs at least one biome." }
    }

    override fun getBiome(worldInfo: WorldInfo, x: Int, y: Int, z: Int): Biome {
        if (biomes.size == 1) return biomes[0]

        val cellX = Math.floorDiv(x, CELL_SIZE)
        val cellZ = Math.floorDiv(z, CELL_SIZE)

        var bestCell = cellX to cellZ
        var bestDistSq = Long.MAX_VALUE
        // Search the 3x3 block of cells around the point; a jittered site can never be closer
        // than its own cell's neighbours, so this window is enough to find the true nearest.
        for (dz in -1..1) {
            for (dx in -1..1) {
                val cx = cellX + dx
                val cz = cellZ + dz
                val (siteX, siteZ) = site(cx, cz)
                val ddx = (x - siteX).toLong()
                val ddz = (z - siteZ).toLong()
                val distSq = ddx * ddx + ddz * ddz
                if (distSq < bestDistSq) {
                    bestDistSq = distSq
                    bestCell = cx to cz
                }
            }
        }

        val pick = Math.floorMod(hash(bestCell.first, bestCell.second), biomes.size)
        return biomes[pick]
    }

    override fun getBiomes(worldInfo: WorldInfo): List<Biome> = biomes

    /** The jittered world-space site position for a cell, stable per cell. */
    private fun site(cellX: Int, cellZ: Int): Pair<Int, Int> {
        val h = hash(cellX, cellZ)
        // Two independent offsets in [0, CELL_SIZE) keep the site inside its own cell.
        val offX = Math.floorMod(h, CELL_SIZE)
        val offZ = Math.floorMod(h ushr 16, CELL_SIZE)
        return (cellX * CELL_SIZE + offX) to (cellZ * CELL_SIZE + offZ)
    }

    private fun hash(cx: Int, cz: Int): Int {
        var h = cx * 73856093 xor cz * 19349663
        h = h xor (h ushr 13)
        h *= 0x5bd1e995.toInt()
        return h xor (h ushr 15)
    }

    private companion object {
        /** Average region scale in blocks; larger = bigger biome patches. */
        const val CELL_SIZE = 200
    }
}
