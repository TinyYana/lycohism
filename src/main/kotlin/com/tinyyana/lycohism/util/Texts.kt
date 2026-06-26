package com.tinyyana.lycohism.util

import com.tinyyana.lycohism.Lycohism
import org.bukkit.configuration.file.YamlConfiguration
import java.util.Locale

/**
 * Editable text layer backed by lang_zh.yml and lang_en.yml.
 * Both files are always loaded so per-player locale lookups work without a reload.
 *
 * The plugin-wide default language (from config.yml `language`) drives the no-arg
 * variants of line()/lines()/render(). Per-player lookups use lineInLang()/linesInLang()
 * with the lang code from langCodeFor(player.locale).
 */
object Texts {

    /** The plugin's configured language code ("zh" or "en"). */
    var activeLanguage: String = "en"
        private set

    private val langs = mutableMapOf<String, YamlConfiguration>()

    fun load(plugin: Lycohism) {
        val setting = plugin.config.getString("language", "auto") ?: "auto"
        activeLanguage = resolveLanguage(setting, Locale.getDefault().language)
        // ponytail: load both so per-player locale lookups work without a reload
        langs["zh"] = ConfigFiles.load(plugin, "lang_zh.yml")
        langs["en"] = ConfigFiles.load(plugin, "lang_en.yml")
    }

    /**
     * Maps a Bukkit player locale string (e.g. "zh_TW", "ja_JP", "es_es") to the
     * nearest supported lang code. zh_* → "zh"; everything else → "en".
     */
    fun langCodeFor(playerLocale: String): String =
        if (playerLocale.lowercase().startsWith("zh")) "zh" else "en"

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

    // ── per-locale API (use when you have a specific player) ────────────────

    /** A single line in the given lang, falling back to EN then to [default]. */
    fun lineInLang(path: String, lang: String, default: String = path): String =
        yaml(lang).getString(path) ?: yaml("en").getString(path) ?: default

    /** A list of lines in the given lang, falling back to EN then to [default]. */
    fun linesInLang(path: String, lang: String, default: List<String> = emptyList()): List<String> {
        val list = yaml(lang).getStringList(path)
        if (list.isNotEmpty()) return list
        val en = yaml("en").getStringList(path)
        return if (en.isNotEmpty()) en else default
    }

    // ── plugin-wide API (use the configured default language) ────────────────

    /** A single line, falling back to [default] when the path is missing. */
    fun line(path: String, default: String = path): String =
        lineInLang(path, activeLanguage, default)

    /** A list of lines, falling back to [default] when the path is missing or empty. */
    fun lines(path: String, default: List<String> = emptyList()): List<String> =
        linesInLang(path, activeLanguage, default)

    /** A list of structured entries (each a map), used for data-driven book pages. */
    fun entries(path: String): List<Map<*, *>> = yaml(activeLanguage).getMapList(path)

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

    private fun yaml(lang: String) = langs[lang] ?: langs["en"]!!
}
