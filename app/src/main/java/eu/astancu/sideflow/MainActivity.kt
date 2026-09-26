package eu.astancu.sideflow

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.app.Activity
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val catalogWorker = Executors.newSingleThreadExecutor()
    private lateinit var root: LinearLayout
    private var shelf: Shelf? = null
    private var loadError: String? = null
    private var recoveryMessage: String? = null
    private var catalogDialog: AlertDialog? = null
    private var alive = true
    private var request = 0
    private val runtimeObserver: () -> Unit = { if (alive && ::root.isInitialized) render() }
    private val shelfObserver: (Shelf) -> Unit = { updated ->
        shelf = updated
        loadError = null
        if (alive && ::root.isInitialized) render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        setContentView(ScrollView(this).apply { addView(root) })
        render()
        load()
    }

    override fun onResume() {
        super.onResume()
        (application as SideFlowApp).observeRuntime(runtimeObserver)
        ShelfRuntime.observe(this, shelfObserver)
        ShelfRuntime.peek(this)?.let {
            shelf = it
            loadError = null
        }
        if (::root.isInitialized) render()
    }

    override fun onPause() {
        (application as SideFlowApp).removeRuntimeObserver(runtimeObserver)
        ShelfRuntime.removeObserver(this, shelfObserver)
        super.onPause()
    }

    private fun load() {
        val current = ++request
        ShelfRuntime.load(this) { result, recovery ->
            if (!alive || current != request) return@load
            recoveryMessage = recovery
            result.fold(onSuccess = {
                shelf = it
                loadError = null
            }, onFailure = {
                loadError = it.message ?: getString(R.string.shelf_error)
            })
            render()
        }
    }

    private fun render() {
        root.removeAllViews()
        heading(getString(R.string.app_name))
        val state = RuntimeState(this)
        val running = FloatingPanelService.isRunning
        info(when {
            running -> getString(R.string.sidebar_running)
            state.enabled -> getString(R.string.sidebar_waiting)
            else -> getString(R.string.sidebar_stopped)
        })
        state.error?.let(::info)
        if (!Settings.canDrawOverlays(this)) button(getString(R.string.allow_overlay)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            button(getString(R.string.allow_notification)) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        if (state.enabled) {
            button(getString(R.string.stop_sidebar)) {
                if (!ServiceControl.stop(this)) info(getString(R.string.stop_failed))
                render()
            }
            if (!running) button(getString(R.string.retry_sidebar)) {
                ServiceControl.start(this)
                render()
            }
        } else button(getString(R.string.start_sidebar)) {
            if (state.setEnabled(true)) ServiceControl.start(this)
            render()
        }
        heading(getString(R.string.handle_settings))
        val handlePrefs = HandlePreferences(this)
        slider(
            getString(R.string.handle_size),
            SidebarStyle.HANDLE_HEIGHT_MIN_DP,
            SidebarStyle.HANDLE_HEIGHT_MAX_DP,
            handlePrefs.heightDp
        ) { value ->
            handlePrefs.heightDp = value
            refreshHandle()
        }
        slider(
            getString(R.string.handle_position),
            SidebarStyle.HANDLE_OFFSET_MIN_DP,
            SidebarStyle.HANDLE_OFFSET_MAX_DP,
            handlePrefs.offsetDp
        ) { value ->
            handlePrefs.offsetDp = value
            refreshHandle()
        }
        info(getString(R.string.handle_touch_area_note, SidebarStyle.HANDLE_TOUCH_WIDTH_DP))

        loadError?.let {
            info(getString(R.string.shelf_needs_attention, it))
            button(getString(R.string.retry_load)) { load() }
            return
        }
        val current = shelf ?: run {
            info(getString(R.string.loading_shelf))
            return
        }
        recoveryMessage?.let(::info)
        button(getString(R.string.add_app)) { chooseApp() }
        current.sections.forEach { section ->
            heading(section.title)
            if (section.items.isEmpty()) info(getString(R.string.empty_section))
            section.items.forEach { item ->
                val name = item.label ?: item.reference.substringAfterLast('.')
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    minimumHeight = dp(48)
                }
                row.addView(TextView(this).apply {
                    text = name
                    textSize = 15f
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(Button(this).apply {
                    text = getString(R.string.remove)
                    contentDescription = getString(R.string.remove_description, name, section.title)
                    setOnClickListener { edit { it.remove(item.id) } }
                })
                root.addView(row)
            }
        }
    }

    private fun chooseApp() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val search = EditText(this).apply {
            hint = getString(R.string.search_apps)
            setSingleLine(true)
        }
        val list = ListView(this)
        box.addView(search)
        box.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(440)))
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_app)
            .setView(box)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        catalogDialog = dialog
        dialog.show()
        val current = mutableListOf<LauncherRepository.App>()
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        list.adapter = adapter
        var discovered: List<LauncherRepository.App> = emptyList()

        fun filter(query: String) {
            current.clear()
            current.addAll(discovered.filter {
                it.label.contains(query, true) || it.packageName.contains(query, true)
            })
            adapter.clear()
            adapter.addAll(current.map { "${it.label}\n${it.packageName}" })
            adapter.notifyDataSetChanged()
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = filter(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
        catalogWorker.execute {
            val result = runCatching { LauncherRepository(applicationContext).list() }
            runOnUiThread {
                if (!alive || !dialog.isShowing) return@runOnUiThread
                result.fold(onSuccess = {
                    discovered = it
                    filter(search.text.toString())
                    if (it.isEmpty()) search.error = getString(R.string.no_launchers)
                }, onFailure = {
                    search.error = getString(R.string.catalog_failed, it.javaClass.simpleName)
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.retry_load)) { _, _ -> chooseApp() }
                })
            }
        }
        list.setOnItemClickListener { _, _, index, _ ->
            val selected = current.getOrNull(index) ?: return@setOnItemClickListener
            dialog.dismiss()
            val sections = shelf?.sections?.takeIf { it.isNotEmpty() } ?: Shelf.empty().sections
            AlertDialog.Builder(this).setTitle(getString(R.string.section_for_app, selected.label))
                .setItems(sections.map { it.title }.toTypedArray()) { _, which ->
                    edit { it.add(sections[which].id, selected.packageName, selected.label) }
                }.show()
        }
    }

    private fun edit(change: (Shelf) -> Shelf) {
        val current = ++request
        ShelfRuntime.update(this, change) { result ->
            if (!alive || current != request) return@update
            result.fold(onSuccess = {
                shelf = it
                loadError = null
            }, onFailure = {
                loadError = getString(R.string.edit_failed, it.message ?: it.javaClass.simpleName)
            })
            render()
        }
    }

    private fun heading(value: String) {
        root.addView(TextView(this).apply {
            text = value
            textSize = 20f
            setPadding(0, dp(12), 0, dp(4))
        })
    }
    private fun info(value: String) {
        root.addView(TextView(this).apply {
            text = value
            textSize = 14f
            setPadding(0, dp(4), 0, dp(4))
        })
    }
    private fun button(value: String, click: () -> Unit) {
        root.addView(Button(this).apply {
            text = value
            setOnClickListener { click() }
        })
    }

    private fun slider(label: String, min: Int, max: Int, value: Int, changed: (Int) -> Unit) {
        val valueLabel = TextView(this).apply {
            text = "$label: ${value}dp"
            textSize = 14f
            setPadding(0, dp(6), 0, 0)
        }
        val control = SeekBar(this).apply {
            contentDescription = label
            this.max = max - min
            progress = value.coerceIn(min, max) - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                private var pending = value
                private var trackingTouch = false
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    pending = min + progress
                    valueLabel.text = "$label: ${pending}dp"
                    // Hardware-key and accessibility changes do not get onStopTrackingTouch.
                    if (fromUser && !trackingTouch) changed(pending)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) { trackingTouch = true }
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    trackingTouch = false
                    changed(pending)
                }
            })
        }
        root.addView(valueLabel)
        root.addView(control, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun refreshHandle() {
        if (!FloatingPanelService.isRunning) return
        runCatching {
            startService(Intent(this, FloatingPanelService::class.java).setAction(FloatingPanelService.ACTION_REFRESH))
        }
    }

    private fun dp(value: Int) = SidebarStyle.dp(this, value)
    override fun onDestroy() {
        alive = false
        request++
        catalogDialog?.dismiss()
        catalogWorker.shutdownNow()
        super.onDestroy()
    }
}
