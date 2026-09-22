package eu.astancu.sideflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortcutPolicyTest {

    @Test
    fun extractsFirstUrlFromSharedText() {
        val target = ShortcutPolicy.extractTarget(
            "Interesting chat: https://example.com/path?q=1 — saved from another app"
        )
        assertEquals("https://example.com/path?q=1", target)
    }

    @Test
    fun stripsCommonTrailingPunctuation() {
        assertEquals(
            "https://example.com/chat/123",
            ShortcutPolicy.extractTarget("See https://example.com/chat/123).")
        )
    }

    @Test
    fun supportsIntentAndCustomSchemeDeepLinks() {
        assertEquals(
            ShelfItemType.DEEP_LINK,
            ShortcutPolicy.classify("intent:#Intent;package=com.example;end")
        )
        assertEquals(
            ShelfItemType.DEEP_LINK,
            ShortcutPolicy.classify("example://conversation/123")
        )
    }

    @Test
    fun httpLinksAreUrlItems() {
        assertEquals(
            ShelfItemType.URL,
            ShortcutPolicy.classify("https://example.com")
        )
    }

    @Test
    fun invalidPlainTextIsRejected() {
        assertNull(ShortcutPolicy.extractTarget("just some text without a link"))
    }

    @Test
    fun wwwTargetsAreNormalizedToHttps() {
        assertEquals(
            "https://www.example.com/path",
            ShortcutPolicy.normalizeTarget("www.example.com/path")
        )
    }

    @Test
    fun supportsCommonNonSlashUriSchemes() {
        assertEquals(
            "mailto:person@example.com",
            ShortcutPolicy.extractTarget("Contact: mailto:person@example.com")
        )
        assertEquals(
            "geo:0,0?q=Bucharest",
            ShortcutPolicy.normalizeTarget("geo:0,0?q=Bucharest")
        )
    }

    @Test
    fun extractsBareWwwLinkFromSurroundingText() {
        assertEquals(
            "https://www.example.com/path",
            ShortcutPolicy.extractTarget("Open www.example.com/path when ready.")
        )
    }

    @Test
    fun suggestedLabelUsesHostWithoutWww() {
        assertEquals(
            "example.com",
            ShortcutPolicy.suggestedLabel("https://www.example.com/path")
        )
    }
}
