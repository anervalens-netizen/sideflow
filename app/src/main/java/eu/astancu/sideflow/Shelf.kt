package eu.astancu.sideflow

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ShelfItem(val id: String, val reference: String, val label: String? = null)
data class ShelfSection(val id: String, val title: String, val items: List<ShelfItem>)
data class Shelf(val sections: List<ShelfSection>) {
    fun add(sectionId: String, packageName: String, label: String?): Shelf {
        require(PACKAGE.matches(packageName))
        val source = if (sections.isEmpty() && sectionId == "apps") empty().sections else sections
        require(source.any { it.id == sectionId })
        require(source.none { section -> section.items.any { it.reference == packageName } }) {
            "This app is already in the sidebar. Remove it before adding it to another section."
        }
        require(source.sumOf { it.items.size } < 256)
        val item = ShelfItem(UUID.randomUUID().toString(), packageName, label)
        return copy(sections = source.map { section ->
            if (section.id == sectionId) section.copy(items = section.items + item) else section
        })
    }

    fun remove(itemId: String): Shelf = copy(sections = sections.map { section ->
        section.copy(items = section.items.filterNot { it.id == itemId })
    })

    companion object {
        val PACKAGE = Regex("[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)+")
        fun empty() = Shelf(listOf(ShelfSection("apps", "Apps", emptyList())))
    }
}

/** Both formats are validated as complete documents before any store write. */
object ShelfCodec {
    const val MAX_BYTES = 256 * 1024
    private val rootKeys = setOf("version", "sections")
    private val legacySectionKeys = setOf("id", "title", "columns", "showTitle", "items")
    private val legacyItemKeys = setOf("id", "type", "reference", "label", "iconPackage", "children")
    private val newSectionKeys = setOf("id", "title", "items")
    private val newItemKeys = setOf("id", "reference", "label")

    fun legacy(json: String): Shelf = decode(json, 1)
    fun current(json: String): Shelf = decode(json, 2)

    private fun decode(json: String, version: Int): Shelf {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Shelf exceeds size limit" }
        val root = JSONObject(json)
        keys(root, rootKeys)
        require(integer(root, "version") == version) { "Unsupported shelf version" }
        val sections = array(root, "sections")
        require(sections.length() <= 32) { "Too many sections" }
        val ids = HashSet<String>()
        var total = 0
        return Shelf((0 until sections.length()).map { index ->
            val section = objectAt(sections, index)
            keys(section, if (version == 1) legacySectionKeys else newSectionKeys)
            val id = bounded(string(section, "id"), 128)
            require(ids.add(id)) { "Duplicate ID" }
            val title = bounded(string(section, "title"), 128)
            if (version == 1) {
                require(integer(section, "columns") in 1..10) { "Invalid legacy columns" }
                require(section.get("showTitle") is Boolean) { "Invalid legacy title visibility" }
            }
            val items = array(section, "items")
            total += items.length()
            require(total <= 256) { "Too many shortcuts" }
            ShelfSection(id, title, (0 until items.length()).map { itemIndex ->
                val item = objectAt(items, itemIndex)
                keys(item, if (version == 1) legacyItemKeys else newItemKeys)
                val itemId = bounded(string(item, "id"), 128)
                require(ids.add(itemId)) { "Duplicate ID" }
                if (version == 1) {
                    require(string(item, "type") == "APP") { "Unsupported shortcut type" }
                    if (item.has("children")) require(array(item, "children").length() == 0) { "Nested shortcut" }
                    require(!item.has("iconPackage")) { "Custom icon needs legacy editor" }
                }
                val target = bounded(string(item, "reference"), 255)
                require(Shelf.PACKAGE.matches(target)) { "Invalid app package" }
                val label = if (item.has("label")) bounded(string(item, "label"), 256) else null
                ShelfItem(itemId, target, label)
            })
        })
    }

    fun encode(shelf: Shelf): String {
        val sections = JSONArray()
        shelf.sections.forEach { section ->
            val items = JSONArray()
            section.items.forEach { item ->
                val obj = JSONObject().put("id", item.id).put("reference", item.reference)
                item.label?.let { obj.put("label", it) }
                items.put(obj)
            }
            sections.put(JSONObject().put("id", section.id).put("title", section.title).put("items", items))
        }
        return JSONObject().put("version", 2).put("sections", sections).toString().also { current(it) }
    }

    private fun objectAt(array: JSONArray, index: Int): JSONObject {
        val value = array.get(index)
        require(value is JSONObject) { "Expected object" }
        return value
    }

    private fun array(obj: JSONObject, key: String): JSONArray {
        val value = obj.get(key)
        require(value is JSONArray) { "Expected array: $key" }
        return value
    }

    private fun string(obj: JSONObject, key: String): String {
        val value = obj.get(key)
        require(value is String) { "Expected string: $key" }
        return value
    }

    private fun integer(obj: JSONObject, key: String): Int {
        val value = obj.get(key)
        require(value is Int || value is Long) { "Expected integer: $key" }
        val number = (value as Number).toLong()
        require(number in Int.MIN_VALUE..Int.MAX_VALUE) { "Integer out of range: $key" }
        return number.toInt()
    }

    private fun bounded(value: String, max: Int): String {
        require(value.isNotBlank() && value.length <= max && value == value.trim()) { "Invalid shelf text" }
        return value
    }

    private fun keys(obj: JSONObject, allowed: Set<String>) {
        require(obj.keys().asSequence().all { it in allowed }) { "Unknown shelf field" }
    }
}
