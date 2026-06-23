package com.tinyyana.lycohism.util

import com.tinyyana.lycohism.Lycohism
import org.bukkit.configuration.file.YamlConfiguration
import java.util.Locale

/**
 * Editable text layer backed by a per-language lang file (lang_zh.yml / lang_en.yml).
 * All player-facing prose (book pages, GUI labels, common messages) lives here so it
 * can be reworded or recoloured without touching code. Loaded once on enable and
 * refreshed on /lycohism reload.
 *
 * Which file is active comes from config.yml `language` (auto|zh|en); `auto` follows
 * the server's JVM locale so a zh-region host defaults to Chinese, everyone else English.
 */
object Texts {

    private var yaml: YamlConfiguration? = null

    fun load(plugin: Lycohism) {
        val setting = plugin.config.getString("language", "auto") ?: "auto"
        val code = resolveLanguage(setting, Locale.getDefault().language)
        // ponytail: each language is its own on-disk file; switching language reads a
        // different file rather than migrating an admin's edits. Fine for ALPHA.
        yaml = ConfigFiles.load(plugin, "lang_$code.yml")
    }

    /**
     * Resolves the bundled language code from the config [setting], falling back to the
     * server's [serverLanguage] (JVM locale language tag) when set to "auto". Bukkit-free
     * so it stays unit-testable. Unknown values default to English.
     */
    fun resolveLanguage(setting: String, serverLanguage: String): String =
        when (setting.trim().lowercase()) {
            "zh", "zh_tw", "zh-tw", "zh-hant", "zh_hant", "cht", "chinese" -> "zh"
            "en", "en_us", "en-us", "english" -> "en"
            else -> if (serverLanguage.lowercase().startsWith("zh")) "zh" else "en"
        }

    /** A single line, falling back to [default] when the path is missing. */
    fun line(path: String, default: String = path): String = yaml?.getString(path) ?: default

    /** A list of lines, falling back to [default] when the path is missing or empty. */
    fun lines(path: String, default: List<String> = emptyList()): List<String> {
        val list = yaml?.getStringList(path) ?: return default
        return if (list.isEmpty()) default else list
    }

    /** A list of structured entries (each a map), used for data-driven book pages. */
    fun entries(path: String): List<Map<*, *>> = yaml?.getMapList(path) ?: emptyList()

    /** Replaces {placeholders} in [path]'s line with the given pairs. */
    fun line(path: String, default: String, vararg replacements: Pair<String, String>): String {
        var result = line(path, default)
        for ((key, value) in replacements) result = result.replace("{$key}", value)
        return result
    }

    /** A configured line with placeholders, falling back to the path only when misconfigured. */
    fun render(path: String, vararg replacements: Pair<String, String>): String =
        replace(line(path), replacements)

    /** Configured lines with the same placeholder replacements applied to every line. */
    fun renderLines(path: String, vararg replacements: Pair<String, String>): List<String> =
        lines(path).map { replace(it, replacements) }

    private fun replace(text: String, replacements: Array<out Pair<String, String>>): String {
        var result = text
        for ((key, value) in replacements) result = result.replace("{$key}", value)
        return result
    }
}
