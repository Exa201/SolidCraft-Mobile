package org.solidcraft.mobile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingCadDocumentTest {
    @Test
    fun acceptsEngineDocumentFormatsFromAndroidContentProviders() {
        val formats = listOf("part.solidcraft", "assembly.step", "part.FCStd", "mesh.3mf", "scene.glb", "drawing.dxf", "sketch.svg")
        formats.forEach { name -> assertTrue("$name should be openable", IncomingCadDocument.supports(name)) }
    }

    @Test
    fun acceptsSupportedCadMimeTypesWhenProvidersUseOpaqueUris() {
        assertTrue(IncomingCadDocument.supports("android.intent.action.VIEW", "content", "opaque-id", "model/step"))
        assertTrue(IncomingCadDocument.supports("android.intent.action.SEND", "content", "opaque-id", "application/x-solidcraft"))
    }

    @Test
    fun rejectsUnrelatedActionsSchemesAndUnsupportedDocuments() {
        assertFalse(IncomingCadDocument.supports("android.intent.action.MAIN", "content", "part.step", "model/step"))
        assertFalse(IncomingCadDocument.supports("android.intent.action.VIEW", "https", "part.step", "model/step"))
        assertFalse(IncomingCadDocument.supports("android.intent.action.VIEW", "content", "photo.jpg", "image/jpeg"))
        assertFalse(IncomingCadDocument.supports("android.intent.action.SEND", "content", "note.txt", "text/plain"))
    }
}
