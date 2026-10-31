package org.solidcraft.mobile

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/** Replaces an app-private opened-document copy only after its complete contents are durable. */
internal object DocumentStaging {
    private const val BUFFER_SIZE = 64 * 1024

    /** Prefer the name reported for the selected URI, but keep picker names safe for local paths. */
    fun safeDocumentName(providerName: String?, fallbackName: String): String {
        fun sanitize(name: String): String = name.substringAfterLast('/').substringAfterLast('\\')
            .filter { it.isLetterOrDigit() || it in "._- ()" }
            .take(160)

        fun usableName(name: String): String = sanitize(name).takeUnless { it == "." || it == ".." }.orEmpty()

        return usableName(providerName.orEmpty()).ifBlank { usableName(fallbackName) }.ifBlank { "document" }
    }

    fun replaceFrom(input: InputStream, target: File, maxBytes: Long? = null) = input.use { source ->
        require(maxBytes == null || maxBytes >= 0) { "The document size limit is invalid" }
        val parent = requireNotNull(target.absoluteFile.parentFile) { "The document workspace has no parent directory" }
        check(parent.isDirectory || parent.mkdirs()) { "Could not create the document workspace" }

        val temporary = Files.createTempFile(parent.toPath(), ".solidcraft-open-", ".stage").toFile()
        try {
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    total += count
                    check(maxBytes == null || total <= maxBytes) { "The document exceeds the Android import size limit" }
                    output.write(buffer, 0, count)
                }
                output.fd.sync()
            }
            Files.move(temporary.toPath(), target.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }
}
