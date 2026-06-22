package com.tinyyana.lycohism.gui

import org.bukkit.inventory.Inventory

/**
 * Tags an inventory as a 溫室 menu so clicks can be identified and routed.
 * [upgraded] = opened from a complete 升級溫室 structure (or via command); gates Lv2 content (v0.7.4 #3).
 */
class GreenhouseHolder(val menu: GreenhouseMenu, val upgraded: Boolean = true) : BackedHolder {
    override lateinit var backing: Inventory
}

enum class GreenhouseMenu { MAIN, CULTIVATE, TOOLS }
