package org.solidcraft.mobile

/** Thread-safe completion storage for asynchronous Android document-picker requests. */
internal class DocumentRequestResults {
    private val lock = Any()
    private val pending = mutableSetOf<Long>()
    private val completed = mutableMapOf<Long, String>()

    fun register(id: Long): Boolean = synchronized(lock) {
        if (id in pending || id in completed) return@synchronized false
        pending.add(id)
    }

    fun isPending(id: Long): Boolean = synchronized(lock) { id in pending }

    /** Returns false when the request was cancelled before the worker completed. */
    fun complete(id: Long, result: String): Boolean = synchronized(lock) {
        if (!pending.remove(id)) return@synchronized false
        completed[id] = result
        true
    }

    fun take(id: Long): String? = synchronized(lock) {
        completed.remove(id)
    }

    fun cancel(id: Long) = synchronized(lock) {
        pending.remove(id)
        completed.remove(id)
    }
}
