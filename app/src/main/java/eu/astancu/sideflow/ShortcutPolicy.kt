package eu.astancu.sideflow

object ShortcutPolicy {
    private val uriPattern = Regex("""(?i)(?:https?://|www\.|intent:|[a-z][a-z0-9+.-]*:)[^\s<>"']+""")
    private val sharedTextPunctuation = charArrayOf('.', ',', '!', '?')

    fun extractTarget(sharedText: String?): String? {
        val text = sharedText?.trim().orEmpty()
        if (text.isBlank()) return null

        val match = uriPattern.find(text) ?: return normalizeTarget(text)
        val prefix = text.substring(0, match.range.first)
        val suffix = text.substring(match.range.last + 1)
        val standaloneTarget = prefix.isBlank() && suffix.isBlank()
        return normalizeTarget(
            trimSharedTextTarget(
                raw = match.value,
                trimSentencePunctuation = !standaloneTarget
            )
        )
    }

    /**
     * Normalizes explicit user input without deleting syntactically valid URI
     * characters. Prose punctuation cleanup belongs only to extractTarget().
     */
    fun normalizeTarget(raw: String?): String? {
        var value = raw?.trim().orEmpty()
        if (value.isBlank()) return null

        if (value.startsWith("www.", ignoreCase = true)) {
            value = "https://$value"
        }

        return if (
            value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("intent:", ignoreCase = true) ||
            Regex("""^[a-zA-Z][a-zA-Z0-9+.-]*:.+""").matches(value)
        ) {
            value
        } else {
            null
        }
    }

    private fun trimSharedTextTarget(
        raw: String,
        trimSentencePunctuation: Boolean
    ): String {
        var value = if (trimSentencePunctuation) {
            raw.trimEnd(*sharedTextPunctuation)
        } else {
            raw
        }

        fun trimUnmatchedClosing(open: Char, close: Char) {
            while (
                value.endsWith(close) &&
                value.count { it == close } > value.count { it == open }
            ) {
                value = value.dropLast(1)
                if (trimSentencePunctuation) {
                    value = value.trimEnd(*sharedTextPunctuation)
                }
            }
        }

        trimUnmatchedClosing('(', ')')
        trimUnmatchedClosing('[', ']')
        trimUnmatchedClosing('{', '}')
        return value
    }

    fun classify(target: String): ShelfItemType = when {
        target.startsWith("http://", ignoreCase = true) ||
            target.startsWith("https://", ignoreCase = true) -> ShelfItemType.URL
        else -> ShelfItemType.DEEP_LINK
    }

    fun suggestedLabel(target: String): String {
        if (classify(target) == ShelfItemType.URL) {
            val withoutScheme = target.substringAfter("://")
            val host = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
            if (host.isNotBlank()) return host.removePrefix("www.")
        }

        val scheme = target.substringBefore(':', missingDelimiterValue = "")
        return scheme.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: "Shortcut"
    }
}
