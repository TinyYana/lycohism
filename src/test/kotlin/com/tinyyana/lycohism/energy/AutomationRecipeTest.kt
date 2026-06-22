package com.tinyyana.lycohism.energy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers the 自動調律機 recipe string parsing. Uses the Bukkit-free [parseRecipeSpec] seam so it runs
 * without the Material registry (paper-api is compileOnly, not on the test runtime classpath).
 */
class AutomationRecipeTest {

    @Test
    fun `parses a full recipe token`() {
        val spec = parseRecipeSpec("COBBLESTONE>STONE:2;MOON;7")
        assertEquals(RecipeSpec("COBBLESTONE", "STONE", 2, EnergyType.MOON, 7), spec)
    }

    @Test
    fun `amount defaults to one and trims whitespace`() {
        assertEquals(RecipeSpec("STONE", "STONE_BRICKS", 1, EnergyType.SUN, 4), parseRecipeSpec(" STONE > STONE_BRICKS ; sun ; 4 "))
    }

    @Test
    fun `amount and cost are clamped to sane minimums`() {
        val spec = parseRecipeSpec("SAND>SANDSTONE:0;SUN;-3")!!
        assertEquals(1, spec.amount) // amount coerced to at least 1
        assertEquals(0, spec.cost)   // cost coerced to at least 0
    }

    @Test
    fun `malformed tokens are rejected`() {
        assertNull(parseRecipeSpec("COBBLESTONE>STONE;SUN"))        // missing cost field
        assertNull(parseRecipeSpec("COBBLESTONE;SUN;4"))            // no > separator
        assertNull(parseRecipeSpec("COBBLESTONE>STONE:1;DUSK;4"))   // unknown energy type
        assertNull(parseRecipeSpec("COBBLESTONE>STONE:1;SUN;x"))    // non-numeric cost
        assertNull(parseRecipeSpec(">STONE:1;SUN;4"))               // empty input
    }
}
