package eu.astancu.sideflow

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.recyclerview.widget.RecyclerView

/**
 * Draws a lightweight rounded surface behind each visible user shelf section.
 * The shelf stays a single RecyclerView/GridLayoutManager: no nested lists and
 * no extra per-item hierarchy are introduced for visual grouping.
 */
class SectionCardDecoration(
    private val horizontalInsetPx: Int,
    private val verticalInsetPx: Int,
    private val cornerRadiusPx: Float,
    private val sectionIdAt: (Int) -> String?,
    private val isEnabled: () -> Boolean,
    private val resolveColor: () -> Int
) : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val decoratedBounds = Rect()

    private data class VisibleSectionBounds(var top: Int, var bottom: Int)

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!isEnabled() || parent.childCount == 0) return

        val sections = linkedMapOf<String, VisibleSectionBounds>()
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue

            val sectionId = sectionIdAt(position) ?: continue
            if (sectionId.startsWith("__") || sectionId.startsWith("folder:")) continue

            parent.getDecoratedBoundsWithMargins(child, decoratedBounds)
            val bounds = sections[sectionId]
            if (bounds == null) {
                sections[sectionId] = VisibleSectionBounds(
                    top = decoratedBounds.top,
                    bottom = decoratedBounds.bottom
                )
            } else {
                bounds.top = minOf(bounds.top, decoratedBounds.top)
                bounds.bottom = maxOf(bounds.bottom, decoratedBounds.bottom)
            }
        }

        if (sections.isEmpty()) return

        paint.color = resolveColor()
        val left = (parent.paddingLeft + horizontalInsetPx).toFloat()
        val right = (parent.width - parent.paddingRight - horizontalInsetPx).toFloat()
        if (right <= left) return

        sections.values.forEach { bounds ->
            val top = (bounds.top + verticalInsetPx).toFloat()
            val bottom = (bounds.bottom - verticalInsetPx).toFloat()
            if (bottom <= top) return@forEach

            canvas.drawRoundRect(
                RectF(left, top, right, bottom),
                cornerRadiusPx,
                cornerRadiusPx,
                paint
            )
        }
    }
}
