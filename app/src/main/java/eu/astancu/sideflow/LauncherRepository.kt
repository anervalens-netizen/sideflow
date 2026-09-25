package eu.astancu.sideflow

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache

class LauncherRepository(private val context: Context) {
    data class App(val packageName: String, val label: String)
    private val manager = context.packageManager
    private val missing = HashSet<String>()
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    fun list(): List<App> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return manager.queryIntentActivities(intent, 0).map { App(it.activityInfo.packageName, it.loadLabel(manager).toString()) }
            .distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
    fun launch(packageName: String): Boolean {
        val intent = manager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }
        catch (_: RuntimeException) { false }
    }
    @Synchronized fun cachedIcon(packageName: String): Bitmap? = cache.get(packageName)
    @Synchronized fun isMissing(packageName: String): Boolean = missing.contains(packageName)
    fun loadIcon(packageName: String, size: Int): Bitmap? {
        if (isMissing(packageName)) return null
        val drawable: Drawable = try { manager.getApplicationIcon(packageName) }
        catch (_: PackageManager.NameNotFoundException) { return null }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }
    @Synchronized fun cacheIcon(packageName: String, bitmap: Bitmap) {
        missing.remove(packageName)
        cache.put(packageName, bitmap)
    }
    @Synchronized fun markMissing(packageName: String) { missing.add(packageName) }
    @Synchronized fun invalidate(packageName: String) {
        missing.remove(packageName)
        cache.remove(packageName)
    }
    @Synchronized fun trim() {
        cache.evictAll()
        missing.clear()
    }
}
