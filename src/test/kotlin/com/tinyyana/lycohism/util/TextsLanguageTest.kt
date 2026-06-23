package com.tinyyana.lycohism.util

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the language resolver behind config `language` (auto|zh|en). Uses the Bukkit-free
 * [Texts.resolveLanguage] seam so it runs without paper-api on the test classpath.
 */
class TextsLanguageTest {

    @Test
    fun `auto follows the server locale`() {
        assertEquals("zh", Texts.resolveLanguage("auto", "zh"))
        assertEquals("en", Texts.resolveLanguage("auto", "en"))
        assertEquals("en", Texts.resolveLanguage("auto", "fr")) // anything non-zh -> English
    }

    @Test
    fun `explicit setting overrides the locale`() {
        assertEquals("en", Texts.resolveLanguage("en", "zh"))
        assertEquals("zh", Texts.resolveLanguage("zh-TW", "en"))
        assertEquals("zh", Texts.resolveLanguage("ZH_TW", "en")) // case-insensitive
    }

    @Test
    fun `unknown setting defaults by locale then English`() {
        assertEquals("zh", Texts.resolveLanguage("garbage", "zh"))
        assertEquals("en", Texts.resolveLanguage("garbage", "ja"))
    }
}
