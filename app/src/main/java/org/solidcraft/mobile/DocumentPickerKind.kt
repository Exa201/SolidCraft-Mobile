package org.solidcraft.mobile

/** The system picker that must produce the next result for an active document request. */
internal enum class DocumentPickerKind(val requestCode: Int) {
    CAD_DOCUMENT(7321),
    GLTF_RESOURCE_FOLDER(7322);

    fun acceptsResult(code: Int): Boolean = requestCode == code
}

/** Holds one Android picker across asynchronous Rust requests and ignores late, mismatched results. */
internal class DocumentPickerRequestSlot<T>(private val kindOf: (T) -> DocumentPickerKind) {
    private val lock = Any()
    private var active: T? = null

    fun begin(request: T): Boolean = synchronized(lock) {
        if (active != null) return@synchronized false
        active = request
        true
    }

    /** Returns and clears the request only when the arriving result belongs to its picker stage. */
    fun takeForResult(requestCode: Int): T? = synchronized(lock) {
        val current = active ?: return@synchronized null
        if (!kindOf(current).acceptsResult(requestCode)) return@synchronized null
        active = null
        current
    }

    fun clearIf(matches: (T) -> Boolean): Boolean = synchronized(lock) {
        val current = active ?: return@synchronized false
        if (!matches(current)) return@synchronized false
        active = null
        true
    }
}
