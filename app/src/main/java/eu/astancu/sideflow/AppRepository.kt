package eu.astancu.sideflow

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Loads metadata for all user-installed, launchable apps from the PackageManager.
 * Icons are handled separately by Glide using the package name.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    private val panelPrefs = PanelPreferences(appContext)
    private val iconPackManager = IconPackManager(appContext)

    companion object {
        // Aggressive memory cache for fully processed static bitmap icons to ensure buttery smooth scrolling
        val iconCache = android.util.LruCache<String, android.graphics.drawable.Drawable>(300)

        fun clearSystemIconCache(packageName: String? = null) {
            iconCache.evictAll()
        }
    }

    /**
     * Loads, processes, and caches the icon for a single app.
     * Uses the "Nuclear Option" to convert Adaptive Icons to static BitmapDrawables.
     */
    fun getProcessedIcon(packageName: String, appearanceKey: String): android.graphics.drawable.Drawable? {
        val cacheKey = "pkg:$packageName|state:$appearanceKey"
        iconCache.get(cacheKey)?.let { return it }

        val rawIcon = loadIconForAppSync(packageName) ?: return null

        val processedIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && rawIcon is android.graphics.drawable.AdaptiveIconDrawable) {
            try {
                val density = appContext.resources.displayMetrics.density
                // Double the size for 2x super-sampling (ensures perfectly smooth edges)
                val size = (144 * density).toInt().coerceAtLeast(256)
                val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)

                // To draw 108dp unclipped layers into a 72dp bitmap:
                // The layers must be 1.5x larger than the bitmap.
                val layerSize = (size * 1.5f).toInt()
                val offset = (size - layerSize) / 2
                val layerBounds = android.graphics.Rect(offset, offset, offset + layerSize, offset + layerSize)

                rawIcon.background?.mutate()?.let {
                    it.bounds = layerBounds
                    it.draw(canvas)
                }
                rawIcon.foreground?.mutate()?.let {
                    it.bounds = layerBounds
                    it.draw(canvas)
                }

                android.graphics.drawable.BitmapDrawable(appContext.resources, bitmap)
            } catch (e: Exception) {
                rawIcon.mutate()
            }
        } else {
            rawIcon.mutate()
        }

        iconCache.put(cacheKey, processedIcon)
        return processedIcon
    }

    /**
     * Loads and returns the icon for a single app.
     * Synchronous version for background threads.
     */
    fun loadIconForAppSync(packageName: String): android.graphics.drawable.Drawable? {
        val selectedPack = panelPrefs.selectedIconPack
        
        // try getting the properly colored launcher icons using LauncherApps
        val systemIcon = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps
                val activityList = launcherApps?.getActivityList(packageName, android.os.Process.myUserHandle())
                if (!activityList.isNullOrEmpty()) {
                    activityList[0].getIcon(0)
                } else {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    appInfo.loadIcon(packageManager)
                }
            } else {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                appInfo.loadIcon(packageManager)
            }
        } catch (e: Exception) {
            try {
                packageManager.getApplicationIcon(packageName)
            } catch (e2: Exception) {
                null
            }
        }

        if (systemIcon == null) return null

        // Apply icon pack (with masking support)
        return iconPackManager.getThemedIcon(packageName, systemIcon, selectedPack)
    }

    /**
     * Returns all launchable apps with metadata only.
     */
    suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }

        val panelIdentifiers = panelPrefs.getPanelApps().toSet()

        val list = packageManager.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                    AppInfo(
                        packageName = pkg,
                        appName = resolveInfo.loadLabel(packageManager).toString(),
                        isInPanel = panelIdentifiers.contains(pkg),
                        type = AppInfo.Type.APP,
                        appearanceKey = panelPrefs.appearanceKey
                    )
            }
            .toMutableList()

        // Add Pseudo Shortcuts
        val oneHandPkg = "sideflow.shortcut.one_hand"
        list.add(AppInfo(oneHandPkg, "One-Handed Mode", panelIdentifiers.contains(oneHandPkg), AppInfo.Type.SHORTCUT))
        
        val sortedList = list.sortedBy { it.appName.lowercase() }

        // Aggressively pre-load icons into the memory cache in the background
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            sortedList.take(50).forEach { appInfo ->
                getProcessedIcon(appInfo.packageName, appInfo.appearanceKey ?: "")
            }
        }

        sortedList
    }

    /**
     * Returns ALL exported activities for each installed app.
     */
    suspend fun getAllActivities(): List<AppInfo> = withContext(Dispatchers.IO) {
        val panelIdentifiers = panelPrefs.getPanelApps().toSet()
        val allActivities = mutableListOf<AppInfo>()

        try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()))
            } else {
                packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)
            }

            for (pkg in packages) {
                val activities = pkg.activities ?: continue
                for (act in activities) {
                    try {
                        if (!act.exported) continue
                        
                        // Construct a URI for this specific activity
                        val intent = android.content.Intent().apply {
                            setClassName(pkg.packageName, act.name)
                        }
                        val uri = intent.toUri(android.content.Intent.URI_INTENT_SCHEME)
                        
                        allActivities.add(AppInfo(
                            packageName = pkg.packageName,
                            appName = act.loadLabel(packageManager).toString().takeIf { it.isNotBlank() } ?: act.name.substringAfterLast("."),
                            isInPanel = panelIdentifiers.contains(uri),
                            type = AppInfo.Type.ACTIVITY,
                            intentUri = uri,
                            activityName = act.name,
                            appearanceKey = panelPrefs.appearanceKey
                        ))
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        val sortedList = allActivities.sortedBy { it.appName.lowercase() }
        
        // Aggressively pre-load icons into the memory cache in the background
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            sortedList.take(50).forEach { appInfo ->
                getProcessedIcon(appInfo.packageName, appInfo.appearanceKey ?: "")
            }
        }

        return@withContext sortedList
    }

    /**
     * Resolves a list of identifiers into AppInfo objects.
     */
    suspend fun getAppsForIdentifiers(identifiers: List<String>): List<AppInfo> = withContext(Dispatchers.IO) {
        if (identifiers.isEmpty()) return@withContext emptyList()

        identifiers.mapNotNull { id ->
            when {
                id == "sideflow.shortcut.one_hand" -> {
                    AppInfo(id, "One-Handed Mode", true, AppInfo.Type.SHORTCUT, appearanceKey = panelPrefs.appearanceKey)
                }
                id == "sideflow.shortcut.reboot" -> {
                    AppInfo(id, "Power Menu", true, AppInfo.Type.SHORTCUT, appearanceKey = panelPrefs.appearanceKey)
                }
                id.startsWith("sideflow.folder.") -> {
                    val name = id.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                    AppInfo(id, name, true, AppInfo.Type.FOLDER, appearanceKey = panelPrefs.appearanceKey)
                }
                id.startsWith("sideflow.tool.") -> {
                    val name = id.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                    AppInfo(id, name, true, AppInfo.Type.TOOL, appearanceKey = panelPrefs.appearanceKey)
                }
                id.startsWith("intent:") -> {
                    try {
                        val intent = android.content.Intent.parseUri(id, android.content.Intent.URI_INTENT_SCHEME)
                        val pkg = intent.getPackage() ?: intent.component?.packageName ?: ""
                        
                        val resolveInfo = packageManager.resolveActivity(intent, 0)
                        val name = resolveInfo?.loadLabel(packageManager)?.toString() 
                                   ?: intent.component?.shortClassName?.substringAfterLast(".")
                                   ?: "Unknown Activity"

                        AppInfo(
                            packageName = pkg,
                            appName = name,
                            isInPanel = true,
                            type = AppInfo.Type.ACTIVITY,
                            intentUri = id,
                            activityName = intent.component?.className,
                            appearanceKey = panelPrefs.appearanceKey
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                else -> {
                    try {
                        val appInfo = packageManager.getApplicationInfo(id, 0)
                        AppInfo(
                            packageName = id,
                            appName = packageManager.getApplicationLabel(appInfo).toString(),
                            isInPanel = true,
                            type = AppInfo.Type.APP,
                            appearanceKey = panelPrefs.appearanceKey
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
    }

    /**
     * Returns the persisted shelf as a flattened stream of section headers and
     * resolved items. The adapter uses a 60-span grid so each section can keep
     * its own 2–6 column density without nested RecyclerViews.
     */
    suspend fun getPanelApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val config = panelPrefs.getShelfConfig()
        val entries = mutableListOf<AppInfo>()

        if (panelPrefs.showNotificationApps) {
            val pinned = ShelfConfigOps.allItems(config).map { it.reference }.toSet()
            val notificationPackages = NotificationTrackingService
                .getActiveNotificationPackages()
                .filterNot { it in pinned }

            if (notificationPackages.isNotEmpty()) {
                val notificationSection = ShelfSection(
                    id = "__notifications__",
                    title = "Notifications",
                    columns = SideFlowPolicy.DEFAULT_COLUMNS,
                    showTitle = true
                )
                entries += sectionHeader(notificationSection)
                notificationPackages.forEach { pkg ->
                    resolveShelfItem(
                        ShelfItem(
                            id = "notification:$pkg",
                            type = ShelfItemType.APP,
                            reference = pkg
                        ),
                        notificationSection,
                        persisted = false
                    )?.let(entries::add)
                }
            }
        }

        config.sections.forEach { section ->
            entries += sectionHeader(section)
            section.items.forEach { item ->
                resolveShelfItem(item, section, persisted = true)?.let(entries::add)
            }
        }

        entries
    }

    suspend fun getFolderItems(folderItemId: String): List<AppInfo> = withContext(Dispatchers.IO) {
        val config = panelPrefs.getShelfConfig()
        val folder = ShelfConfigOps.findItem(config, folderItemId) ?: return@withContext emptyList()
        if (folder.type != ShelfItemType.FOLDER) return@withContext emptyList()

        val section = ShelfSection(
            id = "folder:${folder.id}",
            title = folder.label ?: "Folder",
            columns = SideFlowPolicy.DEFAULT_COLUMNS,
            showTitle = false,
            items = folder.children
        )

        folder.children.mapNotNull { child ->
            // Nested folders must keep their stable shelf IDs so opening a child
            // folder can resolve it recursively from the canonical shelf model.
            resolveShelfItem(child, section, persisted = true)
        }
    }

    private fun sectionHeader(section: ShelfSection): AppInfo =
        AppInfo(
            packageName = "sideflow.section.${section.id}",
            appName = section.title,
            isInPanel = true,
            type = AppInfo.Type.SECTION_HEADER,
            appearanceKey = panelPrefs.appearanceKey,
            sectionId = section.id,
            sectionColumns = section.columns,
            showSectionTitle = section.showTitle
        )

    private fun resolveShelfItem(
        item: ShelfItem,
        section: ShelfSection,
        persisted: Boolean
    ): AppInfo? {
        val common = { info: AppInfo ->
            info.copy(
                shelfItemId = item.id.takeIf { persisted },
                sectionId = section.id,
                sectionColumns = section.columns,
                showSectionTitle = section.showTitle
            )
        }

        return when (item.type) {
            ShelfItemType.APP -> {
                try {
                    val applicationInfo = packageManager.getApplicationInfo(item.reference, 0)
                    common(
                        AppInfo(
                            packageName = item.reference,
                            appName = item.label
                                ?: packageManager.getApplicationLabel(applicationInfo).toString(),
                            isInPanel = true,
                            type = AppInfo.Type.APP,
                            appearanceKey = panelPrefs.appearanceKey
                        )
                    )
                } catch (_: Exception) {
                    null
                }
            }

            ShelfItemType.DEEP_LINK -> {
                try {
                    val intent = android.content.Intent.parseUri(
                        item.reference,
                        android.content.Intent.URI_INTENT_SCHEME
                    )
                    val pkg = item.iconPackage
                        ?: intent.getPackage()
                        ?: intent.component?.packageName
                        ?: ""
                    val resolveInfo = packageManager.resolveActivity(intent, 0)
                    common(
                        AppInfo(
                            packageName = pkg,
                            appName = item.label
                                ?: resolveInfo?.loadLabel(packageManager)?.toString()
                                ?: intent.component?.shortClassName?.substringAfterLast(".")
                                ?: "Shortcut",
                            isInPanel = true,
                            type = AppInfo.Type.ACTIVITY,
                            intentUri = item.reference,
                            activityName = intent.component?.className,
                            appearanceKey = panelPrefs.appearanceKey
                        )
                    )
                } catch (_: Exception) {
                    null
                }
            }

            ShelfItemType.URL -> {
                val uri = runCatching { android.net.Uri.parse(item.reference) }.getOrNull()
                    ?: return null
                val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                val resolvedPackage = item.iconPackage
                    ?: packageManager.resolveActivity(viewIntent, 0)?.activityInfo?.packageName
                    ?: ""
                common(
                    AppInfo(
                        packageName = resolvedPackage,
                        appName = item.label ?: uri.host ?: "Link",
                        isInPanel = true,
                        type = AppInfo.Type.URL,
                        intentUri = item.reference,
                        appearanceKey = panelPrefs.appearanceKey
                    )
                )
            }

            ShelfItemType.FOLDER -> common(
                AppInfo(
                    packageName = item.reference,
                    appName = item.label ?: "Folder",
                    isInPanel = true,
                    type = AppInfo.Type.FOLDER,
                    subItems = item.children.map { it.id },
                    appearanceKey = panelPrefs.appearanceKey
                )
            )

            ShelfItemType.SYSTEM_ACTION -> {
                val type = if (item.reference.startsWith("sideflow.tool.")) {
                    AppInfo.Type.TOOL
                } else {
                    AppInfo.Type.SHORTCUT
                }
                val fallbackName = item.reference
                    .substringAfterLast(".")
                    .replace("_", " ")
                    .replaceFirstChar { it.uppercase() }
                common(
                    AppInfo(
                        packageName = item.reference,
                        appName = item.label ?: fallbackName,
                        isInPanel = true,
                        type = type,
                        appearanceKey = panelPrefs.appearanceKey
                    )
                )
            }
        }
    }

    suspend fun getTop5Apps(): List<String> = withContext(Dispatchers.IO) {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        packageManager.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .take(5)
            .map { it.activityInfo.packageName }
    }
}
