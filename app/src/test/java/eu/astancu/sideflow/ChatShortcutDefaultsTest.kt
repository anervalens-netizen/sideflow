package eu.astancu.sideflow

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatShortcutDefaultsTest {
    @Test
    fun recognizesChatGptWebTargetsOnly() {
        assertTrue(ChatShortcutDefaults.isChatGptTarget("https://chatgpt.com/c/abc"))
        assertTrue(ChatShortcutDefaults.isChatGptTarget("HTTPS://WWW.CHATGPT.COM/share/abc"))
        assertFalse(ChatShortcutDefaults.isChatGptTarget("https://example.com/chatgpt.com"))
        assertFalse(ChatShortcutDefaults.isChatGptTarget(null))
    }
}
