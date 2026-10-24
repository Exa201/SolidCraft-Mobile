package org.solidcraft.mobile

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest

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

        val targetMarker = targetMarker(backup)
        if (backup.exists()) {
            when (destinationMatchesMarker(targetMarker, document)) {
                true -> {
                    // A previous provider write was verified before its local backup cleanup failed.
                    // Never roll that successful save back over the document on a later attempt.
                    if (!deleteIfPresent(backup) || !deleteIfPresent(targetMarker)) {
                        return@synchronized "The previous save is complete, but its local recovery files could not be cleaned up"
                    }
                }
                false -> {
                    // Missing legacy marker means the write was interrupted before completion.
                    if (!restore(backup, document)) {
                        return@synchronized "Could not restore the previous document. Its recovery copy remains at ${backup.absolutePath}; the destination may be incomplete"
                    }
                    if (!deleteIfPresent(backup) || !deleteIfPresent(targetMarker)) {
                        return@synchronized "The previous document was restored, but its local recovery files could not be cleaned up"
                    }
                }
                null -> return@synchronized "Could not check the previous save; the destination was not changed and its recovery copy remains"
            }
        } else if (!deleteIfPresent(targetMarker)) {
            return@synchronized "Could not clean up an obsolete save marker; the destination was not changed"
        }

        val targetHash = try {
            sha256(staged.inputStream())
        } catch (error: Exception) {
            return@synchronized "Could not prepare the save verification marker; the document was not changed. ${error.message ?: "Read failed"}"
        }

        try {
            check(backup.parentFile?.isDirectory == true || backup.parentFile?.mkdirs() == true) {
                "Could not create a local safety backup folder"
            }
            val original = requireNotNull(document.openRead()) {
                "The destination cannot be read to create a safety backup; the document was not changed"
            }
            val temporary = File.createTempFile(".solidcraft-save-", ".backup", backup.parentFile)
            try {
                original.use { input ->
                    FileOutputStream(temporary).use { output ->
                        input.copyTo(output, BUFFER_SIZE)
                        output.fd.sync()
                    }
                }
                check(temporary.renameTo(backup)) { "Could not finalize the local safety backup" }
            } finally {
                temporary.delete()
            }
            writeTargetMarker(targetMarker, targetHash)
        } catch (error: Exception) {
            deleteIfPresent(backup)
            deleteIfPresent(targetMarker)
            return@synchronized "Could not create a safety backup; the document was not changed. ${error.message ?: "Read failed"}"
        }

        try {
            // Opening the provider stream can truncate the file, so the backup and marker must
            // already be durable before this call.
            val output = requireNotNull(document.openWrite()) { "The document provider did not open the destination for writing" }
            staged.inputStream().use { input -> output.use { input.copyTo(it, BUFFER_SIZE) } }
            val written = requireNotNull(document.openRead()) { "The saved document could not be verified" }
            val matches = written.use { input -> staged.inputStream().use { expected -> sameBytes(input, expected) } }
            check(matches) { "The provider saved different content than SolidCraft wrote" }
            if (!deleteIfPresent(backup) || !deleteIfPresent(targetMarker)) {
                "The document was saved, but its local recovery files could not be cleaned up; the next save will verify them"
            } else {
                ""
            }
        } catch (error: Exception) {
            val restored = restore(backup, document)
            if (restored) {
                if (!deleteIfPresent(backup) || !deleteIfPresent(targetMarker)) {
                    "Save failed; the previous document was restored, but its local recovery files could not be cleaned up. ${error.message ?: "Provider write failed"}"
                } else {
                    "Save failed; the previous document was restored. ${error.message ?: "Provider write failed"}"
                }
            } else {
                "Save failed and automatic restore could not be verified. A recovery copy remains at ${backup.absolutePath}. ${error.message ?: "Provider write failed"}"
            }
        }
    }

    /** Null means the provider could not be read, so recovery must not overwrite it blindly. */
    private fun destinationMatchesMarker(marker: File, document: DocumentStreams): Boolean? {
        return try {
            val expected = marker.takeIf(File::isFile)?.readText(Charsets.US_ASCII)?.trim()
                ?.takeIf { it.matches(Regex("[0-9a-f]{64}")) }
            val source = document.openRead() ?: return null
            val actual = source.use { sha256(it) }
            expected != null && actual == expected
        } catch (_: Exception) {
            null
        }
    }

    private fun writeTargetMarker(marker: File, hash: String) {
        val parent = requireNotNull(marker.parentFile)
        val temporary = File.createTempFile(".solidcraft-save-target-", ".tmp", parent)
        try {
            FileOutputStream(temporary).use { output ->
                output.write(hash.toByteArray(Charsets.US_ASCII))
                output.fd.sync()
            }
            Files.move(temporary.toPath(), marker.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }

    private fun targetMarker(backup: File) = File(requireNotNull(backup.parentFile), "${backup.name}.target-sha256")

    private fun deleteIfPresent(file: File): Boolean = !file.exists() || file.delete()

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

    private fun sha256(source: InputStream): String = source.use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
