package com.tinyyana.lycohism

import com.tinyyana.lycohism.gui.Menu
import com.tinyyana.lycohism.energy.towerCanSeeSky
import com.tinyyana.lycohism.energy.NexusManager
import com.tinyyana.lycohism.energy.networkReaches
import com.tinyyana.lycohism.energy.reachableNetworkNodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MenuLogicTest {

    @Test
    fun `back button follows the last interactive content row`() {
        assertEquals(22, Menu.backSlotAfter(listOf(10, 16)))
        assertEquals(31, Menu.backSlotAfter(listOf(10, 25)))
        assertEquals(49, Menu.backSlotAfter(listOf(10, 43)))
    }

    @Test
    fun `tower sky check allows its own end rod but rejects a roof`() {
        assertEquals(true, towerCanSeeSky(crownY = 15, highestBlockingY = 17))
        assertEquals(false, towerCanSeeSky(crownY = 15, highestBlockingY = 18))
    }

    @Test
    fun `tower network supports direct and relay paths`() {
        val nexus = NexusManager.Node("world", 0, 64, 0)
        val directTower = NexusManager.Node("world", 48, 79, 0)
        assertTrue(networkReaches(reachableNetworkNodes(nexus, emptyList(), 48.0), directTower, 48.0))

        val distantTower = NexusManager.Node("world", 96, 79, 0)
        assertFalse(networkReaches(reachableNetworkNodes(nexus, emptyList(), 48.0), distantTower, 48.0))
        val withRelay = reachableNetworkNodes(nexus, listOf(NexusManager.Node("world", 48, 64, 0)), 48.0)
        assertTrue(networkReaches(withRelay, distantTower, 48.0))
    }
}
