package com.tinyyana.lycohism.expedition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpeditionTerrainLogicTest {
    @Test
    fun `terrain shaping is deterministic and bounded`() {
        val first = terrainDelta(ExpeditionTerrainStyle.RAINFALL, 42L, 120, -80)
        assertEquals(first, terrainDelta(ExpeditionTerrainStyle.RAINFALL, 42L, 120, -80))
        assertTrue(first in -2..4)
        assertTrue(terrainDelta(ExpeditionTerrainStyle.MOONLESS, 42L, 120, -80) in -4..6)
        assertEquals(0, terrainDelta(ExpeditionTerrainStyle.VANILLA, 42L, 120, -80))
    }
}
