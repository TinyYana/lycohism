package com.tinyyana.lycohism.gui

import org.bukkit.inventory.Inventory

/** Tags an inventory as a 溫室 menu so clicks can be identified and routed. */
class GreenhouseHolder(val menu: GreenhouseMenu) : BackedHolder {
    override lateinit var backing: Inventory
}

enum class GreenhouseMenu { MAIN, CULTIVATE, TOOLS }
