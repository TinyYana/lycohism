package com.tinyyana.lycohism.util

import org.bukkit.Material

/** Client-translated vanilla item names for GUI and chat text. */
object VanillaItems {

    /**
     * Returns the display name for [material] in the active language.
     * Looks up `vanilla-names.<id>` in the loaded lang file first;
     * falls back to title-case of the enum name so unknown materials stay readable.
     */
    fun tag(material: Material): String {
        val key = material.name.lowercase()
        val fromLang = Texts.line("vanilla-names.$key", "")
        if (fromLang.isNotEmpty()) return fromLang
        return key.replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    }
}
