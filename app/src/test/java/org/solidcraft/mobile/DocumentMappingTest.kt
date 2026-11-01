package org.solidcraft.mobile

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentMappingTest {
    @Test
    fun persistsTheUriAndWriteCapabilityBeforeReturning() {
        var storedPath: String? = null
        var storedValue: String? = null

        DocumentMapping.persist("/working/part.solidcraft", "content://provider/document/7", true) { path, value ->
            storedPath = path
            storedValue = value
            true
        }

        assertEquals("/working/part.solidcraft", storedPath)
        val mapping = JSONObject(requireNotNull(storedValue))
        assertEquals("content://provider/document/7", mapping.getString("uri"))
        assertTrue(mapping.getBoolean("writable"))
    }

    @Test
    fun failedDurableWritePreventsReturningAUsableMapping() {
        var attempted = false

        val error = assertThrows(IllegalStateException::class.java) {
            DocumentMapping.persist("/working/part.solidcraft", "content://provider/document/7", false) { _, _ ->
                attempted = true
                false
            }
        }

        assertTrue(attempted)
        assertEquals("Could not persist the Android document mapping", error.message)
    }

    @Test
    fun rejectsMissingWorkingPathOrProviderUriBeforeWriting() {
        var wrote = false

        assertThrows(IllegalArgumentException::class.java) {
            DocumentMapping.persist(" ", "content://provider/document/7", false) { _, _ -> wrote = true; true }
        }
        assertThrows(IllegalArgumentException::class.java) {
            DocumentMapping.persist("/working/part.solidcraft", "", false) { _, _ -> wrote = true; true }
        }

        assertFalse(wrote)
    }
}
