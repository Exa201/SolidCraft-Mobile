package org.solidcraft.mobile

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CadDocumentTypesTest {
    @Test
    fun meshImporterExtensionsHaveRecognizedTypes() {
        assertArrayEquals(
            arrayOf("model/3mf", "model/stl", "model/obj", "model/gltf-binary", "model/gltf+json"),
            CadDocumentTypes.openMimeTypes(arrayOf("3mf", "stl", "obj", "glb", "gltf")),
        )
    }

    @Test
    fun stepAndIgesAliasesShareRegisteredTypes() {
        assertArrayEquals(
            arrayOf("model/step", "model/iges"),
            CadDocumentTypes.openMimeTypes(arrayOf(".step", "stp", "igs", "iges")),
        )
    }

    @Test
    fun customCadFormatsDisablePartialSafFiltering() {
        assertNull(CadDocumentTypes.openMimeTypes(arrayOf("solidcraft", "step", "fcstd", "3dm")))
    }

    @Test
    fun unsupportedAndEmptyFiltersStayUnrestricted() {
        assertNull(CadDocumentTypes.openMimeTypes(emptyArray()))
        assertNull(CadDocumentTypes.openMimeTypes(arrayOf("unknown-cad-format")))
    }

    @Test
    fun saveUsesSolidcraftDocumentType() {
        assertEquals("application/x-solidcraft", CadDocumentTypes.mimeForExtension(".SOLIDCRAFT"))
    }

    @Test
    fun gltfBufferPathsAllowNestedRelativeFiles() {
        assertEquals("buffers/mesh.bin", GltfExternalResources.safeRelativePath("buffers/mesh.bin"))
    }

    @Test
    fun gltfBufferPathsDecodePercentEscapes() {
        assertEquals("buffers/my mesh.bin", GltfExternalResources.safeRelativePath("buffers/my%20mesh.bin"))
    }

    @Test
    fun gltfBufferPathsKeepPlusAndNormalizeCurrentDirectorySegments() {
        assertEquals("buffers/mesh+lod.bin", GltfExternalResources.safeRelativePath("buffers/./mesh+lod.bin"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun gltfBufferPathsRejectTraversal() {
        GltfExternalResources.safeRelativePath("../outside.bin")
    }

    @Test(expected = IllegalArgumentException::class)
    fun gltfBufferPathsRejectEncodedTraversal() {
        GltfExternalResources.safeRelativePath("%2e%2e/outside.bin")
    }

    @Test(expected = IllegalArgumentException::class)
    fun gltfBufferPathsRejectAbsoluteAndSchemeUris() {
        GltfExternalResources.safeRelativePath("file:///outside.bin")
    }
}
