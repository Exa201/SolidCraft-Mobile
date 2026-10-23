package org.solidcraft.mobile

import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Extracts only external glTF buffer references that the Rust importer will resolve locally. */
internal object GltfExternalResources {
    private const val MAX_JSON_BYTES = 16 * 1024 * 1024
    private const val GLB_MAGIC = 0x46546c67
    private const val JSON_CHUNK = 0x4e4f534a

    fun bufferPaths(file: File): List<String> {
        val json = when (file.extension.lowercase()) {
            "gltf" -> {
                require(file.length() <= MAX_JSON_BYTES) { "The glTF document is too large" }
                file.readText(Charsets.UTF_8)
            }
            "glb" -> readGlbJson(file) ?: return emptyList()
            else -> return emptyList()
        }
        val buffers = JSONObject(json).optJSONArray("buffers") ?: return emptyList()
        require(buffers.length() <= 64) { "The glTF document references too many buffers" }
        return buildList {
            for (index in 0 until buffers.length()) {
                val uri = buffers.optJSONObject(index)?.optString("uri") ?: continue
                if (uri.startsWith("data:", ignoreCase = true)) continue
                add(safeRelativePath(uri))
            }
        }.distinct()
    }

    /** Matches the engine's relative-resource policy and rejects traversal or URI schemes. */
    fun safeRelativePath(uri: String): String {
        require(uri.isNotBlank()) { "The glTF buffer URI is empty" }
        require(!uri.startsWith('/') && !uri.startsWith('\\')) { "Absolute glTF buffer paths are unsupported" }
        require(!uri.contains(':')) { "URI schemes are unsupported for glTF buffers" }
        // URLDecoder treats '+' as a space; URI path segments do not, so preserve it.
        val decoded = URLDecoder.decode(uri.replace("+", "%2B"), StandardCharsets.UTF_8).replace('\\', '/')
        require(decoded.isNotBlank()) { "The glTF buffer path is empty" }
        val segments = decoded.split('/').filter { it.isNotEmpty() && it != "." }
        require(segments.isNotEmpty() && segments.none { it == ".." }) {
            "The glTF buffer path is not a safe relative path"
        }
        return segments.joinToString("/")
    }

    private fun readGlbJson(file: File): String? {
        RandomAccessFile(file, "r").use { input ->
            if (input.length() < 20) return null
            require(input.readInt().toUInt() == GLB_MAGIC.toUInt()) { "Invalid GLB header" }
            require(input.readInt() == 2) { "Only GLB version 2 is supported" }
            val declaredLength = input.readInt().toLong() and 0xffffffffL
            require(declaredLength <= input.length() && declaredLength >= 20) { "Invalid GLB length" }
            var remaining = declaredLength - 12
            while (remaining >= 8) {
                val chunkLength = input.readInt().toLong() and 0xffffffffL
                val chunkType = input.readInt()
                remaining -= 8
                require(chunkLength <= remaining && chunkLength <= MAX_JSON_BYTES) { "Invalid or oversized GLB chunk" }
                if (chunkType == JSON_CHUNK) {
                    val bytes = ByteArray(chunkLength.toInt())
                    input.readFully(bytes)
                    return bytes.toString(Charsets.UTF_8).trimEnd('\u0000', ' ', '\n', '\r', '\t')
                }
                input.seek(input.filePointer + chunkLength)
                remaining -= chunkLength
            }
        }
        return null
    }
}
