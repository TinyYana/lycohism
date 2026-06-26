package com.tinyyana.lycohism.util

/**
 * Minimal MiniMessage→§ converter for Spigot compatibility (1.16+).
 *
 * Supports:
 *   - Named colour tags <aqua>, <gold>, … and format tags <bold>, <italic>, …
 *   - Proper closing tags: </colorname> pops the colour stack and re-emits the
 *     parent colour (so <gray>text <white>hi</white> text</gray> stays gray
 *     after </white>). Closing a format tag or unknown tag emits §r.
 *   - <reset> / </reset> → §r and clears the colour stack.
 *   - Gradient: <gradient:stop1:stop2[:stop3…]>text</gradient> — multi-stop
 *     linear interpolation; emits Spigot 1.16+ §x§R§R§G§G§B§B per character.
 *   - Legacy &x codes — backward compat with existing stored values.
 *
 * Bukkit-free so the whole object stays unit-testable.
 * ponytail: §-legacy + hex; upgrade to adventure-text-minimessage if Paper-only becomes viable.
 */
object MiniText {

    // ── tag → §code tables ──────────────────────────────────────────────────

    val COLOR_TAGS: Map<String, String> = mapOf(
        "black"         to "§0", "dark_blue"    to "§1", "dark_green"  to "§2",
        "dark_aqua"     to "§3", "dark_red"     to "§4", "dark_purple" to "§5",
        "gold"          to "§6", "gray"         to "§7", "dark_gray"   to "§8",
        "blue"          to "§9", "green"        to "§a", "aqua"        to "§b",
        "red"           to "§c", "light_purple" to "§d", "yellow"      to "§e",
        "white"         to "§f",
    )

    private val FORMAT_TAGS: Map<String, String> = mapOf(
        "bold"          to "§l", "italic"       to "§o", "underlined"  to "§n",
        "strikethrough" to "§m", "obfuscated"   to "§k",
    )

    private val ALL_TAGS = COLOR_TAGS + FORMAT_TAGS

    // Named colour → RGB int (matches Minecraft's palette)
    private val NAMED_HEX: Map<String, Int> = mapOf(
        "black"        to 0x000000, "dark_blue"    to 0x0000AA, "dark_green"  to 0x00AA00,
        "dark_aqua"    to 0x00AAAA, "dark_red"     to 0xAA0000, "dark_purple" to 0xAA00AA,
        "gold"         to 0xFFAA00, "gray"         to 0xAAAAAA, "dark_gray"   to 0x555555,
        "blue"         to 0x5555FF, "green"        to 0x55FF55, "aqua"        to 0x55FFFF,
        "red"          to 0xFF5555, "light_purple" to 0xFF55FF, "yellow"      to 0xFFFF55,
        "white"        to 0xFFFFFF,
    )

    private val GRADIENT_RE = Regex("""<gradient:([^>]+)>(.*?)</gradient>""", RegexOption.DOT_MATCHES_ALL)
    private val LEGACY = Regex("&([0-9a-fk-or])", RegexOption.IGNORE_CASE)

    // ── public API ──────────────────────────────────────────────────────────

    fun parse(text: String): String {
        // 1. Expand gradient regions (they contain no nested MiniMessage tags in practice)
        var s = GRADIENT_RE.replace(text) { m ->
            val stops = m.groupValues[1].split(":").mapNotNull { resolveHex(it.trim()) }
            val inner = m.groupValues[2]
            if (stops.size < 2) inner else applyGradient(inner, stops)
        }
        // 2. Stack-based tag parsing (colour stack for proper closing-tag restoration)
        s = parseWithStack(s)
        // 3. Legacy & codes
        return LEGACY.replace(s) { "§${it.groupValues[1]}" }
    }

    // ── stack-based tag scanner ─────────────────────────────────────────────

    private fun parseWithStack(text: String): String {
        val sb = StringBuilder()
        val colorStack = ArrayDeque<String>() // §-codes of active colour tags
        var i = 0
        while (i < text.length) {
            if (text[i] != '<') { sb.append(text[i++]); continue }
            val end = text.indexOf('>', i)
            if (end == -1) { sb.append(text[i++]); continue }

            val raw = text.substring(i + 1, end).trim()
            i = end + 1

            if (raw.startsWith('/')) {
                // Closing tag
                val name = raw.removePrefix("/").lowercase()
                when {
                    name == "reset" || name == "r" -> { colorStack.clear(); sb.append("§r") }
                    COLOR_TAGS.containsKey(name) -> {
                        if (colorStack.isNotEmpty()) colorStack.removeLast()
                        // Restore parent colour; if stack empty, reset
                        sb.append(colorStack.lastOrNull() ?: "§r")
                    }
                    else -> sb.append("§r") // closing a format tag or unknown = reset
                }
            } else {
                val name = raw.lowercase()
                when {
                    name == "reset" -> { colorStack.clear(); sb.append("§r") }
                    COLOR_TAGS.containsKey(name) -> {
                        val code = COLOR_TAGS[name]!!
                        colorStack.addLast(code)
                        sb.append(code)
                    }
                    FORMAT_TAGS.containsKey(name) -> sb.append(FORMAT_TAGS[name]!!)
                    else -> sb.append("<$raw>") // unknown tag — pass through unchanged
                }
            }
        }
        return sb.toString()
    }

    // ── gradient internals ──────────────────────────────────────────────────

    private fun resolveHex(color: String): Int? {
        NAMED_HEX[color.lowercase()]?.let { return it }
        val hex = color.removePrefix("#")
        return if (hex.length == 6) hex.toIntOrNull(16) else null
    }

    /** §x§R§R§G§G§B§B — Spigot 1.16+ hex colour escape. */
    private fun spigotHex(rgb: Int): String {
        val hex = "%06X".format(rgb)
        return "§x" + hex.map { "§$it" }.joinToString("")
    }

    private fun lerpColor(a: Int, b: Int, t: Float): Int {
        val r  = ((a shr 16 and 0xFF) + t * ((b shr 16 and 0xFF) - (a shr 16 and 0xFF))).toInt()
        val g  = ((a shr  8 and 0xFF) + t * ((b shr  8 and 0xFF) - (a shr  8 and 0xFF))).toInt()
        val bl = ((a        and 0xFF) + t * ((b        and 0xFF) - (a        and 0xFF))).toInt()
        return (r shl 16) or (g shl 8) or bl
    }

    /**
     * Applies a multi-stop gradient across the printable characters in [text].
     * Existing §-codes are skipped when stepping characters.
     */
    private fun applyGradient(text: String, stops: List<Int>): String {
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
                sb.append(text[i]).append(text[i + 1]); i += 2; continue
            }
            val t = if (printable <= 1) 0f else charIdx.toFloat() / (printable - 1)
            val seg = (t * (stops.size - 1)).toInt().coerceAtMost(stops.size - 2)
            sb.append(spigotHex(lerpColor(stops[seg], stops[seg + 1], t * (stops.size - 1) - seg)))
            sb.append(text[i])
            charIdx++; i++
        }
        return sb.toString()
    }
}
