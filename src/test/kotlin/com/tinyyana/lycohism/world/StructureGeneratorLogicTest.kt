package com.tinyyana.lycohism.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StructureGeneratorLogicTest {

    @Test
    fun `chunk roll is bounded and deterministic`() {
        assertFalse(StructureRoll.shouldGenerate(42L, 3, -7, 0.0))
        assertTrue(StructureRoll.shouldGenerate(42L, 3, -7, 1.0))
        assertEquals(
            StructureRoll.shouldGenerate(42L, 3, -7, 0.25),
            StructureRoll.shouldGenerate(42L, 3, -7, 0.25),
        )
    }
}
