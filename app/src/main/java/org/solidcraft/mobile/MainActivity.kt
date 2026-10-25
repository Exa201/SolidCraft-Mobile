package org.solidcraft.mobile

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.documentfile.provider.DocumentFile
import com.google.androidgamesdk.GameActivity
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.lang.ref.WeakReference
import org.json.JSONObject

/** Android lifecycle, text-input host, and Storage Access Framework bridge. */
class MainActivity : GameActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        activeActivity = WeakReference(this)
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onDestroy() {
        if (activeActivity?.get() === this) activeActivity = null
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != DOCUMENT_REQUEST_CODE && requestCode != RESOURCE_FOLDER_REQUEST_CODE) return
        val request = synchronized(requestLock) {
            val current = activeRequest
            activeRequest = null
            current
        } ?: return

        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            results[request.id] = ""
            return
        }

        val uri = data.data!!
        if (requestCode == RESOURCE_FOLDER_REQUEST_CODE) {
            Thread({ finishResourceFolderSelection(request, uri, data.flags) }, "solidcraft-gltf-resources").start()
        } else {
            Thread({ finishDocumentSelection(request, uri, data.flags) }, "solidcraft-document-picker").start()
        }
    }

    private fun finishDocumentSelection(request: DocumentRequest, uri: Uri, grantedFlags: Int) {
        try {
            val allowed = grantedFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (allowed != 0) {
                try {
                    contentResolver.takePersistableUriPermission(uri, allowed)
                } catch (_: SecurityException) {
                    // Some document providers offer only a temporary grant. The staged file still
                    // remains usable for this session; the mapping records its write capability.
                }
            }

            // Recent CAD entries store filesystem paths in the shared engine settings. Keep the
            // staged working copy in durable app-private storage so Android cache eviction cannot
            // turn a recent document into a missing file.
            val directory = File(filesDir, "solidcraft-documents")
            check(directory.isDirectory || directory.mkdirs()) { "Could not create the CAD document workspace" }
            val displayName = if (request.save) request.name else queryDisplayName(uri) ?: request.name
            val safeName = displayName.substringAfterLast('/').substringAfterLast('\\')
                .filter { it.isLetterOrDigit() || it in "._- ()" }
                .take(160).ifBlank { "document" }
            val uriKey = MessageDigest.getInstance("SHA-256")
                .digest(uri.toString().toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
            val uriDirectory = File(directory, uriKey)
            check(uriDirectory.isDirectory || uriDirectory.mkdirs()) { "Could not create the document workspace" }
            val staged = File(uriDirectory, safeName)

            if (request.save) {
                staged.delete()
                val mapping = JSONObject().put("uri", uri.toString())
                    .put("writable", allowed and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
                documentPreferences().edit().putString(staged.absolutePath, mapping.toString()).apply()
            } else {
                val input = requireNotNull(contentResolver.openInputStream(uri)) { "The selected document could not be opened" }
                DocumentStaging.replaceFrom(input, staged)
                val mapping = JSONObject().put("uri", uri.toString())
                    .put("writable", allowed and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
                documentPreferences().edit().putString(staged.absolutePath, mapping.toString()).apply()
            }
            val resources = GltfExternalResources.bufferPaths(staged)
            if (resources.isNotEmpty()) {
                requestResourceFolder(request, staged, resources)
                return
            }
            results[request.id] = staged.absolutePath
        } catch (error: Exception) {
            runOnUiThread { Toast.makeText(this, "SolidCraft: ${error.message ?: "Document access failed"}", Toast.LENGTH_LONG).show() }
            results[request.id] = ""
        }
    }

    private fun requestResourceFolder(request: DocumentRequest, staged: File, paths: List<String>) {
        runOnUiThread {
            val pending = request.copy(stagedPath = staged.absolutePath, resourcePaths = paths)
            synchronized(requestLock) {
                if (activeRequest != null) {
                    results[request.id] = ""
                    return@runOnUiThread
                }
                activeRequest = pending
            }
            Toast.makeText(
                this,
                "Select the folder containing the glTF buffer files. Android may require a project subfolder instead of the Downloads root.",
                Toast.LENGTH_LONG,
            ).show()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            try {
                startActivityForResult(intent, RESOURCE_FOLDER_REQUEST_CODE)
            } catch (error: Exception) {
                synchronized(requestLock) { activeRequest = null }
                results[request.id] = ""
                Toast.makeText(this, "SolidCraft: ${error.message ?: "No folder picker is available"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun finishResourceFolderSelection(request: DocumentRequest, treeUri: Uri, grantedFlags: Int) {
        try {
            val readable = grantedFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (readable != 0) {
                try {
                    contentResolver.takePersistableUriPermission(treeUri, readable)
                } catch (_: SecurityException) {
                    // The folder is used only while its returned grant is active; all required
                    // buffers are copied into the app-private working folder below.
                }
            }
            val stagedPath = requireNotNull(request.stagedPath) { "The staged glTF file is missing" }
            val targetRoot = requireNotNull(File(stagedPath).parentFile) { "The glTF working folder is missing" }
            val tree = requireNotNull(DocumentFile.fromTreeUri(this, treeUri)) { "The selected folder is unavailable" }
            fun entry(file: DocumentFile): ExternalResourceEntry = ExternalResourceEntry(
                name = file.name,
                isDirectory = file.isDirectory,
                isFile = file.isFile,
                children = { file.listFiles().map(::entry) },
                open = { contentResolver.openInputStream(file.uri) },
            )
            ExternalResourceStaging.copyRequired(entry(tree), targetRoot, request.resourcePaths, MAX_GLTF_BUFFER_BYTES)
            results[request.id] = stagedPath
        } catch (error: Exception) {
            runOnUiThread { Toast.makeText(this, "SolidCraft: ${error.message ?: "Could not copy the glTF buffers"}", Toast.LENGTH_LONG).show() }
            results[request.id] = ""
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor?.moveToFirst() == true) cursor.getString(0) else null
        } finally {
            cursor?.close()
        }
    }

    private fun documentPreferences() = getSharedPreferences("solidcraft_documents", MODE_PRIVATE)

    companion object {
        private const val DOCUMENT_REQUEST_CODE = 7321
        private const val RESOURCE_FOLDER_REQUEST_CODE = 7322
        private const val MAX_GLTF_BUFFER_BYTES = 1L shl 30
        private val results = ConcurrentHashMap<Long, String>()
        private val requested = ConcurrentHashMap.newKeySet<Long>()
        private val requestLock = Any()
        private var activeRequest: DocumentRequest? = null
        @Volatile private var activeActivity: WeakReference<MainActivity>? = null

        private data class DocumentRequest(
            val id: Long,
            val save: Boolean,
            val name: String,
            val extensions: Array<String>,
            val stagedPath: String? = null,
            val resourcePaths: List<String> = emptyList(),
        )

        /** Called from the Rust render thread. Activity launches are always posted to Android's UI thread. */
        @JvmStatic
        fun requestDocument(id: Long, save: Boolean, name: String, extensions: Array<String>) {
            if (!requested.add(id)) return
            val activity = activeActivity?.get()
            if (activity == null) {
                requested.remove(id)
                results[id] = ""
                return
            }
            val request = DocumentRequest(id, save, name, extensions)
            activity.runOnUiThread {
                synchronized(requestLock) {
                    // The Rust picker future may have been dropped while this UI-thread task was
                    // queued. Do not launch a picker for a request that no longer has a consumer.
                    if (id !in requested) return@runOnUiThread
                    if (activeRequest != null) {
                        requested.remove(id)
                        results[id] = ""
                        return@runOnUiThread
                    }
                    activeRequest = request
                }

                val intent = if (save) Intent(Intent.ACTION_CREATE_DOCUMENT) else Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                if (save) {
                    intent.type = mimeFor(name)
                    intent.putExtra(Intent.EXTRA_TITLE, name)
                } else {
                    intent.type = "*/*"
                    // Only add a provider filter when every supported file type has a MIME
                    // mapping. A partial filter silently hides custom CAD formats such as
                    // .solidcraft, .fcstd and .3dm in otherwise valid document providers.
                    CadDocumentTypes.openMimeTypes(extensions)?.let { intent.putExtra(Intent.EXTRA_MIME_TYPES, it) }
                }
                try {
                    activity.startActivityForResult(intent, DOCUMENT_REQUEST_CODE)
                } catch (error: Exception) {
                    synchronized(requestLock) { activeRequest = null }
                    requested.remove(id)
                    results[id] = ""
                    Toast.makeText(activity, "SolidCraft: ${error.message ?: "No document picker is available"}", Toast.LENGTH_LONG).show()
                }
            }
        }

        /** Null means pending; empty string means the user cancelled or the picker failed. */
        @JvmStatic
        fun takeDocumentResult(id: Long): String? {
            val value = results.remove(id) ?: return null
            requested.remove(id)
            return value
        }

        /** Called when the Rust picker future is dropped before Android returns a result. */
        @JvmStatic
        fun cancelDocumentRequest(id: Long) {
            requested.remove(id)
            results.remove(id)
            synchronized(requestLock) {
                if (activeRequest?.id == id) activeRequest = null
            }
        }

        /** Copies a staged CAD save back to its persisted document-provider URI. Empty means success. */
        @JvmStatic
        fun writeBackDocument(path: String): String {
            val activity = activeActivity?.get() ?: return "The Android activity is no longer available"
            val encoded = activity.documentPreferences().getString(path, null) ?: return ""
            return try {
                val mapping = JSONObject(encoded)
                if (!mapping.optBoolean("writable", false)) return "This source document is read-only. Use Save As to choose a writable location."
                val staged = File(path)
                val uri = Uri.parse(mapping.getString("uri"))
                val backup = File(staged.parentFile, ".${staged.name}.save-backup")
                DocumentWriteBack.persist(staged, backup, object : DocumentStreams {
                    override fun openRead() = activity.contentResolver.openInputStream(uri)
                    override fun openWrite() = activity.contentResolver.openOutputStream(uri, "wt")
                })
            } catch (error: Exception) {
                error.message ?: "Could not write the CAD file to the selected document provider"
            }
        }

        private fun mimeFor(name: String): String = mimeForExtension(name.substringAfterLast('.', "")) ?: "application/octet-stream"

        private fun mimeForExtension(raw: String): String? = CadDocumentTypes.mimeForExtension(raw)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(raw.lowercase().removePrefix("."))
    }
}

/** MIME types for CAD formats; unregistered/private extensions intentionally remain unfiltered. */
internal object CadDocumentTypes {
    /** Returns null when any accepted extension is not mapped, so SAF can show custom CAD files. */
    fun openMimeTypes(extensions: Array<String>): Array<String>? {
        if (extensions.isEmpty()) return null
        val mapped = extensions.map(::mimeForExtension)
        if (mapped.any { it == null }) return null
        return mapped.filterNotNull().distinct().toTypedArray()
    }

    fun mimeForExtension(raw: String): String? = when (raw.lowercase().removePrefix(".")) {
        // IANA Media Types registry: model/3mf, model/gltf-binary, model/gltf+json,
        // model/iges, model/obj, model/step and model/stl.
        "3mf" -> "model/3mf"
        "glb" -> "model/gltf-binary"
        "gltf" -> "model/gltf+json"
        "igs", "iges" -> "model/iges"
        "obj" -> "model/obj"
        "step", "stp" -> "model/step"
        "stl" -> "model/stl"
        "dxf" -> "image/vnd.dxf"
        "svg" -> "image/svg+xml"
        "solidcraft" -> "application/x-solidcraft"
        "json" -> "application/json"
        else -> null
    }
}
