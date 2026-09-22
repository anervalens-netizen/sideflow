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
 *
 * Draw-time state is reused across frames to avoid allocation/lookup churn
 * while scrolling or animating.
 */
class SectionCardDecoration(
    private val horizontalInsetPx: Int,
    private val verticalInsetPx: Int,
    private val cornerRadiusPx: Float,
    private val sectionIdAt: (Int) -> String?,
    initialEnabled: Boolean,
    initialColor: Int
) : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = initialColor }
    private val decoratedBounds = Rect()
    private val cardRect = RectF()
    private val boundsBySection = linkedMapOf<String, VisibleSectionBounds>()
    private val activeSectionIds = ArrayList<String>()
    private var frameId: Long = 0L
    private var enabled: Boolean = initialEnabled

    private data class VisibleSectionBounds(
        var top: Int = 0,
        var bottom: Int = 0,
        var frameId: Long = Long.MIN_VALUE
    )

    fun updateStyle(enabled: Boolean, color: Int): Boolean {
        val changed = this.enabled != enabled || paint.color != color
        this.enabled = enabled
        paint.color = color
        return changed
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!enabled || parent.childCount == 0) return

        frameId += 1L
        activeSectionIds.clear()

        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue

            val sectionId = sectionIdAt(position) ?: continue
            if (sectionId.startsWith("__") || sectionId.startsWith("folder:")) continue

            parent.getDecoratedBoundsWithMargins(child, decoratedBounds)
            val bounds = boundsBySection[sectionId]
                ?: VisibleSectionBounds().also { boundsBySection[sectionId] = it }

            if (bounds.frameId != frameId) {
                bounds.top = decoratedBounds.top
                bounds.bottom = decoratedBounds.bottom
                bounds.frameId = frameId
                activeSectionIds.add(sectionId)
            } else {
                bounds.top = minOf(bounds.top, decoratedBounds.top)
                bounds.bottom = maxOf(bounds.bottom, decoratedBounds.bottom)
            }
        }

        if (activeSectionIds.isEmpty()) return

        val left = (parent.paddingLeft + horizontalInsetPx).toFloat()
        val right = (parent.width - parent.paddingRight - horizontalInsetPx).toFloat()
        if (right <= left) return

        activeSectionIds.forEach { sectionId ->
            val bounds = boundsBySection[sectionId] ?: return@forEach
            val top = (bounds.top + verticalInsetPx).toFloat()
            val bottom = (bounds.bottom - verticalInsetPx).toFloat()
            if (bottom <= top) return@forEach

            cardRect.set(left, top, right, bottom)
            canvas.drawRoundRect(cardRect, cornerRadiusPx, cornerRadiusPx, paint)
        }
    }
}
