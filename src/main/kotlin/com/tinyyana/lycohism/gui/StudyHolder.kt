package com.tinyyana.lycohism.gui

import org.bukkit.inventory.Inventory

/**
 * Tags an inventory as a 書房 menu and remembers which view it is, so clicks route correctly.
 * [upgraded] = opened from a complete 升級書房 structure (or via command); gates Lv2 content (v0.7.4 #3).
 */
class StudyHolder(val menu: StudyMenu, val upgraded: Boolean = true) : BackedHolder {
    override lateinit var backing: Inventory
}

/**
 * The 書房 views. The root is a small set of categories so the facility can keep gaining books
 * and tools without overflowing one flat screen; each leaf holds the items of one category.
 */
enum class StudyMenu { MAIN, BOOKS, TOOLS, EXPEDITION }
