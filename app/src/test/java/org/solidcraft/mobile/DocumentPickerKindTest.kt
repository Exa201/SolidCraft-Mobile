package org.solidcraft.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentPickerKindTest {
    @Test
    fun cadDocumentRequestAcceptsOnlyItsOwnPickerResult() {
        val request = DocumentPickerKind.CAD_DOCUMENT

        assertTrue(request.acceptsResult(request.requestCode))
        assertFalse(request.acceptsResult(DocumentPickerKind.GLTF_RESOURCE_FOLDER.requestCode))
        assertFalse(request.acceptsResult(request.requestCode + 1))
    }

    @Test
    fun gltfFolderRequestAcceptsOnlyItsOwnPickerResult() {
        val request = DocumentPickerKind.GLTF_RESOURCE_FOLDER

        assertTrue(request.acceptsResult(request.requestCode))
        assertFalse(request.acceptsResult(DocumentPickerKind.CAD_DOCUMENT.requestCode))
        assertFalse(request.acceptsResult(request.requestCode + 1))
    }

    @Test
    fun mismatchedLateResultDoesNotConsumeTheActivePickerRequest() {
        data class Request(val id: Long, val kind: DocumentPickerKind)
        val slot = DocumentPickerRequestSlot<Request> { it.kind }
        val cadRequest = Request(1, DocumentPickerKind.CAD_DOCUMENT)
        val folderRequest = Request(2, DocumentPickerKind.GLTF_RESOURCE_FOLDER)

        assertTrue(slot.begin(cadRequest))
        assertNull(slot.takeForResult(DocumentPickerKind.GLTF_RESOURCE_FOLDER.requestCode))
        assertEquals(cadRequest, slot.takeForResult(DocumentPickerKind.CAD_DOCUMENT.requestCode))
        assertTrue(slot.begin(folderRequest))
        assertNull(slot.takeForResult(DocumentPickerKind.CAD_DOCUMENT.requestCode))
        assertEquals(folderRequest, slot.takeForResult(DocumentPickerKind.GLTF_RESOURCE_FOLDER.requestCode))
    }

    @Test
    fun clearingOneRequestDoesNotRemoveAnotherRequest() {
        data class Request(val id: Long, val kind: DocumentPickerKind)
        val slot = DocumentPickerRequestSlot<Request> { it.kind }
        val request = Request(7, DocumentPickerKind.CAD_DOCUMENT)

        assertTrue(slot.begin(request))
        assertFalse(slot.clearIf { it.id == 8L })
        assertEquals(request, slot.takeForResult(request.kind.requestCode))
        assertTrue(slot.begin(request))
        assertTrue(slot.clearIf { it.id == request.id })
        assertNull(slot.takeForResult(request.kind.requestCode))
    }
}
