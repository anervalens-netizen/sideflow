package eu.astancu.sideflow

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

enum class ShelfItemType {
    APP,
    DEEP_LINK,
    URL,
    FOLDER,
    SYSTEM_ACTION
}

data class ShelfItem(
    val id: String,
    val type: ShelfItemType,
    val reference: String,
    val label: String? = null,
    val iconPackage: String? = null,
    val children: List<ShelfItem> = emptyList()
) {
    val identityKey: String
        get() = "${type.name}:$reference"
}

data class ShelfSection(
    val id: String,
    val title: String,
    val columns: Int = SideFlowPolicy.DEFAULT_COLUMNS,
    val showTitle: Boolean = true,
    val items: List<ShelfItem> = emptyList()
)

data class ShelfConfig(
    val version: Int = SCHEMA_VERSION,
    val sections: List<ShelfSection>
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

object ShelfConfigOps {
    const val DEFAULT_SECTION_ID = "apps"
    const val DEFAULT_SECTION_TITLE = "Apps"

    fun defaultConfig(
        items: List<ShelfItem> = emptyList(),
        columns: Int = SideFlowPolicy.DEFAULT_COLUMNS
    ): ShelfConfig =
        ShelfConfig(
            sections = listOf(
                ShelfSection(
                    id = DEFAULT_SECTION_ID,
                    title = DEFAULT_SECTION_TITLE,
                    columns = SideFlowPolicy.sanitizeColumns(columns),
                    showTitle = true,
                    items = items
                )
            )
        )

    fun fromLegacyIdentifiers(
        identifiers: List<String>,
        columns: Int = SideFlowPolicy.DEFAULT_COLUMNS
    ): ShelfConfig {
        val items = identifiers
            .filter { it.isNotBlank() }
            .map(::migrateLegacyIdentifier)
            .distinct()
            .map(::legacyItem)
        return defaultConfig(items, columns)
    }

    fun migrateLegacyIdentifier(identifier: String): String {
        val value = identifier.trim()
        return when {
            value == "smartedge.folder.tools" || value == "sideflow.folder.tools" ->
                "sideflow.tool.tools"
            value.startsWith("smartedge.folder.") ->
                "sideflow.folder." + value.removePrefix("smartedge.folder.")
            value.startsWith("smartedge.tool.") ->
                "sideflow.tool." + value.removePrefix("smartedge.tool.")
            value.startsWith("smartedge.shortcut.") ->
                "sideflow.shortcut." + value.removePrefix("smartedge.shortcut.")
            else -> value
        }
    }

    fun legacyItem(identifier: String): ShelfItem {
        val canonicalIdentifier = migrateLegacyIdentifier(identifier)
        return ShelfItem(
            id = deterministicId("legacy:" + canonicalIdentifier),
            type = legacyType(canonicalIdentifier),
            reference = canonicalIdentifier
        )
    }

    private fun legacyType(identifier: String): ShelfItemType = when {
        identifier.startsWith("intent:") -> ShelfItemType.DEEP_LINK
        identifier.startsWith("http://") || identifier.startsWith("https://") -> ShelfItemType.URL
        identifier.startsWith("sideflow.folder.") -> ShelfItemType.FOLDER
        identifier.startsWith("sideflow.tool.") ||
            identifier == "sideflow.shortcut.one_hand" ||
            identifier == "sideflow.shortcut.reboot" -> ShelfItemType.SYSTEM_ACTION
        else -> ShelfItemType.APP
    }

    fun normalize(config: ShelfConfig): ShelfConfig {
        val sourceSections = if (config.sections.isEmpty()) defaultConfig().sections else config.sections
        val seenSections = mutableSetOf<String>()
        val seenTopLevelIdentities = mutableSetOf<String>()
        val usedItemIds = mutableSetOf<String>()

        val normalizedSections = sourceSections.mapIndexedNotNull { index, section ->
            val sectionId = section.id.trim().ifBlank {
                if (index == 0) DEFAULT_SECTION_ID else "section-" + (index + 1)
            }
            if (!seenSections.add(sectionId)) return@mapIndexedNotNull null

            val normalizedItems = normalizeItems(
                items = section.items,
                path = "section:" + sectionId,
                usedItemIds = usedItemIds,
                seenIdentities = seenTopLevelIdentities
            )

            section.copy(
                id = sectionId,
                title = section.title.trim().ifBlank { "Section " + (index + 1) },
                columns = SideFlowPolicy.sanitizeColumns(section.columns),
                items = normalizedItems
            )
        }

        return ShelfConfig(
            version = ShelfConfig.SCHEMA_VERSION,
            sections = if (normalizedSections.isEmpty()) defaultConfig().sections else normalizedSections
        )
    }

    fun addSection(
        config: ShelfConfig,
        title: String = "New section",
        columns: Int = SideFlowPolicy.DEFAULT_COLUMNS
    ): ShelfConfig {
        val normalized = normalize(config)
        val section = ShelfSection(
            id = "section-${UUID.randomUUID()}",
            title = title.trim().ifBlank { "New section" },
            columns = SideFlowPolicy.sanitizeColumns(columns),
            showTitle = true
        )
        return normalized.copy(sections = normalized.sections + section)
    }

    fun updateSection(
        config: ShelfConfig,
        sectionId: String,
        title: String? = null,
        columns: Int? = null,
        showTitle: Boolean? = null
    ): ShelfConfig {
        val normalized = normalize(config)
        return normalize(
            normalized.copy(
                sections = normalized.sections.map { section ->
                    if (section.id != sectionId) section
                    else section.copy(
                        title = title?.trim()?.takeIf { it.isNotEmpty() } ?: section.title,
                        columns = columns?.let(SideFlowPolicy::sanitizeColumns) ?: section.columns,
                        showTitle = showTitle ?: section.showTitle
                    )
                }
            )
        )
    }

    fun setAllSectionColumns(config: ShelfConfig, columns: Int): ShelfConfig {
        val normalized = normalize(config)
        val sanitized = SideFlowPolicy.sanitizeColumns(columns)
        return normalized.copy(
            sections = normalized.sections.map { section ->
                section.copy(columns = sanitized)
            }
        )
    }

    fun moveSection(config: ShelfConfig, fromIndex: Int, toIndex: Int): ShelfConfig {
        val normalized = normalize(config)
        if (fromIndex !in normalized.sections.indices || toIndex !in normalized.sections.indices) return normalized
        if (fromIndex == toIndex) return normalized

        val sections = normalized.sections.toMutableList()
        val section = sections.removeAt(fromIndex)
        sections.add(toIndex, section)
        return normalized.copy(sections = sections)
    }

    fun removeSection(config: ShelfConfig, sectionId: String): ShelfConfig {
        val normalized = normalize(config)
        if (normalized.sections.size <= 1) return normalized

        val removeIndex = normalized.sections.indexOfFirst { it.id == sectionId }
        if (removeIndex == -1) return normalized

        val removed = normalized.sections[removeIndex]
        val remaining = normalized.sections.toMutableList().apply { removeAt(removeIndex) }
        val destinationIndex = (removeIndex - 1).coerceAtLeast(0).coerceAtMost(remaining.lastIndex)
        val destination = remaining[destinationIndex]
        remaining[destinationIndex] = destination.copy(items = destination.items + removed.items)
        return normalize(normalized.copy(sections = remaining))
    }

    fun addItem(config: ShelfConfig, item: ShelfItem, sectionId: String? = null): ShelfConfig {
        var normalized = normalize(config)
        normalized = removeItem(normalized, item.identityKey)

        val targetId = sectionId?.takeIf { id -> normalized.sections.any { it.id == id } }
            ?: normalized.sections.first().id

        val sections = normalized.sections.map { section ->
            if (section.id == targetId) section.copy(items = section.items + item) else section
        }
        return normalize(normalized.copy(sections = sections))
    }

    /**
     * Removes by either stable item id or identity key (TYPE:reference).
     */
    fun removeItem(config: ShelfConfig, itemIdOrIdentity: String): ShelfConfig {
        val normalized = normalize(config)
        val sections = normalized.sections.map { section ->
            section.copy(
                items = section.items.filterNot {
                    it.id == itemIdOrIdentity || it.identityKey == itemIdOrIdentity
                }
            )
        }
        return normalize(normalized.copy(sections = sections))
    }

    fun moveItem(
        config: ShelfConfig,
        itemId: String,
        targetSectionId: String,
        targetIndex: Int
    ): ShelfConfig {
        val normalized = normalize(config)
        val item = normalized.sections.asSequence()
            .flatMap { it.items.asSequence() }
            .firstOrNull { it.id == itemId }
            ?: return normalized

        if (normalized.sections.none { it.id == targetSectionId }) return normalized

        val withoutItem = normalized.copy(
            sections = normalized.sections.map { section ->
                section.copy(items = section.items.filterNot { it.id == itemId })
            }
        )

        val sections = withoutItem.sections.map { section ->
            if (section.id != targetSectionId) return@map section
            val items = section.items.toMutableList()
            items.add(targetIndex.coerceIn(0, items.size), item)
            section.copy(items = items)
        }
        return normalize(withoutItem.copy(sections = sections))
    }

    /**
     * Applies item order captured from the flattened panel. Unknown/synthetic
     * sections are ignored and items not present in the order snapshot retain
     * their original section at the end. This makes drag persistence resilient
     * to notification rows and temporarily unresolved packages.
     */
    fun applyPanelOrder(
        config: ShelfConfig,
        orderedItemIdsBySection: Map<String, List<String>>
    ): ShelfConfig {
        val normalized = normalize(config)
        val itemById = normalized.sections
            .flatMap { it.items }
            .associateBy { it.id }
        val assigned = mutableSetOf<String>()

        val sections = normalized.sections.map { section ->
            val requested = orderedItemIdsBySection[section.id].orEmpty()
                .mapNotNull(itemById::get)
                .filter { assigned.add(it.id) }

            val retained = section.items.filter { item ->
                item.id !in assigned && orderedItemIdsBySection.values.none { item.id in it }
            }

            section.copy(items = requested + retained)
        }

        return normalize(normalized.copy(sections = sections))
    }

    fun updateItem(
        config: ShelfConfig,
        itemId: String,
        replacement: ShelfItem,
        targetSectionId: String? = null
    ): ShelfConfig {
        val normalized = normalize(config)
        val sourceSection = normalized.sections.firstOrNull { section ->
            section.items.any { it.id == itemId }
        } ?: return normalized

        val originalIndex = sourceSection.items.indexOfFirst { it.id == itemId }
        val destinationId = targetSectionId
            ?.takeIf { id -> normalized.sections.any { it.id == id } }
            ?: sourceSection.id

        val replacementWithStableId = replacement.copy(id = itemId)
        var without = normalized.copy(
            sections = normalized.sections.map { section ->
                section.copy(items = section.items.filterNot { it.id == itemId })
            }
        )

        // Enforce the same identity uniqueness rule as addItem.
        without = removeItem(without, replacementWithStableId.identityKey)

        val sections = without.sections.map { section ->
            if (section.id != destinationId) return@map section
            val items = section.items.toMutableList()
            val index = if (destinationId == sourceSection.id) {
                originalIndex.coerceIn(0, items.size)
            } else {
                items.size
            }
            items.add(index, replacementWithStableId)
            section.copy(items = items)
        }

        return normalize(without.copy(sections = sections))
    }

    fun findItem(config: ShelfConfig, itemId: String): ShelfItem? =
        findItemRecursive(
            normalize(config).sections.flatMap { it.items },
            itemId
        )

    fun allItems(config: ShelfConfig): List<ShelfItem> =
        normalize(config).sections.flatMap { it.items }

    fun allItemsRecursive(config: ShelfConfig): List<ShelfItem> {
        val result = mutableListOf<ShelfItem>()
        fun collect(items: List<ShelfItem>) {
            items.forEach { item ->
                result += item
                collect(item.children)
            }
        }
        collect(normalize(config).sections.flatMap { it.items })
        return result
    }

    fun containsShortcutTargets(config: ShelfConfig): Boolean =
        allItemsRecursive(config).any {
            it.type == ShelfItemType.URL || it.type == ShelfItemType.DEEP_LINK
        }

    private fun findItemRecursive(items: List<ShelfItem>, itemId: String): ShelfItem? {
        items.forEach { item ->
            if (item.id == itemId) return item
            findItemRecursive(item.children, itemId)?.let { return it }
        }
        return null
    }

    private fun normalizeItems(
        items: List<ShelfItem>,
        path: String,
        usedItemIds: MutableSet<String>,
        seenIdentities: MutableSet<String>
    ): List<ShelfItem> =
        items.mapIndexedNotNull { index, item ->
            val originalReference = item.reference.trim()
            if (originalReference.isBlank()) return@mapIndexedNotNull null

            val reference = migrateLegacyIdentifier(originalReference)
            val type = if (
                isLegacyPseudoIdentifier(originalReference) ||
                originalReference == "sideflow.folder.tools"
            ) {
                legacyType(reference)
            } else {
                item.type
            }
            val identity = type.name + ":" + reference
            if (!seenIdentities.add(identity)) return@mapIndexedNotNull null

            val id = uniqueItemId(
                preferredId = item.id,
                seed = path + ":" + index + ":" + identity,
                usedItemIds = usedItemIds
            )

            item.copy(
                id = id,
                type = type,
                reference = reference,
                label = item.label?.trim()?.takeIf { it.isNotEmpty() },
                iconPackage = item.iconPackage?.trim()?.takeIf { it.isNotEmpty() },
                children = normalizeChildren(
                    children = item.children,
                    parentPath = path + "/" + id,
                    usedItemIds = usedItemIds
                )
            )
        }

    private fun normalizeChildren(
        children: List<ShelfItem>,
        parentPath: String,
        usedItemIds: MutableSet<String>
    ): List<ShelfItem> =
        normalizeItems(
            items = children,
            path = parentPath,
            usedItemIds = usedItemIds,
            seenIdentities = mutableSetOf()
        )

    private fun uniqueItemId(
        preferredId: String,
        seed: String,
        usedItemIds: MutableSet<String>
    ): String {
        val preferred = preferredId.trim()
        if (preferred.isNotEmpty() && usedItemIds.add(preferred)) return preferred

        var attempt = 0
        while (true) {
            val candidateSeed = if (attempt == 0) seed else seed + ":" + attempt
            val candidate = deterministicId(candidateSeed)
            if (usedItemIds.add(candidate)) return candidate
            attempt += 1
        }
    }

    private fun isLegacyPseudoIdentifier(identifier: String): Boolean =
        identifier.startsWith("smartedge.folder.") ||
            identifier.startsWith("smartedge.tool.") ||
            identifier.startsWith("smartedge.shortcut.")

    private fun deterministicId(seed: String): String =
        UUID.nameUUIDFromBytes(seed.toByteArray(StandardCharsets.UTF_8)).toString()
}

object ShelfConfigJson {
    fun encode(config: ShelfConfig): String {
        val normalized = ShelfConfigOps.normalize(config)
        val root = JSONObject()
            .put("version", ShelfConfig.SCHEMA_VERSION)

        val sections = JSONArray()
        normalized.sections.forEach { section ->
            sections.put(
                JSONObject()
                    .put("id", section.id)
                    .put("title", section.title)
                    .put("columns", section.columns)
                    .put("showTitle", section.showTitle)
                    .put("items", encodeItems(section.items))
            )
        }
        root.put("sections", sections)
        return root.toString()
    }

    fun decode(json: String): ShelfConfig? = runCatching {
        val root = JSONObject(json)
        val version = root.optInt("version", 0)
        if (version !in 1..ShelfConfig.SCHEMA_VERSION) return null

        val sectionsJson = root.optJSONArray("sections") ?: return null
        val sections = buildList {
            for (i in 0 until sectionsJson.length()) {
                val obj = sectionsJson.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val title = obj.optString("title")
                val columns = obj.optInt("columns", SideFlowPolicy.DEFAULT_COLUMNS)
                val showTitle = obj.optBoolean("showTitle", true)
                val items = decodeItems(obj.optJSONArray("items"))
                add(ShelfSection(id, title, columns, showTitle, items))
            }
        }
        ShelfConfigOps.normalize(ShelfConfig(version = version, sections = sections))
    }.getOrNull()

    private fun encodeItems(items: List<ShelfItem>): JSONArray {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
                .put("id", item.id)
                .put("type", item.type.name)
                .put("reference", item.reference)

            item.label?.let { obj.put("label", it) }
            item.iconPackage?.let { obj.put("iconPackage", it) }
            if (item.children.isNotEmpty()) obj.put("children", encodeItems(item.children))
            array.put(obj)
        }
        return array
    }

    private fun decodeItems(array: JSONArray?): List<ShelfItem> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val type = runCatching {
                    ShelfItemType.valueOf(obj.optString("type"))
                }.getOrNull() ?: continue
                val reference = obj.optString("reference")
                if (reference.isBlank()) continue
                add(
                    ShelfItem(
                        id = obj.optString("id"),
                        type = type,
                        reference = reference,
                        label = obj.optString("label").takeIf { it.isNotBlank() },
                        iconPackage = obj.optString("iconPackage").takeIf { it.isNotBlank() },
                        children = decodeItems(obj.optJSONArray("children"))
                    )
                )
            }
        }
    }
}
