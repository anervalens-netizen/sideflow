package eu.astancu.sideflow

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Tickets keep old package loads from publishing after a refresh. Closing does not expire tickets. */
class IconRequestPolicy {
    data class Ticket(val data: Int, val target: String, val packageVersion: Int)
    private val dataVersion = AtomicInteger()
    private val packageVersions = ConcurrentHashMap<String, Int>()
    @Volatile private var destroyed = false

    fun newData() { dataVersion.incrementAndGet() }
    fun invalidate(target: String) { packageVersions.merge(target, 1, Int::plus) }
    fun ticket(target: String) = Ticket(dataVersion.get(), target, packageVersions[target] ?: 0)
    fun current(ticket: Ticket): Boolean = !destroyed && ticket.data == dataVersion.get() &&
        ticket.packageVersion == (packageVersions[ticket.target] ?: 0)
    fun destroy() {
        destroyed = true
        newData()
    }
}
