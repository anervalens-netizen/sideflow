package eu.astancu.sideflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicInteger

class FloatingPanelService : Service() {
    companion object {
        const val ACTION_STOP = "eu.astancu.sideflow.STOP"
        const val ACTION_REFRESH = "eu.astancu.sideflow.REFRESH"
        private const val CHANNEL = "sideflow_sidebar"
        private const val NOTIFICATION_ID = 12
        @Volatile var isRunning = false
            private set
    }

    private lateinit var windows: WindowManager
    private lateinit var apps: LauncherRepository
    private lateinit var grid: PanelGrid
    private lateinit var handle: FrameLayout
    private lateinit var panel: FrameLayout
    private var permissionWatcher: OverlayPermissionWatcher? = null
    private var receiverRegistered = false
    private var handleAttached = false
    private var panelAttached = false
    private var backHandler: BackHandlerApi33? = null
    private val transition = AtomicInteger()
    private var loadGeneration = 0
    private var shelf = Shelf.empty()
    private val shelfObserver: (Shelf) -> Unit = { updated ->
        shelf = updated
        if (isRunning && ::grid.isInitialized) grid.show(updated)
    }
    private var lastSpace: Space? = null

    private val packages = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val target = intent.data?.schemeSpecificPart ?: return
            if (::grid.isInitialized && shelf.sections.any { section -> section.items.any { it.reference == target } }) {
                grid.packageChanged(target)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windows = getSystemService(WINDOW_SERVICE) as WindowManager
        apps = LauncherRepository(this)
        ShelfRuntime.observe(this, shelfObserver)
        permissionWatcher = OverlayPermissionWatcher(this) {
            RuntimeState(this).setError(getString(R.string.overlay_revoked))
            stopSelf()
        }.also { watcher ->
            try { watcher.start() } catch (_: RuntimeException) {
                // Start/open paths also check permission; never poll or request more access.
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(packages, filter, RECEIVER_NOT_EXPORTED)
            else registerReceiver(packages, filter)
            receiverRegistered = true
        } catch (failure: RuntimeException) {
            RuntimeState(this).setError("Package updates unavailable: ${failure.javaClass.simpleName}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = RuntimeState(this)
        if (intent?.action == ACTION_STOP) {
            if (state.setEnabled(false)) stopSelf()
            return START_NOT_STICKY
        }
        if (!state.enabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            state.setError("Overlay permission was revoked. Open SideFlow to grant it again.")
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            startForeground(NOTIFICATION_ID, notification())
        } catch (failure: RuntimeException) {
            state.setError("Foreground start failed: ${failure.javaClass.simpleName}")
            stopSelf()
            return START_NOT_STICKY
        }
        if (isRunning) {
            if (intent?.action == ACTION_REFRESH) ShelfRuntime.peek(this)?.let {
                shelf = it
                grid.show(it)
            }
            return START_STICKY
        }
        val request = ++loadGeneration
        ShelfRuntime.load(this) { result, recovery ->
            if (request != loadGeneration || !RuntimeState(this).enabled) return@load
            result.fold(onSuccess = { loaded ->
                try {
                    shelf = ShelfRuntime.peek(this) ?: loaded
                    createOverlay()
                    isRunning = true
                    state.markBoot()
                    if (recovery != null) state.setError(recovery) else state.clearError()
                    (application as SideFlowApp).notifyRuntimeChanged()
                } catch (failure: RuntimeException) {
                    state.setError("Overlay failed: ${failure.javaClass.simpleName}")
                    stopSelf()
                }
            }, onFailure = { failure ->
                state.setError("Shelf needs attention: ${failure.message ?: failure.javaClass.simpleName}")
                stopSelf()
            })
        }
        return START_STICKY
    }

    private fun notification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 1,
            Intent(this, FloatingPanelService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val editor = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.sidebar_running))
            .setContentText(getString(R.string.sidebar_notification_body))
            .setContentIntent(editor)
            .setOngoing(true)
            .addAction(0, getString(R.string.stop), stop)
            .build()
    }

    private fun createOverlay() {
        grid = PanelGrid(this, apps) { item ->
            if (apps.launch(item.reference)) closePanel()
            else Toast.makeText(this, R.string.app_unavailable, Toast.LENGTH_SHORT).show()
        }
        grid.show(shelf)
        panel = object : FrameLayout(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    closePanel()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }

            override fun performClick(): Boolean = super.performClick()

            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    closePanel()
                    return true
                }
                if (event.action == MotionEvent.ACTION_UP) {
                    performClick()
                    return true
                }
                return super.onTouchEvent(event)
            }
        }.apply {
            isFocusableInTouchMode = true
            background = GradientDrawable().apply {
                setColor(SidebarStyle.PANEL_COLOR)
                cornerRadius = SidebarStyle.dp(this@FloatingPanelService, SidebarStyle.RADIUS_DP).toFloat()
            }
            addView(grid)
        }
        handle = FrameLayout(this).apply {
            contentDescription = getString(R.string.open_sidebar)
            addView(View(this@FloatingPanelService).apply {
                background = GradientDrawable().apply {
                    setColor(0x995C687B.toInt())
                    cornerRadius = SidebarStyle.dp(this@FloatingPanelService, 3).toFloat()
                }
            }, FrameLayout.LayoutParams(SidebarStyle.dp(this@FloatingPanelService, SidebarStyle.HANDLE_STRIPE_WIDTH_DP),
                SidebarStyle.dp(this@FloatingPanelService, SidebarStyle.HANDLE_HEIGHT_DP), Gravity.RIGHT or Gravity.CENTER_VERTICAL))
            setOnClickListener { openPanel() }
            setOnTouchListener(object : View.OnTouchListener {
                var downX = 0f
                override fun onTouch(view: View, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.rawX
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (downX - event.rawX > SidebarStyle.dp(this@FloatingPanelService, 20)) openPanel()
                            else view.performClick()
                            return true
                        }
                    }
                    return true
                }
            })
        }
        windows.addView(handle, handleParams())
        handleAttached = true
        if (Build.VERSION.SDK_INT >= 29) {
            handle.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                view.systemGestureExclusionRects = listOf(Rect(0, 0, view.width, view.height))
            }
        }
        lastSpace = space()
        handle.setOnApplyWindowInsetsListener { view, insets ->
            view.post { refreshGeometry() }
            insets
        }
        panel.setOnApplyWindowInsetsListener { view, insets ->
            view.post { refreshGeometry() }
            insets
        }
    }

    private data class Space(val width: Int, val height: Int)
    private fun space(): Space {
        if (Build.VERSION.SDK_INT >= 30) {
            val metrics = windows.currentWindowMetrics
            val insets = metrics.windowInsets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
            return Space((metrics.bounds.width() - insets.left - insets.right).coerceAtLeast(1),
                (metrics.bounds.height() - insets.top - insets.bottom).coerceAtLeast(1))
        }
        val size = Point()
        @Suppress("DEPRECATION")
        windows.defaultDisplay.getSize(size)
        return Space(size.x.coerceAtLeast(1), size.y.coerceAtLeast(1))
    }

    private fun handleParams(): WindowManager.LayoutParams {
        val available = space()
        val size = SidebarStyle.dimensions(available.width, available.height,
            SidebarStyle.dp(this, SidebarStyle.HANDLE_TOUCH_WIDTH_DP),
            SidebarStyle.dp(this, SidebarStyle.HANDLE_HEIGHT_DP))
        return WindowManager.LayoutParams(size.first, size.second, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            y = SidebarStyle.handleOffset(available.height, size.second,
                SidebarStyle.dp(this@FloatingPanelService, SidebarStyle.HANDLE_OFFSET_DP))
        }
    }

    private fun panelParams(): WindowManager.LayoutParams {
        val available = space()
        val margin = SidebarStyle.dp(this, SidebarStyle.RIGHT_MARGIN_DP)
        val size = SidebarStyle.dimensions((available.width - margin).coerceAtLeast(1), available.height,
            SidebarStyle.dp(this, SidebarStyle.WIDTH_DP), SidebarStyle.dp(this, SidebarStyle.MAX_HEIGHT_DP))
        return WindowManager.LayoutParams(size.first, size.second, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            x = margin
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
    }

    private fun animationsEnabled(): Boolean =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

    private fun openPanel() {
        if (!Settings.canDrawOverlays(this)) {
            RuntimeState(this).setError("Overlay permission was revoked.")
            stopSelf()
            return
        }
        transition.incrementAndGet()
        try {
            val newlyAttached = !panelAttached
            val travel = SidebarStyle.dp(this, SidebarStyle.WIDTH_DP).toFloat()
            panel.animate().cancel()
            if (newlyAttached) {
                // Prime the reused root before attaching it so neither a stale transparent
                // close state nor the first unshifted frame can flash on screen.
                panel.translationX = travel
                panel.alpha = 1f
                windows.addView(panel, panelParams())
                panelAttached = true
            } else {
                panel.alpha = 1f
                windows.updateViewLayout(panel, panelParams())
            }
            panel.requestFocus()
            if (Build.VERSION.SDK_INT >= 33) panel.post {
                if (panelAttached && backHandler == null) backHandler = BackHandlerApi33.attach(panel) { closePanel() }
            }
            panel.animate().translationX(0f).alpha(1f)
                .setDuration(if (animationsEnabled()) SidebarStyle.ANIMATION_MS else 0L).start()
        } catch (failure: RuntimeException) {
            RuntimeState(this).setError("Overlay failed: ${failure.javaClass.simpleName}")
            stopSelf()
        }
    }

    private fun closePanel() {
        if (!panelAttached) return
        val request = transition.incrementAndGet()
        val travel = SidebarStyle.dp(this, SidebarStyle.WIDTH_DP).toFloat()
        panel.animate().cancel()
        panel.animate().translationX(travel).alpha(0f)
            .setDuration(if (animationsEnabled()) SidebarStyle.ANIMATION_MS else 0L)
            .withEndAction {
                if (request == transition.get() && panelAttached) {
                    // Commit one fully transparent frame before destroying the overlay surface.
                    // OxygenOS can otherwise briefly composite the root at its old transform while
                    // WindowManager tears the surface down, producing a visible close flicker.
                    panel.alpha = 0f
                    panel.postOnAnimation {
                        if (request == transition.get() && panelAttached) {
                            if (Build.VERSION.SDK_INT >= 33) backHandler?.unregister()
                            backHandler = null
                            try { windows.removeView(panel) } catch (_: RuntimeException) { }
                            panelAttached = false
                        }
                    }
                }
            }.start()
    }

    private fun refreshGeometry(force: Boolean = false) {
        val now = space()
        if (!force && now == lastSpace) return
        lastSpace = now
        try {
            if (handleAttached) windows.updateViewLayout(handle, handleParams())
            if (panelAttached) windows.updateViewLayout(panel, panelParams())
        } catch (failure: RuntimeException) {
            RuntimeState(this).setError("Overlay geometry failed: ${failure.javaClass.simpleName}")
            stopSelf()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshGeometry(force = true)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::apps.isInitialized) apps.trim()
    }

    override fun onDestroy() {
        loadGeneration++
        transition.incrementAndGet()
        permissionWatcher?.close()
        permissionWatcher = null
        ShelfRuntime.removeObserver(this, shelfObserver)
        if (receiverRegistered) {
            try { unregisterReceiver(packages) } catch (_: RuntimeException) { /* already unregistered */ }
        }
        if (Build.VERSION.SDK_INT >= 33) backHandler?.unregister()
        backHandler = null
        if (::panel.isInitialized) {
            panel.animate().cancel()
            if (panelAttached) try { windows.removeView(panel) } catch (_: RuntimeException) { }
        }
        if (::handle.isInitialized && handleAttached) {
            try { windows.removeView(handle) } catch (_: RuntimeException) { }
        }
        if (::grid.isInitialized) grid.destroy()
        handleAttached = false
        panelAttached = false
        isRunning = false
        (application as SideFlowApp).notifyRuntimeChanged()
        super.onDestroy()
    }
}
