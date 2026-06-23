package com.tinyyana.lycohism.puzzle

import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests the Bukkit-free unlock predicate extracted from SealManager. */
class SealManagerTest {

    @Test
    fun `correct discovery and item yields UNLOCKED`() {
        val result = SealManager.canUnlock(
            setOf("energy_nexus"), "energy_crystal", "energy_nexus", "energy_crystal",
        )
        assertEquals(SealManager.UnlockResult.UNLOCKED, result)
    }

    @Test
    fun `missing discovery returns MISSING_DISCOVERY even when item correct`() {
        val result = SealManager.canUnlock(
            emptySet(), "energy_crystal", "energy_nexus", "energy_crystal",
        )
        assertEquals(SealManager.UnlockResult.MISSING_DISCOVERY, result)
    }

    @Test
    fun `wrong item returns MISSING_ITEM when discovery present`() {
        val result = SealManager.canUnlock(
            setOf("energy_nexus"), "wrong_item", "energy_nexus", "energy_crystal",
        )
        assertEquals(SealManager.UnlockResult.MISSING_ITEM, result)
    }

    @Test
    fun `both missing returns MISSING_DISCOVERY (discovery checked first)`() {
        val result = SealManager.canUnlock(emptySet(), "", "energy_nexus", "energy_crystal")
        assertEquals(SealManager.UnlockResult.MISSING_DISCOVERY, result)
    }
}
