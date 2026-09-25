package eu.astancu.sideflow

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ShelfQueueInstrumentedTest {
    @Test fun committedEditPublishesEvenWhenEditorCallbackIsDiscarded() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File(base.filesDir, "synthetic-queue-test").apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                base.getSharedPreferences("synthetic_queue_$name", mode)
        }
        val dir = context.filesDir
        dir.listFiles()?.forEach { it.delete() }
        context.getSharedPreferences("side_panel_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        val store = ShelfStore(context)
        val queue = ShelfQueue(store)
        val observed = CountDownLatch(1)
        var runtime: Shelf? = null
        val listener: (Shelf) -> Unit = { runtime = it; observed.countDown() }
        queue.observe(listener)
        val editorAlive = false
        val uiUpdates = AtomicInteger()
        try {
            queue.update({ it.add("apps", "test.example.alpha", "Alpha") }) { result ->
                if (editorAlive && result.isSuccess) uiUpdates.incrementAndGet()
            }
            assertTrue(observed.await(10, TimeUnit.SECONDS))
            assertEquals("test.example.alpha", runtime?.sections?.single()?.items?.single()?.reference)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(0, uiUpdates.get())
            assertEquals(runtime, ShelfStore(context).load())
        } finally {
            queue.removeObserver(listener)
            queue.close()
            dir.listFiles()?.forEach { it.delete() }
            context.getSharedPreferences("side_panel_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
