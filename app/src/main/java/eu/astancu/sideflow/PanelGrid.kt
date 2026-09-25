package eu.astancu.sideflow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/** Reused grid. Section cards are drawn behind a full-width header and two-column items. */
class PanelGrid(context: Context, private val apps: LauncherRepository, private val onLaunch: (ShelfItem) -> Unit) : RecyclerView(context) {
    private sealed interface Row {
        data class Header(val title: String) : Row
        data class Shortcut(val item: ShelfItem) : Row
    }
    private val rows = mutableListOf<Row>()
    private val sections = mutableListOf<IntRange>()
    private val worker = Executors.newSingleThreadExecutor()
    private val requests = IconRequestPolicy()
    private val grid = GridLayoutManager(context, 2)
    private val iconSize = SidebarStyle.dp(context, SidebarStyle.ICON_DP)
    private val cellGap = SidebarStyle.dp(context, SidebarStyle.CELL_GAP_DP)

    init {
        layoutManager = grid
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int = if (rows.getOrNull(position) is Row.Header) 2 else 1
        }
        adapter = Cells()
        addItemDecoration(SectionCards())
        setPadding(SidebarStyle.dp(context, 8), SidebarStyle.dp(context, 2), SidebarStyle.dp(context, 8), SidebarStyle.dp(context, 2))
        clipToPadding = false
        overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
    }

    fun show(shelf: Shelf) {
        requests.newData()
        rows.clear()
        sections.clear()
        shelf.sections.forEach { section ->
            val first = rows.size
            rows.add(Row.Header(section.title))
            rows.addAll(section.items.map(Row::Shortcut))
            sections.add(first..rows.lastIndex)
        }
        adapter?.notifyDataSetChanged()
    }

    fun packageChanged(packageName: String) {
        requests.invalidate(packageName)
        apps.invalidate(packageName)
        rows.forEachIndexed { index, row ->
            if (row is Row.Shortcut && row.item.reference == packageName) adapter?.notifyItemChanged(index)
        }
    }

    fun destroy() {
        requests.destroy()
        worker.shutdownNow()
    }

    private inner class SectionCards : ItemDecoration() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SidebarStyle.CARD_COLOR }
        private val bounds = Rect()
        private val radius = SidebarStyle.dp(context, 10).toFloat()
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
            val position = parent.getChildAdapterPosition(view)
            if (position >= 0 && rows.getOrNull(position) is Row.Header) outRect.top = SidebarStyle.dp(context, SidebarStyle.SECTION_GAP_DP)
        }
        override fun onDraw(canvas: Canvas, parent: RecyclerView, state: State) {
            sections.forEach { section ->
                var top = Int.MAX_VALUE
                var bottom = Int.MIN_VALUE
                for (index in 0 until parent.childCount) {
                    val child = parent.getChildAt(index)
                    val position = parent.getChildAdapterPosition(child)
                    if (position in section) {
                        parent.getDecoratedBoundsWithMargins(child, bounds)
                        val visibleTop = if (position == section.first) child.top else bounds.top
                        top = minOf(top, visibleTop)
                        bottom = maxOf(bottom, bounds.bottom)
                    }
                }
                if (top <= bottom) {
                    canvas.drawRoundRect(parent.paddingLeft.toFloat(), top.toFloat(),
                        (parent.width - parent.paddingRight).toFloat(), bottom.toFloat(), radius, radius, paint)
                }
            }
        }
    }

    private inner class Cells : Adapter<Cell>() {
        override fun getItemCount() = rows.size
        override fun getItemViewType(position: Int) = if (rows[position] is Row.Header) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): Cell {
            if (type == 0) {
                val title = TextView(context).apply {
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    minHeight = SidebarStyle.dp(context, SidebarStyle.HEADER_HEIGHT_DP)
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(SidebarStyle.dp(context, 8), 0, 0, 0)
                }
                return Cell(title)
            }
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                minimumHeight = SidebarStyle.dp(context, SidebarStyle.CELL_MIN_HEIGHT_DP)
                setPadding(cellGap, 0, cellGap, cellGap)
            }
            column.addView(ImageView(context).apply { id = android.R.id.icon }, LinearLayout.LayoutParams(iconSize, iconSize))
            column.addView(TextView(context).apply {
                id = android.R.id.text1
                setTextColor(Color.WHITE)
                textSize = 10f
                gravity = Gravity.CENTER
                maxLines = 2
                includeFontPadding = false
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return Cell(column)
        }

        override fun onBindViewHolder(holder: Cell, position: Int) {
            when (val row = rows[position]) {
                is Row.Header -> (holder.itemView as TextView).text = row.title
                is Row.Shortcut -> bindShortcut(holder, row.item)
            }
        }

        private fun bindShortcut(holder: Cell, item: ShelfItem) {
            val name = item.label ?: item.reference.substringAfterLast('.')
            val icon = holder.itemView.findViewById<ImageView>(android.R.id.icon)
            val label = holder.itemView.findViewById<TextView>(android.R.id.text1)
            label.text = name
            holder.itemView.contentDescription = context.getString(R.string.open_app, name)
            holder.itemView.setOnClickListener { onLaunch(item) }
            icon.setImageResource(android.R.drawable.sym_def_app_icon)
            icon.alpha = 0.7f
            if (apps.isMissing(item.reference)) {
                holder.itemView.contentDescription = context.getString(R.string.unavailable_app, name)
                return
            }
            val cached = apps.cachedIcon(item.reference)
            if (cached != null) {
                icon.setImageBitmap(cached)
                icon.alpha = 1f
                return
            }
            val ticket = requests.ticket(item.reference)
            try {
                worker.execute {
                    if (!requests.current(ticket)) return@execute
                    val bitmap: Bitmap? = try { apps.loadIcon(item.reference, iconSize) } catch (_: RuntimeException) { null }
                    post {
                        if (!requests.current(ticket)) return@post
                        if (bitmap != null) apps.cacheIcon(item.reference, bitmap)
                        else apps.markMissing(item.reference)
                        val adapterPosition = holder.bindingAdapterPosition
                        val bound = rows.getOrNull(adapterPosition) as? Row.Shortcut
                        if (bound?.item?.id == item.id) {
                            if (bitmap != null) {
                                icon.setImageBitmap(bitmap)
                                icon.alpha = 1f
                            } else {
                                holder.itemView.contentDescription = context.getString(R.string.unavailable_app, name)
                            }
                        }
                    }
                }
            } catch (_: RejectedExecutionException) {
                // Destroy can race with RecyclerView's last bind.
            }
        }
    }
    private class Cell(view: View) : ViewHolder(view)
}
