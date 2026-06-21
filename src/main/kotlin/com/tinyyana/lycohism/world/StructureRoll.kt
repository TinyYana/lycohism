package com.tinyyana.lycohism.world

import java.util.Random

internal object StructureRoll {
    fun shouldGenerate(worldSeed: Long, chunkX: Int, chunkZ: Int, chance: Double): Boolean =
        chance > 0.0 && (chance >= 1.0 || Random(seed(worldSeed, chunkX, chunkZ)).nextDouble() < chance)

    fun seed(worldSeed: Long, chunkX: Int, chunkZ: Int): Long =
        worldSeed xor (chunkX.toLong() * 341873128712L) xor (chunkZ.toLong() * 132897987541L)
}
