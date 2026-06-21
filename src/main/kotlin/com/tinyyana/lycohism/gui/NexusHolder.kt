package com.tinyyana.lycohism.gui

import org.bukkit.inventory.Inventory

/** Read-only 輝能核心 view: shows stored 日輝/月輝 and owner/members. Clicks are cancelled. */
class NexusHolder : BackedHolder {
    override lateinit var backing: Inventory
}
