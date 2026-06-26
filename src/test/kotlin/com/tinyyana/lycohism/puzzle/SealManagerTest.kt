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

    // ---- shouldProtectBreak -------------------------------------------------

    private val controllers = setOf("world,0,64,0")
    private val chestToController = mapOf("world,0,65,0" to "world,0,64,0")

    @Test
    fun `unrelated block is never protected`() {
        assert(!SealManager.shouldProtectBreak(controllers, chestToController, emptySet(), "world,99,64,99"))
    }

    @Test
    fun `controller is protected when shrine not yet unlocked`() {
        assert(SealManager.shouldProtectBreak(controllers, chestToController, emptySet(), "world,0,64,0"))
    }

    @Test
    fun `chest is protected when shrine not yet unlocked`() {
        assert(SealManager.shouldProtectBreak(controllers, chestToController, emptySet(), "world,0,65,0"))
    }

    @Test
    fun `controller is NOT protected after player unlocks the shrine`() {
        val unsealed = setOf("world,0,64,0")
        assert(!SealManager.shouldProtectBreak(controllers, chestToController, unsealed, "world,0,64,0"))
    }

    @Test
    fun `chest is NOT protected after player unlocks the shrine`() {
        val unsealed = setOf("world,0,64,0")
        assert(!SealManager.shouldProtectBreak(controllers, chestToController, unsealed, "world,0,65,0"))
    }
}
