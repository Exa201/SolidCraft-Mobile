package org.solidcraft.mobile

import android.content.Intent
import android.net.Uri

/** Resolves only CAD documents that SolidCraft's engine can open from Android intents. */
internal object IncomingCadDocument {
    private val supportedExtensions = setOf(
        "solidcraft", "step", "stp", "igs", "iges", "fcstd", "3dm", "3mf", "stl", "obj", "glb", "gltf", "dxf", "svg", "json",
    )
    private val supportedMimeTypes = setOf(
        "application/x-solidcraft", "application/step", "application/iges", "application/json",
        "model/step", "model/iges", "model/vnd.3dm", "model/3mf", "model/stl", "model/obj",
        "model/gltf+json", "model/gltf-binary", "image/vnd.dxf", "image/svg+xml",
    )

    fun uri(intent: Intent): Uri? {
        val candidate = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri ?: intent.clipData?.getItemAt(0)?.uri ?: intent.data
            }
            else -> null
        } ?: return null

        return candidate.takeIf { supports(intent.action, it.scheme, it.lastPathSegment, intent.type) }
    }

    fun extension(name: String): String = name.substringAfterLast('.', "").lowercase()

    fun supports(name: String): Boolean = extension(name) in supportedExtensions

    fun supports(action: String?, scheme: String?, path: String?, mimeType: String?): Boolean {
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) return false
        if (scheme != "content" && scheme != "file") return false
        val supportedMime = mimeType?.lowercase() in supportedMimeTypes
        return supports(path.orEmpty()) || supportedMime
    }
}
