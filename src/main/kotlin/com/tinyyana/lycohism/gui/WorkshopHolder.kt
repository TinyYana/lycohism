package com.tinyyana.lycohism.gui

import org.bukkit.inventory.Inventory

/** Tags an inventory as a Lycohism workshop menu so clicks can be identified and routed. */
class WorkshopHolder(val menu: WorkshopMenu) : BackedHolder {
    override lateinit var backing: Inventory
}

enum class WorkshopMenu { MAIN, TOOLS, MATERIALS }
