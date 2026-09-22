package eu.astancu.sideflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfConfigTest {

    @Test
    fun legacyMigrationPreservesOrderAndClassifiesItems() {
        val config = ShelfConfigOps.fromLegacyIdentifiers(
            listOf(
                "com.example.one",
                "intent:#Intent;component=com.example/.MainActivity;end",
                "https://example.com/path",
                "sideflow.tool.screenshot"
            )
        )

        assertEquals(1, config.sections.size)
        assertEquals(4, config.sections.single().items.size)
        assertEquals(
            listOf(
                ShelfItemType.APP,
                ShelfItemType.DEEP_LINK,
                ShelfItemType.URL,
                ShelfItemType.SYSTEM_ACTION
            ),
            config.sections.single().items.map { it.type }
        )
    }

    @Test
    fun jsonRoundTripKeepsSectionsAndFolderChildren() {
        val config = ShelfConfig(
            sections = listOf(
                ShelfSection(
                    id = "daily",
                    title = "Daily",
                    columns = 5,
                    showTitle = true,
                    items = listOf(
                        ShelfItem(
                            id = "folder-1",
                            type = ShelfItemType.FOLDER,
                            reference = "sideflow.folder.tools",
                            label = "Tools",
                            children = listOf(
                                ShelfItem(
                                    id = "child-1",
                                    type = ShelfItemType.URL,
                                    reference = "https://example.com",
                                    label = "Example"
                                )
                            )
                        )
                    )
                )
            )
        )

        val decoded = ShelfConfigJson.decode(ShelfConfigJson.encode(config))
        assertNotNull(decoded)
        assertEquals("Daily", decoded!!.sections.single().title)
        assertEquals(5, decoded.sections.single().columns)
        assertEquals("Example", decoded.sections.single().items.single().children.single().label)
    }

    @Test
    fun normalizeRemovesDuplicateItemsAcrossSections() {
        val duplicateA = ShelfItem("a", ShelfItemType.APP, "com.example")
        val duplicateB = ShelfItem("b", ShelfItemType.APP, "com.example")
        val config = ShelfConfig(
            sections = listOf(
                ShelfSection("one", "One", items = listOf(duplicateA)),
                ShelfSection("two", "Two", items = listOf(duplicateB))
            )
        )

        val normalized = ShelfConfigOps.normalize(config)
        assertEquals(1, normalized.sections.sumOf { it.items.size })
        assertTrue(normalized.sections.first().items.isNotEmpty())
        assertTrue(normalized.sections.last().items.isEmpty())
    }

    @Test
    fun moveItemCanCrossSectionsAndPreserveTargetOrder() {
        val first = ShelfItem("one", ShelfItemType.APP, "com.one")
        val second = ShelfItem("two", ShelfItemType.APP, "com.two")
        val third = ShelfItem("three", ShelfItemType.APP, "com.three")
        val config = ShelfConfig(
            sections = listOf(
                ShelfSection("a", "A", items = listOf(first, second)),
                ShelfSection("b", "B", items = listOf(third))
            )
        )

        val moved = ShelfConfigOps.moveItem(config, "two", "b", 0)

        assertEquals(listOf("one"), moved.sections[0].items.map { it.id })
        assertEquals(listOf("two", "three"), moved.sections[1].items.map { it.id })
    }

    @Test
    fun removingASectionMovesItsItemsInsteadOfDroppingThem() {
        val first = ShelfItem("one", ShelfItemType.APP, "com.one")
        val second = ShelfItem("two", ShelfItemType.APP, "com.two")
        val config = ShelfConfig(
            sections = listOf(
                ShelfSection("a", "A", items = listOf(first)),
                ShelfSection("b", "B", items = listOf(second))
            )
        )

        val result = ShelfConfigOps.removeSection(config, "b")
        assertEquals(1, result.sections.size)
        assertEquals(listOf("one", "two"), result.sections.single().items.map { it.id })
    }

    @Test
    fun panelOrderCanMoveItemsAcrossSectionsWithoutDroppingUnresolvedItems() {
        val first = ShelfItem("one", ShelfItemType.APP, "com.one")
        val second = ShelfItem("two", ShelfItemType.APP, "com.two")
        val unresolved = ShelfItem("three", ShelfItemType.APP, "com.three")
        val config = ShelfConfig(
            sections = listOf(
                ShelfSection("a", "A", items = listOf(first, unresolved)),
                ShelfSection("b", "B", items = listOf(second))
            )
        )

        val reordered = ShelfConfigOps.applyPanelOrder(
            config,
            linkedMapOf(
                "a" to emptyList(),
                "b" to listOf("one", "two")
            )
        )

        assertEquals(listOf("three"), reordered.sections[0].items.map { it.id })
        assertEquals(listOf("one", "two"), reordered.sections[1].items.map { it.id })
    }

    @Test
    fun sectionColumnsAreClampedAndSectionOrderIsStable() {
        val config = ShelfConfig(
            sections = listOf(
                ShelfSection("a", "A", columns = 99),
                ShelfSection("b", "B", columns = 1)
            )
        )

        val normalized = ShelfConfigOps.normalize(config)
        assertEquals(6, normalized.sections[0].columns)
        assertEquals(2, normalized.sections[1].columns)

        val moved = ShelfConfigOps.moveSection(normalized, 1, 0)
        assertEquals(listOf("b", "a"), moved.sections.map { it.id })
    }

    @Test
    fun updatingShortcutKeepsStableIdAndCanMoveSections() {
        val original = ShelfItem(
            id = "shortcut-1",
            type = ShelfItemType.URL,
            reference = "https://old.example",
            label = "Old"
        )
        val config = ShelfConfig(
            sections = listOf(
                ShelfSection("a", "A", items = listOf(original)),
                ShelfSection("b", "B")
            )
        )

        val updated = ShelfConfigOps.updateItem(
            config,
            itemId = "shortcut-1",
            replacement = ShelfItem(
                id = "ignored",
                type = ShelfItemType.URL,
                reference = "https://new.example",
                label = "New",
                iconPackage = "com.example.icon"
            ),
            targetSectionId = "b"
        )

        assertEquals(emptyList<String>(), updated.sections[0].items.map { it.id })
        assertEquals("shortcut-1", updated.sections[1].items.single().id)
        assertEquals("New", updated.sections[1].items.single().label)
        assertEquals("com.example.icon", updated.sections[1].items.single().iconPackage)
    }

    @Test
    fun unsupportedJsonVersionFailsSafely() {
        assertFalse(ShelfConfigJson.decode("{\"version\":99,\"sections\":[]}") != null)
    }
}
