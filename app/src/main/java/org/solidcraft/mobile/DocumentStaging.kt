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

    fun replaceFrom(input: InputStream, target: File) = input.use { source ->
        val parent = requireNotNull(target.absoluteFile.parentFile) { "The document workspace has no parent directory" }
        check(parent.isDirectory || parent.mkdirs()) { "Could not create the document workspace" }

        val temporary = Files.createTempFile(parent.toPath(), ".solidcraft-open-", ".stage").toFile()
        try {
            FileOutputStream(temporary).use { output ->
                source.copyTo(output, BUFFER_SIZE)
                output.fd.sync()
            }
            Files.move(temporary.toPath(), target.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }
}
