package eu.astancu.sideflow

import android.app.Application

/** Owns the shelf queue for this process; creation does not start the sidebar. */
class SideFlowApp : Application() {
    val shelfQueue: ShelfQueue by lazy {
        ShelfQueue(ShelfStore(this)) { ShortcutCleanup.run(this) }
    }
    // Accessed only from the main thread by the service and the visible editor.
    private val runtimeObservers = LinkedHashSet<() -> Unit>()
    fun observeRuntime(observer: () -> Unit) { runtimeObservers.add(observer) }
    fun removeRuntimeObserver(observer: () -> Unit) { runtimeObservers.remove(observer) }
    fun notifyRuntimeChanged() { runtimeObservers.toList().forEach { it() } }
}
