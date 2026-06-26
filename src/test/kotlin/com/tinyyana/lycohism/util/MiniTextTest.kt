package com.tinyyana.lycohism.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MiniTextTest {

    @Test fun legacyAndCodesPassThrough() {
        assertEquals("§bHello§r", MiniText.parse("&bHello&r"))
    }

    @Test fun miniMessageColourTag() {
        // opening tag emits §code; self-closing reset emits §r
        assertEquals("§bHello§r", MiniText.parse("<aqua>Hello<reset>"))
    }

    @Test fun closingColourTagRestoresParent() {
        // </white> should pop white and re-emit parent §7 (gray), not §r
        val out = MiniText.parse("<gray>text <white>hi</white> more</gray>")
        // after </white> we should see §7 again, then after </gray> §r
        assertTrue(out.contains("§7text §fhi§7 more§r"), "got: $out")
    }

    @Test fun closingTagOnEmptyStackResets() {
        val out = MiniText.parse("<white>hi</white>")
        assertEquals("§fhi§r", out)
    }

    @Test fun resetClearsStack() {
        val out = MiniText.parse("<gray><white>x<reset>y")
        // after <reset>, colour stack is cleared — y has no colour prefix
        assertEquals("§7§fx§ry", out)
    }

    @Test fun gradientTwoStopsProducesHexPerChar() {
        val out = MiniText.parse("<gradient:#FF0000:#0000FF>AB</gradient>")
        assertTrue(out.startsWith("§x§F§F§0§0§0§0A"), "first char red: $out")
        assertTrue(out.contains("§x§0§0§0§0§F§FB"), "last char blue: $out")
    }

    @Test fun gradientThreeStops() {
        val out = MiniText.parse("<gradient:#FF0000:#00FF00:#0000FF>ABC</gradient>")
        assertEquals(3, "§x".toRegex().findAll(out).count(), "3 hex prefixes: $out")
    }

    @Test fun gradientInsideColorStack() {
        // gradient inside a gray context; the </gray> after should emit §r (stack empty after pop)
        val out = MiniText.parse("<gray>use <gradient:#ffd86b:#b07cff>crystal</gradient> now</gray>")
        assertTrue(out.contains("§7use "), "gray prefix: $out")
        assertTrue(out.contains("§x"), "gradient hex: $out")
        assertTrue(out.contains(" now§r"), "restore after gradient+close: $out")
    }

    @Test fun unknownTagPassedThrough() {
        val out = MiniText.parse("<gray>text</gray>")
        assertEquals("§7text§r", out)
    }
}
