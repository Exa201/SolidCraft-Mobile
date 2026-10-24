package org.solidcraft.mobile

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files

class DocumentWriteBackTest {
    @Test
    fun successfulWriteIsVerifiedAndRemovesBackup() = withFiles { staged, backup ->
        val provider = MemoryDocument("old document".toByteArray())

        assertTrue(DocumentWriteBack.persist(staged, backup, provider).isEmpty())

        assertArrayEquals(staged.readBytes(), provider.bytes)
        assertFalse(backup.exists())
    }

    @Test
    fun failedPartialWriteRestoresOriginalDocument() = withFiles { staged, backup ->
        val provider = MemoryDocument("important original data".toByteArray(), failWriteNumbers = setOf(1), failFirstWriteAfterBytes = 5)

        val result = DocumentWriteBack.persist(staged, backup, provider)

        assertTrue(result.contains("previous document was restored"))
        assertArrayEquals("important original data".toByteArray(), provider.bytes)
        assertFalse(backup.exists())
    }

    @Test
    fun retainedBackupRecoversBeforeTheNextSaveAttempt() = withFiles { staged, backup ->
        val provider = MemoryDocument("important original data".toByteArray(), failWriteNumbers = setOf(1, 2), failFirstWriteAfterBytes = 5)

        val firstResult = DocumentWriteBack.persist(staged, backup, provider)

        assertTrue(firstResult.contains("recovery copy remains"))
        assertTrue(backup.isFile)
        assertTrue(DocumentWriteBack.persist(staged, backup, provider).isEmpty())
        assertArrayEquals(staged.readBytes(), provider.bytes)
        assertFalse(backup.exists())
    }

    @Test
    fun unreadableDestinationIsNotChanged() = withFiles { staged, backup ->
        val provider = MemoryDocument("original".toByteArray(), readable = false)

        val result = DocumentWriteBack.persist(staged, backup, provider)

        assertTrue(result.contains("document was not changed"))
        assertArrayEquals("original".toByteArray(), provider.bytes)
        assertTrue(provider.writeCount == 0)
    }

    @Test
    fun providerContentMismatchRollsBackToOriginal() = withFiles { staged, backup ->
        val provider = MemoryDocument("original".toByteArray(), corruptFirstWrite = true)

        val result = DocumentWriteBack.persist(staged, backup, provider)

        assertTrue(result.contains("previous document was restored"))
        assertArrayEquals("original".toByteArray(), provider.bytes)
    }

    private fun withFiles(test: (File, File) -> Unit) {
        val directory = Files.createTempDirectory("solidcraft-save-test-").toFile()
        try {
            val staged = File(directory, "design.solidcraft").apply { writeBytes("new complete document".toByteArray()) }
            test(staged, File(directory, ".design.solidcraft.save-backup"))
        } finally {
            directory.deleteRecursively()
        }
    }

    private class MemoryDocument(
        initial: ByteArray,
        private val readable: Boolean = true,
        private val failWriteNumbers: Set<Int> = emptySet(),
        private val failFirstWriteAfterBytes: Int? = null,
        private val corruptFirstWrite: Boolean = false,
    ) : DocumentStreams {
        var bytes = initial
            private set
        var writeCount = 0
            private set

        override fun openRead(): InputStream? = if (readable) ByteArrayInputStream(bytes) else null

        override fun openWrite(): OutputStream {
            val writeNumber = ++writeCount
            bytes = byteArrayOf()
            return object : ByteArrayOutputStream() {
                override fun write(value: Int) {
                    if (writeNumber in failWriteNumbers && failFirstWriteAfterBytes != null && count >= failFirstWriteAfterBytes) {
                        bytes = toByteArray()
                        throw java.io.IOException("simulated provider failure")
                    }
                    super.write(value)
                    bytes = toByteArray()
                }

                override fun write(buffer: ByteArray, offset: Int, length: Int) {
                    for (index in offset until offset + length) write(buffer[index].toInt())
                }

                override fun close() {
                    bytes = toByteArray()
                    if (writeNumber == 1 && corruptFirstWrite) bytes = "provider corruption".toByteArray()
                }
            }
        }
    }
}
