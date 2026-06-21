package com.tinyyana.lycohism.gui

import org.bukkit.inventory.Inventory

/** Tags an inventory as a 書房 menu and remembers which view it is, so clicks route correctly. */
class StudyHolder(val menu: StudyMenu) : BackedHolder {
    override lateinit var backing: Inventory
}

/**
 * The 書房 views. The root is a small set of categories so the facility can keep gaining books
 * and tools without overflowing one flat screen; each leaf holds the items of one category.
 */
enum class StudyMenu { MAIN, BOOKS, TOOLS, EXPEDITION }
