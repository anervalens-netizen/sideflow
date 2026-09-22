package eu.astancu.sideflow

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class PanelAppsAdapter(
    private val context: Context,
    private val onRemove: (AppInfo) -> Unit,
    private val onAddClick: (Boolean) -> Unit,
    private val onAppLaunched: () -> Unit,
    private val onFolderClick: (String) -> Unit,
    private val onToolClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val panelPrefs = PanelPreferences(context)
    private var showAddButton: Boolean = false
    var isEditMode: Boolean = false // Expose to SidePanelView for ItemTouchHelper
    private var currentColumns: Int = SideFlowPolicy.DEFAULT_COLUMNS
    private var forceFreeform: Boolean = false
    private var compactMode: Boolean = false
    private var availablePanelWidthDp: Int = panelPrefs.panelWidthDp
    
    private var mutableApps = mutableListOf<AppInfo>()
    val currentList: List<AppInfo> get() = mutableApps

    fun submitList(list: List<AppInfo>?) {
        mutableApps = list?.toMutableList() ?: mutableListOf()
        notifyDataSetChanged()
    }

    fun submitList(list: List<AppInfo>?, commitCallback: Runnable?) {
        mutableApps = list?.toMutableList() ?: mutableListOf()
        notifyDataSetChanged()
        commitCallback?.run()
    }

    fun moveItem(from: Int, to: Int) {
        if (from < 0 || to < 0 || from >= mutableApps.size || to >= mutableApps.size) return
        if (mutableApps[from].type == AppInfo.Type.SECTION_HEADER) return
        val item = mutableApps.removeAt(from)
        val target = to.coerceIn(0, mutableApps.size)
        mutableApps.add(target, item)
        notifyItemMoved(from, target)
    }

    fun getApps(): List<AppInfo> = mutableApps.toList()

    fun getSpanSize(position: Int): Int {
        if (position >= mutableApps.size) return GRID_SPAN_COUNT
        val app = mutableApps[position]
        if (app.type == AppInfo.Type.SECTION_HEADER || compactMode) return GRID_SPAN_COUNT
        val columns = SideFlowPolicy.sanitizeColumns(app.sectionColumns ?: currentColumns)
        return GRID_SPAN_COUNT / columns
    }

    fun isMovable(position: Int): Boolean {
        if (position !in mutableApps.indices) return false
        val app = mutableApps[position]
        return SideFlowPolicy.canReorderShelfItem(
            isSectionHeader = app.type == AppInfo.Type.SECTION_HEADER,
            hasShelfItemId = app.shelfItemId != null,
            sectionId = app.sectionId
        )
    }

    fun setShowAddButton(show: Boolean) {
        if (showAddButton != show) {
            showAddButton = show
            isEditMode = show
            notifyDataSetChanged()
        }
    }

    fun setForceFreeform(force: Boolean) {
        forceFreeform = force
    }

    fun setCompactMode(compact: Boolean) {
        if (compactMode != compact) {
            compactMode = compact
            notifyDataSetChanged()
        }
    }

    fun setColumns(cols: Int) {
        if (currentColumns != cols) {
            currentColumns = cols
            notifyDataSetChanged()
        }
    }

    fun setAvailablePanelWidthDp(widthDp: Int) {
        if (availablePanelWidthDp != widthDp) {
            availablePanelWidthDp = widthDp
            notifyDataSetChanged()
        }
    }

    fun refreshIcons() {
        notifyDataSetChanged()
    }

    companion object {
        const val GRID_SPAN_COUNT = 60
        private const val VIEW_TYPE_APP = 0
        private const val VIEW_TYPE_ADD = 1
        private const val VIEW_TYPE_FOLDER = 2
        private const val VIEW_TYPE_TOOL = 3
        private const val VIEW_TYPE_SECTION = 4
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvAppName)
    }

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvSectionTitle)
        val divider: View = itemView.findViewById(R.id.sectionDivider)
    }

    inner class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAdd: ImageView = itemView.findViewById(R.id.ivAddIcon)
    }

    override fun getItemViewType(position: Int): Int {
        if (position >= mutableApps.size) return VIEW_TYPE_ADD
        return when (mutableApps[position].type) {
            AppInfo.Type.SECTION_HEADER -> VIEW_TYPE_SECTION
            AppInfo.Type.FOLDER -> VIEW_TYPE_FOLDER
            AppInfo.Type.TOOL -> VIEW_TYPE_TOOL
            else -> VIEW_TYPE_APP
        }
    }

    override fun getItemCount(): Int {
        return if (showAddButton) mutableApps.size + 1 else mutableApps.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SECTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_panel_section_header, parent, false)
                SectionViewHolder(view)
            }
            VIEW_TYPE_APP, VIEW_TYPE_FOLDER, VIEW_TYPE_TOOL -> {
                val layoutId = if (panelPrefs.uiTheme == PanelPreferences.THEME_RICH)
                    R.layout.item_panel_app_rich else R.layout.item_panel_app

                val view = LayoutInflater.from(parent.context)
                    .inflate(layoutId, parent, false)
                AppViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_panel_add, parent, false)
                AddViewHolder(view)
            }
        }
    }

    private var highlightIdentifier: String? = null

    fun highlightItem(identifier: String) {
        highlightIdentifier = identifier
        val index = currentList.indexOfFirst { it.identifier == identifier }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val scale = context.getAutoScalingFactor() * panelPrefs.scaleFactor
        val isRich = panelPrefs.uiTheme == PanelPreferences.THEME_RICH

        if (holder is SectionViewHolder) {
            val section = mutableApps.getOrNull(position) ?: return
            holder.title.text = section.appName
            val sectionContentColor = if (panelPrefs.panelUsesDarkContent()) {
                android.graphics.Color.parseColor("#CC1A1A1A")
            } else {
                android.graphics.Color.parseColor("#CCFFFFFF")
            }
            holder.title.setTextColor(sectionContentColor)
            holder.divider.backgroundTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.graphics.ColorUtils.setAlphaComponent(sectionContentColor, 64)
            )
            val visible = section.showSectionTitle && !compactMode
            val keepEditDropZone = isEditMode
            holder.title.visibility = if (visible) View.VISIBLE else View.GONE
            holder.divider.visibility = if (visible || keepEditDropZone) View.VISIBLE else View.GONE
            holder.divider.alpha = if (visible) 1f else 0.35f
            val lp = holder.itemView.layoutParams
            lp.height = if (compactMode && !keepEditDropZone) 0 else ViewGroup.LayoutParams.WRAP_CONTENT
            holder.itemView.layoutParams = lp
            holder.itemView.minimumHeight = if (keepEditDropZone) context.dpToPx(24) else 0
            holder.itemView.setPadding(
                holder.itemView.paddingLeft,
                if (visible || keepEditDropZone) context.dpToPx(6) else 0,
                holder.itemView.paddingRight,
                if (visible || keepEditDropZone) context.dpToPx(6) else 0
            )
            return
        }

        if (holder is AppViewHolder) {
            // Restore original sizes + scaling
            var baseIconSize = if (isRich) 44 else 40
            val effectiveColumns = if (compactMode) 1 else SideFlowPolicy.sanitizeColumns(
                mutableApps.getOrNull(position)?.sectionColumns ?: currentColumns
            )
            if (effectiveColumns <= 2) baseIconSize = (baseIconSize * 1.1).toInt()
            
            val baseTextSize = if (isRich) 9f else 8f

            val requestedIconDp = (baseIconSize * scale).toInt()
            val fittedIconDp = if (compactMode) {
                requestedIconDp
            } else {
                SideFlowPolicy.fitIconSizeDp(
                    panelWidthDp = availablePanelWidthDp,
                    columns = effectiveColumns,
                    requestedIconDp = requestedIconDp,
                    itemGapDp = panelPrefs.itemGapDp,
                    contentHorizontalPaddingDp = if (isRich) 8 else 0
                )
            }
            holder.ivIcon.layoutParams.let { lp ->
                lp.width = context.dpToPx(fittedIconDp)
                lp.height = context.dpToPx(fittedIconDp)
                holder.ivIcon.layoutParams = lp
            }
            holder.tvName.textSize = baseTextSize * scale
            
            val contentColor = if (panelPrefs.panelUsesDarkContent()) {
                android.graphics.Color.parseColor("#E61A1A1A")
            } else {
                android.graphics.Color.parseColor("#D9FFFFFF")
            }
            holder.tvName.setTextColor(contentColor)

            // Spacing is independent from icon scale and works consistently
            // across all supported column counts.
            val halfGapPx = context.dpToPx(panelPrefs.itemGapDp) / 2
            holder.itemView.setPadding(halfGapPx, halfGapPx, halfGapPx, halfGapPx)

            // Fetch from mutableApps so it stays synchronous with rapid dragging
            val app = if (position < mutableApps.size) mutableApps[position] else return
            
            if (app.type == AppInfo.Type.FOLDER || app.type == AppInfo.Type.TOOL || app.packageName.startsWith("sideflow.shortcut.") || (app.type == AppInfo.Type.URL && app.packageName.isBlank())) {
                Glide.with(context).clear(holder.ivIcon)
                val iconRes = when {
                    app.type == AppInfo.Type.FOLDER -> R.drawable.ic_section_tools
                    app.packageName == "sideflow.tool.screenshot" -> android.R.drawable.ic_menu_camera
                    app.packageName == "sideflow.tool.tools" -> R.drawable.ic_section_tools
                    app.packageName == "sideflow.tool.volume_up" -> R.drawable.ic_brightness_up // Using placeholders if specific ones not available
                    app.packageName == "sideflow.tool.volume_down" -> R.drawable.ic_brightness_down
                    app.packageName == "sideflow.tool.brightness_up" -> R.drawable.ic_brightness_up
                    app.packageName == "sideflow.tool.brightness_down" -> R.drawable.ic_brightness_down
                    app.packageName == "sideflow.shortcut.one_hand" -> android.R.drawable.ic_menu_crop
                    app.packageName == "sideflow.shortcut.reboot" -> android.R.drawable.ic_lock_power_off
                    app.type == AppInfo.Type.URL -> android.R.drawable.ic_menu_view
                    else -> android.R.drawable.sym_def_app_icon
                }
                
                // Specific adjustments for placeholders to look like volume
                if (app.packageName.contains("volume")) {
                    holder.ivIcon.setImageResource(R.drawable.ic_plus) // Better placeholder for +
                    if (app.packageName.endsWith("down")) holder.ivIcon.setImageResource(R.drawable.ic_minus)
                }

                holder.ivIcon.setImageResource(iconRes)
                val pseudoContent = if (panelPrefs.panelUsesDarkContent()) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
                holder.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(pseudoContent)
                holder.ivIcon.background = android.graphics.drawable.GradientDrawable().apply {
                    val chipColor = if (panelPrefs.panelUsesDarkContent()) "#14000000" else "#33FFFFFF"
                    setColor(android.graphics.Color.parseColor(chipColor))
                    cornerRadius = context.dpToPx(12).toFloat()
                }
                val pseudoPaddingDp = SideFlowPolicy.pseudoIconPaddingDp(fittedIconDp)
                val pseudoPaddingPx = context.dpToPx(pseudoPaddingDp)
                holder.ivIcon.setPadding(
                    pseudoPaddingPx,
                    pseudoPaddingPx,
                    pseudoPaddingPx,
                    pseudoPaddingPx
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    holder.ivIcon.clipToOutline = true
                }
            } else {
                Glide.with(context).clear(holder.ivIcon)
                holder.ivIcon.imageTintList = null
                holder.ivIcon.background = null
                holder.ivIcon.setPadding(0, 0, 0, 0)
                
                Glide.with(context)
                    .load(AppIconRequest(app.packageName, panelPrefs.appearanceKey))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.sym_def_app_icon)
                    .override((120 * scale).toInt(), (120 * scale).toInt())
                    .into(holder.ivIcon)
                    
                IconShapeHelper.applyShape(holder.ivIcon, panelPrefs.iconShape)
            }
                
            holder.tvName.text = app.appName

            if (app.identifier == highlightIdentifier) {
                SpringAnimator.scalePulse(holder.itemView)
                highlightIdentifier = null
            }

            holder.itemView.setOnClickListener {
                if (panelPrefs.hapticEnabled) {
                    holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                }
                SpringAnimator.scalePulse(holder.itemView)

                if (app.type == AppInfo.Type.FOLDER) {
                    onFolderClick(app.shelfItemId ?: app.identifier)
                    return@setOnClickListener
                }

                if (app.type == AppInfo.Type.TOOL) {
                    if (app.packageName == "sideflow.tool.tools") {
                        onFolderClick("sideflow.folder.tools")
                    } else {
                        onToolClick(app.packageName)
                    }
                    return@setOnClickListener
                }

                val launchIntent = when {
                    app.type == AppInfo.Type.SHORTCUT && app.packageName == "sideflow.shortcut.one_hand" -> {
                        Intent(context, PanelAccessibilityService::class.java).apply {
                            action = PanelAccessibilityService.ACTION_ONE_HANDED
                        }
                    }
                    app.type == AppInfo.Type.SHORTCUT && app.packageName == "sideflow.shortcut.reboot" -> {
                        Intent(context, PanelAccessibilityService::class.java).apply {
                            action = PanelAccessibilityService.ACTION_SHOW_POWER_MENU
                        }
                    }
                    app.type == AppInfo.Type.URL && app.intentUri != null -> {
                        Intent(Intent.ACTION_VIEW, android.net.Uri.parse(app.intentUri))
                    }
                    app.intentUri != null -> {
                        try {
                            Intent.parseUri(app.intentUri, Intent.URI_INTENT_SCHEME)
                        } catch (e: Exception) {
                            context.packageManager.getLaunchIntentForPackage(app.packageName)
                        }
                    }
                    else -> context.packageManager.getLaunchIntentForPackage(app.packageName)
                }

                if (launchIntent != null) {
                    launchIntent.addFlags(
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                    
                    val shouldFreeform = SideFlowPolicy.shouldLaunchFreeform(
                        explicitSecondaryAction = forceFreeform,
                        freeformAvailable = context.isFreeformEnabled()
                    )
                    val isAccessibilityShortcut = app.type == AppInfo.Type.SHORTCUT && 
                                               (app.packageName == "sideflow.shortcut.one_hand" || 
                                                app.packageName == "sideflow.shortcut.reboot")
                    
                    if (shouldFreeform && context.isFreeformEnabled() && app.type != AppInfo.Type.SHORTCUT) {
                        launchFreeform(launchIntent)
                    } else {
                        try {
                            if (isAccessibilityShortcut) {
                                context.startService(launchIntent)
                            } else {
                                context.startActivity(launchIntent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    // Close panel AFTER initiating launch
                    onAppLaunched()
                }
            }

            holder.itemView.setOnLongClickListener {
                if (isEditMode) {
                    return@setOnLongClickListener false // Let ItemTouchHelper handle it
                }

                if (!panelPrefs.dragToSplit) {
                    // Do nothing if drag-to-split is disabled and we're not in edit mode
                    return@setOnLongClickListener true
                }

                if (!SideFlowPolicy.canUseWindowingDrag(
                        isPlainApp = app.type == AppInfo.Type.APP,
                        hasIntentUri = app.intentUri != null
                    )
                ) {
                    // v1 deliberately avoids windowing drag for URL/deep-link/activity
                    // entries because package-only drag state would lose the real target.
                    return@setOnLongClickListener true
                }

                // Drag to Split Logic
                if (panelPrefs.hapticEnabled) {
                    holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                }
                
                val clipData = android.content.ClipData.newPlainText("pkg", app.packageName)
                val shadow = View.DragShadowBuilder(holder.ivIcon)
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    holder.itemView.startDragAndDrop(clipData, shadow, app.packageName, 0)
                } else {
                    @Suppress("DEPRECATION")
                    holder.itemView.startDrag(clipData, shadow, app.packageName, 0)
                }
                
                true
            }
        } else if (holder is AddViewHolder) {
            val baseIconSize = 40
            holder.ivAdd.layoutParams.let { lp ->
                lp.width = (context.dpToPx(baseIconSize) * scale).toInt()
                lp.height = (context.dpToPx(baseIconSize) * scale).toInt()
                holder.ivAdd.layoutParams = lp
            }

            val lightPanel = panelPrefs.panelUsesDarkContent()
            val bgTint = android.graphics.Color.parseColor(if (lightPanel) "#1F000000" else "#4DFFFFFF")
            val iconTint = if (lightPanel) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            
            holder.ivAdd.backgroundTintList = android.content.res.ColorStateList.valueOf(bgTint)
            holder.ivAdd.imageTintList = android.content.res.ColorStateList.valueOf(iconTint)

            val tvEdit = holder.itemView.findViewById<TextView>(R.id.tvEdit)
            if (tvEdit != null) {
                tvEdit.setTextColor(iconTint)
                tvEdit.textSize = 11f * scale
            }

            holder.itemView.animate().cancel()
            holder.itemView.alpha = 1f
            holder.itemView.scaleX = 1f
            holder.itemView.scaleY = 1f
            
            holder.itemView.setOnClickListener {
                if (panelPrefs.hapticEnabled) {
                    holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
                SpringAnimator.scalePulse(holder.itemView)
                onAddClick(true)
            }
        }
    }

    @android.annotation.SuppressLint("BlockedPrivateApi")
    private fun launchFreeform(intent: Intent) {
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        // Add intent extras that some OEMs/ROMs respect for freeform launching
        intent.putExtra("android.intent.extra.WINDOWING_MODE", 5)
        intent.putExtra("android.intent.extra.LAUNCH_WINDOWING_MODE", 5)
        
        try {
            val options = android.app.ActivityOptions.makeBasic()
            val displayMetrics = context.resources.displayMetrics
            val policyBounds = SideFlowPolicy.freeformBounds(
                screenWidthPx = displayMetrics.widthPixels,
                screenHeightPx = displayMetrics.heightPixels,
                mode = panelPrefs.freeformWindowMode,
                customWidthPercent = panelPrefs.freeformCustomWidth,
                customHeightPercent = panelPrefs.freeformCustomHeight
            )
            val bounds = Rect(
                policyBounds.left,
                policyBounds.top,
                policyBounds.right,
                policyBounds.bottom
            )
            options.launchBounds = bounds
            Log.d("PanelAppsAdapter", "Launching Freeform: pkg=${intent.`package`}, bounds=$bounds")

            // Use HiddenApiBypass instead of direct reflection to avoid F-Droid lint errors
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    org.lsposed.hiddenapibypass.HiddenApiBypass.invoke(
                        android.app.ActivityOptions::class.java,
                        options,
                        "setLaunchWindowingMode",
                        5
                    )
                    Log.d("PanelAppsAdapter", "HiddenApiBypass: setLaunchWindowingMode(5) success")
                }
            } catch (e: Exception) {
                Log.e("PanelAppsAdapter", "HiddenApiBypass fail: ${e.message}")
            }
            context.startActivity(intent, options.toBundle())
            Log.d("PanelAppsAdapter", "startActivity called with options")
        } catch (e: Exception) {
            context.startActivity(intent)
        }
    }


}
