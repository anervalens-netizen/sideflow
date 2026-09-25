package eu.astancu.sideflow

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Uses the test APK's isolated data directory; never touches the target app's real shelf. */
@RunWith(AndroidJUnit4::class)
class ShelfStoreInstrumentedTest {
    private lateinit var context: Context
    private val filename = "minimal-shelf-v2.json"
    private val goodName = "minimal-shelf-v2-good.json"
    private val legacy = """{"version":1,"sections":[{"id":"s1","title":"Synthetic","columns":2,"showTitle":true,"items":[{"id":"i1","type":"APP","reference":"test.example.alpha","label":"Alpha"}]}]}"""

    @Before fun setup() {
        context = IsolatedContext(InstrumentationRegistry.getInstrumentation().targetContext)
        clear()
    }
    @After fun teardown() { clear() }
    private fun clear() {
        for (name in listOf(filename, goodName)) {
            File(context.filesDir, name).delete()
            File(context.filesDir, "$name.bak").delete()
            File(context.filesDir, "$name.new").delete()
        }
        context.getSharedPreferences("side_panel_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun file(name: String) = File(context.filesDir, name)

    private class IsolatedContext(base: Context) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = File(baseContext.filesDir, "synthetic-shelf-tests").apply { mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            baseContext.getSharedPreferences("synthetic_$name", mode)
    }

    @Test fun migrateEditPersistAndKeepLegacyUntouched() {
        val prefs = context.getSharedPreferences("side_panel_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("shelf_config_v1", legacy).commit()
        val first = ShelfStore(context).load()
        assertEquals("i1", first.sections.single().items.single().id)
        assertEquals(legacy, prefs.getString("shelf_config_v1", null))
        ShelfStore(context).update { it.add("s1", "test.example.beta", "Beta") }
        assertEquals(listOf("i1", "test.example.beta"), ShelfStore(context).load().sections.single().items.mapIndexed { index, item -> if (index == 0) item.id else item.reference })
        assertEquals(legacy, prefs.getString("shelf_config_v1", null))
    }

    @Test fun missingPrimaryUsesGoodInsteadOfReseedingLegacy() {
        file(goodName).writeText(ShelfCodec.encode(Shelf.empty().add("apps", "test.example.good", "Good")))
        context.getSharedPreferences("side_panel_prefs", Context.MODE_PRIVATE).edit().putString("shelf_config_v1", legacy).commit()
        assertEquals("test.example.good", ShelfStore(context).load().sections.single().items.single().reference)
    }

    @Test fun interruptedPrimaryWriteUsesAtomicBackup() {
        file("$filename.bak").writeText(ShelfCodec.encode(Shelf.empty().add("apps", "test.example.backup", null)))
        assertEquals("test.example.backup", ShelfStore(context).load().sections.single().items.single().reference)
    }

    @Test fun corruptedPrimaryRecoversGoodAndBothBadFailClosed() {
        file(filename).writeText("broken")
        file(goodName).writeText(ShelfCodec.encode(Shelf.empty()))
        assertEquals(Shelf.empty(), ShelfStore(context).load())
        file(goodName).writeText("broken")
        try { ShelfStore(context).load(); fail("Expected corruption") } catch (_: IllegalStateException) { }
    }

    @Test fun interruptedFirstWriteWithoutGoodFailsClosed() {
        file("$filename.new").writeText("partial")
        context.getSharedPreferences("side_panel_prefs", Context.MODE_PRIVATE).edit().putString("shelf_config_v1", legacy).commit()
        try { ShelfStore(context).load(); fail("Expected interrupted write failure") } catch (_: IllegalStateException) { }
        assertFalse(file(filename).exists())
    }

    @Test fun twentyConcurrentEditsSerializeWithoutLostItems() {
        val store = ShelfStore(context)
        val executor = Executors.newFixedThreadPool(4)
        val done = CountDownLatch(20)
        repeat(20) { index ->
            executor.execute {
                try { store.update { it.add("apps", "test.example.app$index", "Item $index") } }
                finally { done.countDown() }
            }
        }
        assertTrue(done.await(10, TimeUnit.SECONDS))
        executor.shutdownNow()
        assertEquals(20, ShelfStore(context).load().sections.single().items.size)
    }

    @Test fun unsupportedLegacyNeverWritesMinimalStore() {
        context.getSharedPreferences("side_panel_prefs", Context.MODE_PRIVATE).edit()
            .putString("shelf_config_v1", legacy.replace("\"APP\"", "\"URL\"")).commit()
        try { ShelfStore(context).load(); fail("Expected unsupported shortcut") } catch (_: IllegalArgumentException) { }
        assertFalse(file(filename).exists())
        assertFalse(file(goodName).exists())
    }
}
