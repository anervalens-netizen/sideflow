package eu.astancu.sideflow

import androidx.recyclerview.widget.GridLayoutManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PanelGridInstrumentedTest {
    @Test fun sectionHeaderSpansBothColumnsAndUsesOneCardDecoration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val grid = PanelGrid(context, LauncherRepository(context)) { }
        try {
            grid.show(Shelf(listOf(
                ShelfSection("one", "One", listOf(ShelfItem("a", "test.example.alpha"))),
                ShelfSection("two", "Two", emptyList())
            )))
            val layout = grid.layoutManager as GridLayoutManager
            assertEquals(2, layout.spanCount)
            assertEquals(2, layout.spanSizeLookup.getSpanSize(0))
            assertEquals(1, layout.spanSizeLookup.getSpanSize(1))
            assertEquals(2, layout.spanSizeLookup.getSpanSize(2))
            assertEquals(1, grid.itemDecorationCount)
            assertEquals(3, grid.adapter?.itemCount)
        } finally { grid.destroy() }
    }
}
