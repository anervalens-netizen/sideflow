package eu.astancu.sideflow

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

object SplitScreenHelper {
    private const val TAG = "SplitScreenHelper"

    // Windowing modes from Android Source
    private const val WINDOWING_MODE_FULLSCREEN = 1
    private const val WINDOWING_MODE_PINNED = 2
    private const val WINDOWING_MODE_SPLIT_SCREEN_PRIMARY = 3
    private const val WINDOWING_MODE_SPLIT_SCREEN_SECONDARY = 4
    private const val WINDOWING_MODE_FREEFORM = 5

    fun launchApp(context: Context, packageName: String, mode: Int) {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Log.w(TAG, "No launch intent for $packageName")
            return
        }

        val effectiveMode = if (
            mode == WINDOWING_MODE_FREEFORM &&
            !context.isFreeformEnabled()
        ) {
            Log.w(TAG, "Freeform unavailable; falling back to fullscreen for $packageName")
            WINDOWING_MODE_FULLSCREEN
        } else {
            mode
        }

        if (effectiveMode == WINDOWING_MODE_FULLSCREEN) {
            launchFullscreen(context, launchIntent, packageName)
            return
        }

        // Critical flags for explicit multi-window actions only.
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

        val isSplit = effectiveMode == WINDOWING_MODE_SPLIT_SCREEN_PRIMARY ||
            effectiveMode == WINDOWING_MODE_SPLIT_SCREEN_SECONDARY

        if (isSplit) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        }

        val options = ActivityOptions.makeBasic()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                HiddenApiBypass.invoke(
                    ActivityOptions::class.java,
                    options,
                    "setLaunchWindowingMode",
                    effectiveMode
                )
            }

            launchIntent.putExtra("android.intent.extra.WINDOWING_MODE", effectiveMode)
            launchIntent.putExtra("android.intent.extra.LAUNCH_WINDOWING_MODE", effectiveMode)

            val dm = context.resources.displayMetrics
            val w = dm.widthPixels
            val h = dm.heightPixels

            val rect = when (effectiveMode) {
                WINDOWING_MODE_SPLIT_SCREEN_PRIMARY -> Rect(0, 0, w, h / 2)
                WINDOWING_MODE_SPLIT_SCREEN_SECONDARY -> Rect(0, h / 2, w, h)
                WINDOWING_MODE_FREEFORM -> {
                    val prefs = PanelPreferences(context)
                    val bounds = SideFlowPolicy.freeformBounds(
                        screenWidthPx = w,
                        screenHeightPx = h,
                        mode = prefs.freeformWindowMode,
                        customWidthPercent = prefs.freeformCustomWidth,
                        customHeightPercent = prefs.freeformCustomHeight
                    )
                    Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)
                }
                else -> null
            }

            if (rect != null) {
                options.launchBounds = rect
            }

            Log.d(TAG, "Launching $packageName: mode=$effectiveMode, bounds=$rect")
            context.startActivity(launchIntent, options.toBundle())
        } catch (e: Exception) {
            Log.e(TAG, "Windowed launch failed; falling back to fullscreen", e)
            launchFullscreen(context, launchIntent, packageName)
        }
    }

    private fun launchFullscreen(context: Context, sourceIntent: Intent, packageName: String) {
        val fullscreenIntent = Intent(sourceIntent).apply {
            removeFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            removeFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            removeExtra("android.intent.extra.WINDOWING_MODE")
            removeExtra("android.intent.extra.LAUNCH_WINDOWING_MODE")
        }

        try {
            context.startActivity(fullscreenIntent)
        } catch (e: Exception) {
            // A helper launch must never crash the accessibility/service path.
            Log.e(TAG, "Fullscreen launch failed for $packageName", e)
        }
    }

    const val MODE_FULLSCREEN = WINDOWING_MODE_FULLSCREEN
    const val MODE_TOP = WINDOWING_MODE_SPLIT_SCREEN_PRIMARY
    const val MODE_BOTTOM = WINDOWING_MODE_SPLIT_SCREEN_SECONDARY
    const val MODE_FREEFORM = WINDOWING_MODE_FREEFORM
}
