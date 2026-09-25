package eu.astancu.sideflow

import org.junit.Assert.*
import org.junit.Test

class ShelfCodecTest {
    private val legacy = """{"version":1,"sections":[{"id":"first","title":"First","columns":2,"showTitle":true,"items":[{"id":"one","type":"APP","reference":"test.example.alpha","label":"Alpha"},{"id":"two","type":"APP","reference":"test.example.beta"}]},{"id":"second","title":"Second","columns":2,"showTitle":true,"items":[]}]}"""

    @Test fun migrationPreservesIdsTargetsLabelsOrderAndEmptySection() {
        val shelf = ShelfCodec.legacy(legacy)
        assertEquals(listOf("first", "second"), shelf.sections.map { it.id })
        assertEquals(listOf("one", "two"), shelf.sections.first().items.map { it.id })
        assertEquals(listOf("test.example.alpha", "test.example.beta"), shelf.sections.first().items.map { it.reference })
        assertEquals("Alpha", shelf.sections.first().items.first().label)
        assertTrue(shelf.sections.last().items.isEmpty())
        assertEquals(shelf, ShelfCodec.current(ShelfCodec.encode(shelf)))
    }

    @Test fun unsupportedTypesAndFutureVersionsFailWholeDocument() {
        rejects(legacy.replace("\"APP\"", "\"URL\""))
        rejects(legacy.replace("\"version\":1", "\"version\":3"))
        rejects(legacy.replace("\"version\":1", "\"version\":\"1\""))
        rejects(legacy.replace("\"version\":1", "\"version\":1.0"))
    }

    @Test fun malformedObjectsAndMissingFieldsFailWholeDocument() {
        rejects(legacy.replace("\"items\":[", "\"items\":{} ,\"wrong\":["))
        rejects(legacy.replace("\"reference\":\"test.example.beta\"", "\"reference\":null"))
        rejects(legacy.replace("\"showTitle\":true", "\"showTitle\":\"true\""))
        rejects(legacy.replace("\"columns\":2", "\"columns\":2.5"))
        rejects(legacy.replace("\"label\":\"Alpha\"", "\"label\":null"))
        rejects(legacy.replace("\"items\":[{", "\"items\":[false,{"))
    }

    @Test fun duplicateIdsAndInvalidPackagesFail() {
        rejects(legacy.replace("\"id\":\"two\"", "\"id\":\"one\""))
        rejects(legacy.replace("test.example.beta", "https://example.test"))
        rejects(legacy.replace("\"id\":\"second\"", "\"id\":\"first\""))
    }

    @Test fun boundedInputAndEmptyShelf() {
        rejects(legacy + " ".repeat(ShelfCodec.MAX_BYTES))
        assertEquals(0, ShelfCodec.legacy("""{"version":1,"sections":[]}""").sections.size)
        assertEquals(0, ShelfCodec.current("""{"version":2,"sections":[]}""").sections.size)
    }

    @Test fun emptyShelfStaysEmptyUntilExplicitAdd() {
        val empty = ShelfCodec.current("""{"version":2,"sections":[]}""")
        assertTrue(empty.sections.isEmpty())
        assertEquals("apps", empty.add("apps", "test.example.alpha", null).sections.single().id)
    }

    @Test fun editsKeepOrderAndPreserveOtherSections() {
        val old = ShelfCodec.legacy(legacy)
        val added = old.add("second", "test.example.gamma", "Gamma")
        assertEquals(old.sections.first(), added.sections.first())
        assertEquals("test.example.gamma", added.sections.last().items.single().reference)
        val removed = added.remove("one")
        assertEquals(listOf("two"), removed.sections.first().items.map { it.id })
        assertEquals("two", ShelfCodec.current(ShelfCodec.encode(removed)).sections.first().items.single().id)
    }

    private fun rejects(json: String) {
        try {
            ShelfCodec.legacy(json)
            fail("Expected rejection")
        } catch (_: Exception) { /* expected */ }
    }
}
