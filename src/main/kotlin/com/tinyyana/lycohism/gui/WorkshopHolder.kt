package com.tinyyana.lycohism.gui

import org.bukkit.inventory.Inventory

/**
 * Tags an inventory as a Lycohism workshop menu so clicks can be identified and routed.
 * [upgraded] = opened from a complete 升級工房 structure (or via command); when false, Lv2 content
 * is hidden even if the player has unlocked it (v0.7.4 #3: Lv2 access is tied to the structure).
 */
class WorkshopHolder(val menu: WorkshopMenu, val upgraded: Boolean = true) : BackedHolder {
    override lateinit var backing: Inventory
}

enum class WorkshopMenu { MAIN, TOOLS, MATERIALS }
