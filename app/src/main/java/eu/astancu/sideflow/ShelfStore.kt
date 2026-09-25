package eu.astancu.sideflow

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.CopyOnWriteArraySet

/** One serialized writer with an atomic current copy and a last-known-good copy. */
class ShelfStore(context: Context) {
    private val app = context.applicationContext
    private val primary = AtomicFile(File(app.filesDir, "minimal-shelf-v2.json"))
    private val good = AtomicFile(File(app.filesDir, "minimal-shelf-v2-good.json"))
    @Volatile private var snapshot: Shelf? = null
    @Volatile var recoveryMessage: String? = null
        private set

    @Synchronized fun peek(): Shelf? = snapshot

    @Synchronized fun load(): Shelf {
        snapshot?.let { return it }
        val currentExists = exists(primary)
        val goodExists = exists(good)
        val value = when {
            currentExists -> {
                try {
                    ShelfCodec.current(read(primary))
                } catch (failure: Exception) {
                    if (!goodExists) throw IllegalStateException("Shelf is damaged: ${failure.message}", failure)
                    val recovered = try { ShelfCodec.current(read(good)) }
                        catch (other: Exception) { throw IllegalStateException("Both shelf copies are damaged", other) }
                    recoveryMessage = "Recovered the last good shelf; check recent edits."
                    recovered
                }
            }
            goodExists -> {
                recoveryMessage = "Recovered the shelf after an interrupted write."
                try { ShelfCodec.current(read(good)) }
                catch (failure: Exception) { throw IllegalStateException("Recovery shelf is damaged", failure) }
            }
            hasPending(primary) || hasPending(good) -> throw IllegalStateException("Interrupted shelf write has no valid recovery copy")
            else -> migrate()
        }
        snapshot = value
        return value
    }

    @Synchronized fun update(change: (Shelf) -> Shelf): Shelf {
        val before = load()
        val after = change(before)
        val encoded = ShelfCodec.encode(after)
        if (after == before) return before
        write(good, ShelfCodec.encode(before))
        write(primary, encoded)
        snapshot = after
        return after
    }

    private fun migrate(): Shelf {
        val legacy = app.getSharedPreferences("side_panel_prefs", Context.MODE_PRIVATE)
        val value = when {
            legacy.contains("shelf_config_v1") -> ShelfCodec.legacy(
                legacy.getString("shelf_config_v1", null) ?: throw IllegalStateException("Missing legacy shelf"))
            legacy.contains("panel_apps") -> {
                val raw = legacy.getString("panel_apps", null) ?: throw IllegalStateException("Invalid legacy apps")
                val packages = if (raw.isEmpty()) emptyList() else raw.split(',')
                require(packages.size <= 256 && packages.all(Shelf.PACKAGE::matches) && packages.distinct().size == packages.size) { "Invalid legacy apps" }
                Shelf(listOf(ShelfSection("apps", "Apps", packages.map { packageName ->
                    ShelfItem(UUID.nameUUIDFromBytes(packageName.toByteArray()).toString(), packageName)
                })))
            }
            else -> Shelf.empty()
        }
        val encoded = ShelfCodec.encode(value)
        // The good copy is created first, so a failed first primary write cannot reseed legacy data.
        write(good, encoded)
        write(primary, encoded)
        return value
    }

    private fun exists(file: AtomicFile): Boolean = file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()
    private fun hasPending(file: AtomicFile): Boolean = File(file.baseFile.path + ".new").exists()

    private fun read(file: AtomicFile): String = file.openRead().use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= ShelfCodec.MAX_BYTES) { "Shelf exceeds size limit" }
            output.write(buffer, 0, count)
        }
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(output.toByteArray())).toString()
    }

    private fun write(file: AtomicFile, value: String) {
        val output = file.startWrite()
        try {
            output.write(value.toByteArray(StandardCharsets.UTF_8))
            file.finishWrite(output)
        } catch (failure: Exception) {
            file.failWrite(output)
            throw failure
        }
    }
}

/** A serial queue publishes committed snapshots before any editor UI callback. */
class ShelfQueue(private val target: ShelfStore, private val afterLoad: () -> Unit = {}) {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val observers = CopyOnWriteArraySet<(Shelf) -> Unit>()

    fun peek(): Shelf? = target.peek()
    fun observe(observer: (Shelf) -> Unit) { observers.add(observer) }
    fun removeObserver(observer: (Shelf) -> Unit) { observers.remove(observer) }

    fun load(callback: (Result<Shelf>, String?) -> Unit) {
        worker.execute {
            val result = runCatching { target.load() }
            if (result.isSuccess) runCatching { afterLoad() }
            main.post { callback(result, target.recoveryMessage) }
        }
    }

    fun update(change: (Shelf) -> Shelf, callback: (Result<Shelf>) -> Unit) {
        worker.execute {
            val result = runCatching { target.update(change) }
            main.post {
                result.getOrNull()?.let { shelf -> observers.forEach { it(shelf) } }
                callback(result)
            }
        }
    }

    fun close() {
        worker.shutdownNow()
        observers.clear()
    }
}

object ShelfRuntime {
    private fun queue(context: Context): ShelfQueue = (context.applicationContext as SideFlowApp).shelfQueue
    fun peek(context: Context): Shelf? = queue(context).peek()
    fun observe(context: Context, observer: (Shelf) -> Unit) { queue(context).observe(observer) }
    fun removeObserver(context: Context, observer: (Shelf) -> Unit) { queue(context).removeObserver(observer) }
    fun load(context: Context, callback: (Result<Shelf>, String?) -> Unit) = queue(context).load(callback)
    fun update(context: Context, change: (Shelf) -> Shelf, callback: (Result<Shelf>) -> Unit) =
        queue(context).update(change, callback)
}
