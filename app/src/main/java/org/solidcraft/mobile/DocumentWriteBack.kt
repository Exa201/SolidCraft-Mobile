package org.solidcraft.mobile

import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** Small seam around a document-provider URI so the save transaction can be tested on the JVM. */
internal interface DocumentStreams {
    fun openRead(): InputStream?
    fun openWrite(): OutputStream?
}

/** Protects a provider document from ordinary write failures and verifies the completed save. */
internal object DocumentWriteBack {
    private const val BUFFER_SIZE = 64 * 1024

    fun persist(staged: File, backup: File, document: DocumentStreams): String = synchronized(backup.absolutePath.intern()) {
        if (!staged.isFile) return@synchronized "The staged CAD file was not created"
        if (staged.canonicalFile == backup.canonicalFile) return@synchronized "The save backup path is invalid"

        // A backup left by a process death or failed rollback must be honored before any new write.
        if (backup.exists()) {
            if (!restore(backup, document)) {
                return@synchronized "A previous save has an unverified recovery copy at ${backup.absolutePath}; the destination was not changed"
            }
            backup.delete()
        }

        var backupInProgress: File? = null
        try {
            check(backup.parentFile?.isDirectory == true || backup.parentFile?.mkdirs() == true) {
                "Could not create a local safety backup folder"
            }
            val original = requireNotNull(document.openRead()) {
                "The destination cannot be read to create a safety backup; the document was not changed"
            }
            val temporary = File.createTempFile(".solidcraft-save-", ".backup", backup.parentFile)
            backupInProgress = temporary
            original.use { input -> temporary.outputStream().use { output -> input.copyTo(output, BUFFER_SIZE) } }
            check(temporary.renameTo(backup)) { "Could not finalize the local safety backup" }
            backupInProgress = null
        } catch (error: Exception) {
            backupInProgress?.delete()
            return@synchronized "Could not create a safety backup; the document was not changed. ${error.message ?: "Read failed"}"
        }

        // Providers may truncate as part of opening the output stream, before returning it.
        var writeStarted = true
        try {
            val output = requireNotNull(document.openWrite()) { "The document provider did not open the destination for writing" }
            staged.inputStream().use { input -> output.use { input.copyTo(it, BUFFER_SIZE) } }
            val written = requireNotNull(document.openRead()) { "The saved document could not be verified" }
            val matches = written.use { input -> staged.inputStream().use { expected -> sameBytes(input, expected) } }
            check(matches) { "The provider saved different content than SolidCraft wrote" }
            backup.delete()
            ""
        } catch (error: Exception) {
            if (!writeStarted) {
                backup.delete()
                return@synchronized error.message ?: "Could not write the CAD file"
            }

            val restored = restore(backup, document)
            if (restored) {
                backup.delete()
                "Save failed; the previous document was restored. ${error.message ?: "Provider write failed"}"
            } else {
                "Save failed and automatic restore could not be verified. A recovery copy remains at ${backup.absolutePath}. ${error.message ?: "Provider write failed"}"
            }
        }
    }

    private fun restore(backup: File, document: DocumentStreams): Boolean = try {
        val output = requireNotNull(document.openWrite()) { "The document provider refused the restore stream" }
        backup.inputStream().use { input -> output.use { input.copyTo(it, BUFFER_SIZE) } }
        val check = requireNotNull(document.openRead()) { "The restored document could not be verified" }
        check.use { input -> backup.inputStream().use { expected -> sameBytes(input, expected) } }
    } catch (_: Exception) {
        false
    }

    private fun sameBytes(first: InputStream, second: InputStream): Boolean {
        val firstBuffer = ByteArray(BUFFER_SIZE)
        val secondBuffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val firstCount = first.read(firstBuffer)
            val secondCount = second.read(secondBuffer)
            if (firstCount != secondCount) return false
            if (firstCount < 0) return true
            for (index in 0 until firstCount) if (firstBuffer[index] != secondBuffer[index]) return false
        }
    }
}
