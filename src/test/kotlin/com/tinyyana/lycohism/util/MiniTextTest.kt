package com.tinyyana.lycohism.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MiniTextTest {

    @Test fun legacyAndCodesPassThrough() {
        assertEquals("§bHello§r", MiniText.parse("&bHello&r"))
    }

    @Test fun miniMessageColourTag() {
        assertEquals("§bHello§r", MiniText.parse("<aqua>Hello</aqua>"))
    }

    @Test fun closingTagBecomesReset() {
        assertEquals("§atext§r", MiniText.parse("<green>text</green>"))
    }

    @Test fun gradientTwoStopsProducesHexPerChar() {
        // Red (#FF0000) → Blue (#0000FF) across "AB" — each char gets a §x hex prefix
        val out = MiniText.parse("<gradient:#FF0000:#0000FF>AB</gradient>")
        // First char should be §x§F§F§0§0§0§0 (pure red), second §x§0§0§0§0§F§F (pure blue)
        assertTrue(out.startsWith("§x§F§F§0§0§0§0A"), "first char should be red: $out")
        assertTrue(out.contains("§x§0§0§0§0§F§FB"), "last char should be blue: $out")
    }

    @Test fun gradientNamedColors() {
        val out = MiniText.parse("<gradient:red:blue>X</gradient>")
        // Single char — should use first stop (red = #FF5555)
        assertTrue(out.contains("§x"), "should contain hex escape: $out")
        assertTrue(out.contains("X"), "should contain the char: $out")
    }

    @Test fun gradientThreeStops() {
        val out = MiniText.parse("<gradient:#FF0000:#00FF00:#0000FF>ABC</gradient>")
        assertTrue(out.contains("A") && out.contains("B") && out.contains("C"))
        // A=red, B≈green, C=blue — just verify output has 3 hex escapes
        assertEquals(3, "§x".toRegex().findAll(out).count(), "expected 3 hex colour prefixes: $out")
    }

    @Test fun gradientSkipsExistingCodes() {
        // §l inside gradient should be preserved and not counted as a printable char
        val out = MiniText.parse("<gradient:#FF0000:#0000FF>§lAB</gradient>")
        assertTrue(out.contains("§l"), "bold code should survive: $out")
        assertEquals(2, "§x".toRegex().findAll(out).count(), "still only 2 printable chars: $out")
    }
}
