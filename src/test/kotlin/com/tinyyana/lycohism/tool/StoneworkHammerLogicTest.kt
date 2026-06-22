package com.tinyyana.lycohism.tool

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StoneworkHammerLogicTest {
    @Test
    fun `tier two hammer selects one three by three plane`() {
        val floor = hammerPlaneOffsets("UP")
        assertEquals(9, floor.distinct().size)
        assertTrue(floor.all { it.second == 0 })

        val wall = hammerPlaneOffsets("NORTH")
        assertEquals(9, wall.distinct().size)
        assertTrue(wall.all { it.third == 0 })
    }

    @Test
    fun `tier two wand modes produce their advertised shapes`() {
        assertEquals(8, wandOffsets("LINE", "EAST", 8).distinct().size)
        assertEquals(9, wandOffsets("WALL", "NORTH", 8).distinct().size)
        assertEquals(25, wandOffsets("FLOOR", "UP", 8).distinct().size)
        assertEquals(8, wandOffsets("COLUMN", "UP", 8).distinct().size)
    }
}
