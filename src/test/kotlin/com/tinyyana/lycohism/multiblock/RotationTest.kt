package com.tinyyana.lycohism.multiblock

import kotlin.test.Test
import kotlin.test.assertEquals

class RotationTest {

    @Test
    fun `none is identity`() {
        assertEquals(3 to -7, Rotation.NONE.rotate(3, -7))
    }

    @Test
    fun `four quarter turns return to the original offset`() {
        var point = 2 to 5
        repeat(4) {
            val (x, z) = point
            point = Rotation.CW90.rotate(x, z)
        }
        assertEquals(2 to 5, point)
    }

    @Test
    fun `cw180 equals two cw90 turns`() {
        val (x1, z1) = Rotation.CW90.rotate(2, 5)
        assertEquals(Rotation.CW180.rotate(2, 5), Rotation.CW90.rotate(x1, z1))
    }

    @Test
    fun `cw90 and cw270 are inverses`() {
        val (x, z) = Rotation.CW90.rotate(4, -1)
        assertEquals(4 to -1, Rotation.CW270.rotate(x, z))
    }
}
