package org.solidcraft.mobile

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files

class DocumentStagingTest {
    @Test
    fun completeReadAtomicallyReplacesAnExistingWorkingCopy() = withWorkspace { directory ->
        val target = File(directory, "part.solidcraft").apply { writeText("old complete model") }

        DocumentStaging.replaceFrom(ByteArrayInputStream("new complete model".toByteArray()), target)

        assertEquals("new complete model", target.readText())
        assertEquals(listOf("part.solidcraft"), directory.list()?.toList())
    }

    @Test
    fun failedReadKeepsThePreviousWorkingCopyAndRemovesTemporaryFile() = withWorkspace { directory ->
        val target = File(directory, "part.solidcraft").apply { writeText("old complete model") }
        val brokenInput = object : InputStream() {
            private var sentPrefix = false

            override fun read(): Int {
                if (!sentPrefix) {
                    sentPrefix = true
                    return 'x'.code
                }
                throw IOException("simulated provider read failure")
            }
        }

        try {
            DocumentStaging.replaceFrom(brokenInput, target)
            throw AssertionError("The provider read error should be surfaced")
        } catch (error: IOException) {
            assertEquals("simulated provider read failure", error.message)
        }

        assertArrayEquals("old complete model".toByteArray(), target.readBytes())
        assertEquals(listOf("part.solidcraft"), directory.list()?.toList())
    }

    @Test
    fun failedReadDoesNotCreateAPartialNewWorkingCopy() = withWorkspace { directory ->
        val target = File(directory, "new.solidcraft")
        val brokenInput = object : InputStream() {
            override fun read(): Int = throw IOException("provider disconnected")
        }

        try {
            DocumentStaging.replaceFrom(brokenInput, target)
            throw AssertionError("The provider read error should be surfaced")
        } catch (_: IOException) {
            // The caller reports the provider error; the staged path remains absent.
        }

        assertEquals(false, target.exists())
        assertEquals(emptyList<String>(), directory.list()?.toList())
    }

    private fun withWorkspace(test: (File) -> Unit) {
        val directory = Files.createTempDirectory("solidcraft-open-test-").toFile()
        try {
            test(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
