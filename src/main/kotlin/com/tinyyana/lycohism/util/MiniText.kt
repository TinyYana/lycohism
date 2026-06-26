package com.tinyyana.lycohism.util

/**
 * Minimal MiniMessage→§ converter for Spigot compatibility (1.16+).
 * Supports colour/format tags, gradient with 2+ stops, and legacy & codes.
 *
 * Gradients emit §x§R§R§G§G§B§B before every printable character (Spigot 1.16+ hex format).
 * Click/hover events are not supported — those need Adventure/Paper.
 * Bukkit-free so the whole object stays unit-testable.
 * ponytail: §-legacy + hex; upgrade to adventure-text-minimessage if Paper-only becomes viable.
 */
object MiniText {

    private val TAGS: Map<String, String> = mapOf(
        "black"         to "§0", "dark_blue"    to "§1", "dark_green"  to "§2",
        "dark_aqua"     to "§3", "dark_red"     to "§4", "dark_purple" to "§5",
        "gold"          to "§6", "gray"         to "§7", "dark_gray"   to "§8",
        "blue"          to "§9", "green"        to "§a", "aqua"        to "§b",
        "red"           to "§c", "light_purple" to "§d", "yellow"      to "§e",
        "white"         to "§f",
        "bold"          to "§l", "italic"       to "§o", "underlined"  to "§n",
        "strikethrough" to "§m", "obfuscated"   to "§k",
        "reset"         to "§r",
    )

    // Named colour → RGB int (matches Minecraft's named colour hex values)
    private val NAMED_HEX: Map<String, Int> = mapOf(
        "black"        to 0x000000, "dark_blue"    to 0x0000AA, "dark_green"  to 0x00AA00,
        "dark_aqua"    to 0x00AAAA, "dark_red"     to 0xAA0000, "dark_purple" to 0xAA00AA,
        "gold"         to 0xFFAA00, "gray"         to 0xAAAAAA, "dark_gray"   to 0x555555,
        "blue"         to 0x5555FF, "green"        to 0x55FF55, "aqua"        to 0x55FFFF,
        "red"          to 0xFF5555, "light_purple" to 0xFF55FF, "yellow"      to 0xFFFF55,
        "white"        to 0xFFFFFF,
    )

    // <gradient:stop1:stop2[:stop3...]>text</gradient>
    private val GRADIENT_RE = Regex("""<gradient:([^>]+)>(.*?)</gradient>""", RegexOption.DOT_MATCHES_ALL)
    private val CLOSING = Regex("</[^>]+>")
    private val LEGACY = Regex("&([0-9a-fk-or])", RegexOption.IGNORE_CASE)

    fun parse(text: String): String {
        var s = GRADIENT_RE.replace(text) { m ->
            val stops = m.groupValues[1].split(":").mapNotNull { resolveHex(it.trim()) }
            val inner = m.groupValues[2]
            if (stops.size < 2) inner else applyGradient(inner, stops)
        }
        s = CLOSING.replace(s, "§r")
        for ((tag, code) in TAGS) s = s.replace("<$tag>", code)
        // ponytail: inline the & → § translation so this stays Bukkit-free and testable
        return LEGACY.replace(s) { "§${it.groupValues[1]}" }
    }

    // ── internals ────────────────────────────────────────────────────────────

    private fun resolveHex(color: String): Int? {
        NAMED_HEX[color.lowercase()]?.let { return it }
        val hex = color.removePrefix("#")
        return if (hex.length == 6) hex.toIntOrNull(16) else null
    }

    /** Encodes an RGB int as Spigot's §x§R§R§G§G§B§B hex colour escape. */
    private fun spigotHex(rgb: Int): String {
        val hex = "%06X".format(rgb)
        return "§x" + hex.map { "§$it" }.joinToString("")
    }

    private fun lerpColor(a: Int, b: Int, t: Float): Int {
        val r = ((a shr 16 and 0xFF) + t * ((b shr 16 and 0xFF) - (a shr 16 and 0xFF))).toInt()
        val g = ((a shr  8 and 0xFF) + t * ((b shr  8 and 0xFF) - (a shr  8 and 0xFF))).toInt()
        val bl = ((a and 0xFF) + t * ((b and 0xFF) - (a and 0xFF))).toInt()
        return (r shl 16) or (g shl 8) or bl
    }

    /**
     * Applies a multi-stop gradient across the printable characters in [text].
     * Existing §-codes in [text] are preserved and skipped during colour assignment.
     */
    private fun applyGradient(text: String, stops: List<Int>): String {
        // Count printable chars (skip § escapes)
        var printable = 0
        var i = 0
        while (i < text.length) {
            if (text[i] == '§' && i + 1 < text.length) { i += 2; continue }
            printable++; i++
        }

        val sb = StringBuilder()
        var charIdx = 0
        i = 0
        while (i < text.length) {
            if (text[i] == '§' && i + 1 < text.length) {
                sb.append(text[i]).append(text[i + 1])
                i += 2; continue
            }
            val t = if (printable <= 1) 0f else charIdx.toFloat() / (printable - 1)
            val seg = (t * (stops.size - 1)).toInt().coerceAtMost(stops.size - 2)
            val localT = t * (stops.size - 1) - seg
            sb.append(spigotHex(lerpColor(stops[seg], stops[seg + 1], localT)))
            sb.append(text[i])
            charIdx++; i++
        }
        return sb.toString()
    }
}
