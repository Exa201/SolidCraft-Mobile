package org.solidcraft.mobile

import java.io.File
import java.io.InputStream

/** Minimal document-provider tree entry, kept separate so path selection can be tested on the JVM. */
internal data class ExternalResourceEntry(
    val name: String?,
    val isDirectory: Boolean,
    val isFile: Boolean,
    val children: () -> List<ExternalResourceEntry> = { emptyList() },
    val open: (() -> InputStream?)? = null,
)

/** Copies only the external resources requested by a glTF file into its private working folder. */
internal object ExternalResourceStaging {
    private const val MAX_DEPTH = 32
    private const val MAX_ENTRIES = 10_000

    fun copyRequired(
        tree: ExternalResourceEntry,
        targetRoot: File,
        requiredPaths: List<String>,
        maxBytes: Long,
    ) {
        require(maxBytes >= 0) { "The glTF resource size limit is invalid" }
        val required = requiredPaths.toSet()
        if (required.isEmpty()) return
        check(targetRoot.isDirectory || targetRoot.mkdirs()) { "Could not create the glTF working folder" }
        val canonicalRoot = targetRoot.canonicalFile
        val rootPrefix = canonicalRoot.path + File.separator
        val copied = mutableSetOf<String>()
        var visited = 0
        var copiedBytes = 0L

        fun visit(folder: ExternalResourceEntry, prefix: String, depth: Int) {
            require(depth <= MAX_DEPTH) { "The selected folder is nested too deeply" }
            for (child in folder.children()) {
                visited += 1
                require(visited <= MAX_ENTRIES) { "The selected folder contains too many entries" }
                val name = child.name ?: continue
                if (name == "." || name == ".." || '/' in name || '\\' in name) continue
                val relative = if (prefix.isEmpty()) name else "$prefix/$name"
                if (child.isDirectory) {
                    if (required.any { it.startsWith("$relative/") }) visit(child, relative, depth + 1)
                } else if (child.isFile && relative in required) {
                    val target = File(canonicalRoot, relative).canonicalFile
                    check(target.path.startsWith(rootPrefix)) { "The glTF resource path leaves its working folder" }
                    check(target.parentFile?.isDirectory == true || target.parentFile?.mkdirs() == true) {
                        "Could not create a glTF resource subfolder"
                    }
                    val source = requireNotNull(child.open?.invoke()) { "Could not open glTF buffer $relative" }
                    DocumentStaging.replaceFrom(source, target, maxBytes - copiedBytes)
                    copiedBytes += target.length()
                    copied += relative
                }
            }
        }

        visit(tree, "", 0)
        val missing = required - copied
        check(missing.isEmpty()) { "The selected folder is missing: ${missing.sorted().joinToString()}" }
    }
}
