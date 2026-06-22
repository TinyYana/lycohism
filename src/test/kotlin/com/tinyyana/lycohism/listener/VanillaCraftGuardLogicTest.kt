package com.tinyyana.lycohism.listener

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VanillaCraftGuardLogicTest {

    @Test
    fun `confirmation matches only the same unexpired recipe`() {
        assertTrue(confirmationMatches("recipe:item" to 20L, "recipe:item", 20L))
        assertFalse(confirmationMatches("recipe:item" to 19L, "recipe:item", 20L))
        assertFalse(confirmationMatches("recipe:item" to 20L, "other:item", 20L))
    }
}
