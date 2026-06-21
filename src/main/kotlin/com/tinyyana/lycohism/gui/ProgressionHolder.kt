package com.tinyyana.lycohism.gui

import org.bukkit.inventory.Inventory

/** Tags an inventory as the 調律之路 progression screen. Read-only; clicks are just cancelled. */
class ProgressionHolder : BackedHolder {
    override lateinit var backing: Inventory
}
