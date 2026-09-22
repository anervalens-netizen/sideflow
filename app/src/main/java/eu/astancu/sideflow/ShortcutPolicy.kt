package eu.astancu.sideflow

object ShortcutPolicy {
    private val uriPattern = Regex("""(?i)(?:https?://|www\.|intent:|[a-z][a-z0-9+.-]*:)[^\s<>"']+""")
    private val trailingPunctuation = charArrayOf('.', ',', ';', ':', ')', ']', '}', '!', '?')

    fun extractTarget(sharedText: String?): String? {
        val text = sharedText?.trim().orEmpty()
        if (text.isBlank()) return null

        val match = uriPattern.find(text)?.value ?: return normalizeTarget(text)
        return normalizeTarget(match)
    }

    fun normalizeTarget(raw: String?): String? {
        var value = raw?.trim().orEmpty()
        if (value.isBlank()) return null

        value = value.trimEnd(*trailingPunctuation)

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
