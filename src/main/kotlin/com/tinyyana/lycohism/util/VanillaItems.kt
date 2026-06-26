package com.tinyyana.lycohism.util

import org.bukkit.Material

/** Client-translated vanilla item names for GUI and chat text. */
object VanillaItems {

    /**
     * Returns the display name for [material] in [lang] ("zh" or "en").
     * Looks up `vanilla-names.<id>` from the lang file first; falls back to
     * title-case of the enum name so unlisted materials remain readable.
     */
    fun tag(material: Material, lang: String = ""): String {
        val resolvedLang = lang.ifEmpty { Texts.activeLanguage }
        val key = material.name.lowercase()
        val fromLang = Texts.lineInLang("vanilla-names.$key", resolvedLang, "")
        return fromLang.ifEmpty {
            key.replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        }
    }
}
