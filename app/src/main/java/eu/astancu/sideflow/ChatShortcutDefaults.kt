package eu.astancu.sideflow

object ChatShortcutDefaults {
    const val SECTION_ID = "chatgpt"
    const val ICON_PACKAGE = "com.openai.chatgpt"
    const val ADD_TOOL_REFERENCE = "sideflow.tool.add_chatgpt_chat"
    const val DEFAULT_TITLE = "Chat"

    fun isChatGptTarget(target: String?): Boolean {
        val value = target?.trim()?.lowercase().orEmpty()
        return value.startsWith("https://chatgpt.com/") ||
            value.startsWith("http://chatgpt.com/") ||
            value.startsWith("https://www.chatgpt.com/") ||
            value.startsWith("http://www.chatgpt.com/")
    }
}
