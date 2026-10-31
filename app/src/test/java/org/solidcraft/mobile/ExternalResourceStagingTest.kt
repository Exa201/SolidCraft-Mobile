package org.solidcraft.mobile

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files

class ExternalResourceStagingTest {
    @Test
    fun copiesOnlyRequestedBuffersFromNestedProviderFolders() = withWorkspace { workspace ->
        val tree = folder(
            "root",
            folder(
                "buffers",
                file("mesh.bin", byteArrayOf(1, 2, 3)),
                file("unused.bin", byteArrayOf(9)),
            ),
            folder("unrelated", file("other.bin", byteArrayOf(8))),
        )
        val target = File(workspace, "design-work")

        ExternalResourceStaging.copyRequired(tree, target, listOf("buffers/mesh.bin"), maxBytes = 8)

        assertArrayEquals(byteArrayOf(1, 2, 3), File(target, "buffers/mesh.bin").readBytes())
        assertFalse(File(target, "buffers/unused.bin").exists())
        assertFalse(File(target, "unrelated/other.bin").exists())
    }

    @Test
    fun missingRequiredBufferFailsWithoutCreatingItsTarget() = withWorkspace { workspace ->
        val target = File(workspace, "design-work")

        val error = try {
            ExternalResourceStaging.copyRequired(folder("root", file("present.bin", byteArrayOf(1))), target, listOf("buffers/missing.bin"), 8)
            throw AssertionError("A missing glTF buffer must fail the import")
        } catch (expected: IllegalStateException) {
            expected
        }

        assertTrue(error.message!!.contains("buffers/missing.bin"))
        assertFalse(File(target, "buffers/missing.bin").exists())
    }

    @Test
    fun totalSizeLimitIsSharedAcrossAllRequiredBuffers() = withWorkspace { workspace ->
        val tree = folder("root", file("first.bin", byteArrayOf(1, 2, 3)), file("second.bin", byteArrayOf(4, 5, 6)))
        val target = File(workspace, "design-work")

        val error = try {
            ExternalResourceStaging.copyRequired(tree, target, listOf("first.bin", "second.bin"), maxBytes = 5)
            throw AssertionError("Combined external buffers must honor the size limit")
        } catch (expected: IllegalStateException) {
            expected
        }

        assertEquals("The document exceeds the Android import size limit", error.message)
        assertArrayEquals(byteArrayOf(1, 2, 3), File(target, "first.bin").readBytes())
        assertFalse(File(target, "second.bin").exists())
    }

    @Test
    fun invalidProviderEntryNamesCannotEscapeTheWorkingFolder() = withWorkspace { workspace ->
        val target = File(workspace, "design-work")
        val tree = folder("root", file("../outside.bin", byteArrayOf(1)))

        val error = try {
            ExternalResourceStaging.copyRequired(tree, target, listOf("outside.bin"), maxBytes = 8)
            throw AssertionError("A provider entry with path separators must not satisfy a required resource")
        } catch (expected: IllegalStateException) {
            expected
        }

        assertTrue(error.message!!.contains("outside.bin"))
        assertFalse(File(workspace, "outside.bin").exists())
    }

    private fun file(name: String, bytes: ByteArray) = ExternalResourceEntry(
        name = name,
        isDirectory = false,
        isFile = true,
        open = { ByteArrayInputStream(bytes) },
    )

    private fun folder(name: String, vararg children: ExternalResourceEntry) = ExternalResourceEntry(
        name = name,
        isDirectory = true,
        isFile = false,
        children = { children.toList() },
    )

    private fun withWorkspace(test: (File) -> Unit) {
        val directory = Files.createTempDirectory("solidcraft-gltf-resources-").toFile()
        try {
            test(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
