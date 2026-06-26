package com.tinyyana.lycohism.util

import org.bukkit.Material

/** Client-translated vanilla item names for GUI and chat text. */
object VanillaItems {

    /** Human-readable material name formatted as title case (e.g. IRON_INGOT → "Iron Ingot"). */
    fun tag(material: Material): String =
        material.name.lowercase().replace('_', ' ')
            .split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}
