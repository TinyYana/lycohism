package com.tinyyana.lycohism.util

import org.bukkit.Material

/** Client-translated vanilla item names for GUI and chat text. */
object VanillaItems {

    /** MiniMessage translatable tag resolved using each player's Minecraft language. */
    fun tag(material: Material): String = "<lang:${material.translationKey()}>"
}
