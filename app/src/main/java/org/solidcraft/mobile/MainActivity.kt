package org.solidcraft.mobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.google.androidgamesdk.GameActivity
import java.io.File
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.util.concurrent.ConcurrentLinkedQueue
import org.json.JSONObject

/** Android lifecycle, text-input host, and Storage Access Framework bridge. */
class MainActivity : GameActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        processContext = applicationContext
        super.onCreate(savedInstanceState)
        hideSystemBars()
        activeActivity = WeakReference(this)
        handleIncomingDocument(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingDocument(intent)
    }

    internal fun handleIncomingDocument(intent: Intent?) {
        val incoming = intent ?: return
        val uri = IncomingCadDocument.uri(incoming) ?: return
        val id = nextExternalRequestId.incrementAndGet()
        if (!documentResults.register(id)) return
        val name = DocumentStaging.safeDocumentName(null, uri.lastPathSegment.orEmpty())
        val extension = IncomingCadDocument.extension(name)
        val request = DocumentRequest(id, false, name, arrayOf(extension), fromExternalIntent = true)
        Thread({ finishDocumentSelection(request, uri, incoming.flags) }, "solidcraft-shared-document").start()
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
        val request = activeRequest.takeForResult(requestCode) ?: return

        if (!documentResults.isPending(request.id)) return

        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            completeDocument(request, "")
            return
        }

        val uri = data.data!!
        if (request.kind == DocumentPickerKind.GLTF_RESOURCE_FOLDER) {
            Thread({ finishResourceFolderSelection(request, uri, data.flags) }, "solidcraft-gltf-resources").start()
        } else {
            Thread({ finishDocumentSelection(request, uri, data.flags) }, "solidcraft-document-picker").start()
        }
    }

    private fun finishDocumentSelection(request: DocumentRequest, uri: Uri, grantedFlags: Int) {
        try {
            if (!documentResults.isPending(request.id)) return
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
            val safeName = DocumentStaging.safeDocumentName(queryDisplayName(uri), request.name)
            val uriKey = MessageDigest.getInstance("SHA-256")
                .digest(uri.toString().toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
            val uriDirectory = File(directory, uriKey)
            check(uriDirectory.isDirectory || uriDirectory.mkdirs()) { "Could not create the document workspace" }
            val staged = DocumentStaging.saveTarget(uriDirectory, safeName)

            if (request.save) {
                // Keep a previous working copy: file.save_as writes atomically and can fail before
                // replacing it, while the provider destination is untouched until write-back.
                persistDocumentMapping(staged.absolutePath, uri, allowed)
            } else {
                val input = requireNotNull(contentResolver.openInputStream(uri)) { "The selected document could not be opened" }
                DocumentStaging.replaceFrom(input, staged)
                persistDocumentMapping(staged.absolutePath, uri, allowed)
            }
            val resources = GltfExternalResources.bufferPaths(staged)
            if (resources.isNotEmpty()) {
                requestResourceFolder(request, staged, resources)
                return
            }
            completeDocument(request, staged.absolutePath)
        } catch (error: Exception) {
            if (completeDocument(request, "")) {
                runOnUiThread { Toast.makeText(this, "SolidCraft: ${error.message ?: "Document access failed"}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun requestResourceFolder(request: DocumentRequest, staged: File, paths: List<String>) {
        runOnUiThread {
            if (!documentResults.isPending(request.id)) return@runOnUiThread
            val pending = request.copy(
                stagedPath = staged.absolutePath,
                resourcePaths = paths,
                kind = DocumentPickerKind.GLTF_RESOURCE_FOLDER,
            )
            if (!activeRequest.begin(pending)) {
                completeDocument(pending, "")
                return@runOnUiThread
            }
            Toast.makeText(
                this,
                "Select the folder containing the glTF buffer files. Android may require a project subfolder instead of the Downloads root.",
                Toast.LENGTH_LONG,
            ).show()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            try {
                startActivityForResult(intent, pending.kind.requestCode)
            } catch (error: Exception) {
                activeRequest.clearIf { it.id == request.id }
                completeDocument(request, "")
                Toast.makeText(this, "SolidCraft: ${error.message ?: "No folder picker is available"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun finishResourceFolderSelection(request: DocumentRequest, treeUri: Uri, grantedFlags: Int) {
        try {
            if (!documentResults.isPending(request.id)) return
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
            completeDocument(request, stagedPath)
        } catch (error: Exception) {
            if (completeDocument(request, "")) {
                runOnUiThread { Toast.makeText(this, "SolidCraft: ${error.message ?: "Could not copy the glTF buffers"}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun completeDocument(request: DocumentRequest, result: String): Boolean {
        val completed = documentResults.complete(request.id, result)
        if (completed && request.fromExternalIntent) {
            documentResults.take(request.id)
            if (result.isNotBlank()) pendingExternalDocuments.add(result)
        }
        return completed
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
            }
        } catch (_: Exception) {
            // Display-name metadata is optional. A provider that cannot answer queries may still
            // grant stream access, so keep opening the document using the picker name as fallback.
            null
        }
    }

    private fun documentPreferences() = getSharedPreferences("solidcraft_documents", MODE_PRIVATE)

    /** Persist the provider mapping before returning a working path to the native CAD engine. */
    private fun persistDocumentMapping(path: String, uri: Uri, grantedFlags: Int) {
        DocumentMapping.persist(path, uri.toString(), grantedFlags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0) { key, value ->
            documentPreferences().edit().putString(key, value).commit()
        }
    }

    companion object {
        private const val MAX_GLTF_BUFFER_BYTES = 1L shl 30
        private const val SHARE_CACHE_RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1000
        private val documentResults = DocumentRequestResults()
        private val activeRequest = DocumentPickerRequestSlot<DocumentRequest> { it.kind }
        private val nextExternalRequestId = java.util.concurrent.atomic.AtomicLong(Long.MIN_VALUE)
        private val pendingExternalDocuments = ConcurrentLinkedQueue<String>()
        /** Provider write-back runs on native save workers that may outlive the current Activity. */
        @Volatile private var processContext: Context? = null
        @Volatile private var activeActivity: WeakReference<MainActivity>? = null

        private data class DocumentRequest(
            val id: Long,
            val save: Boolean,
            val name: String,
            val extensions: Array<String>,
            val kind: DocumentPickerKind = DocumentPickerKind.CAD_DOCUMENT,
            val stagedPath: String? = null,
            val resourcePaths: List<String> = emptyList(),
            val fromExternalIntent: Boolean = false,
        )

        /** The Rust render loop drains completed documents shared from Android file providers. */
        @JvmStatic
        fun takePendingExternalDocument(): String? = pendingExternalDocuments.poll()

        /** Called from the Rust render thread. Activity launches are always posted to Android's UI thread. */
        @JvmStatic
        fun requestDocument(id: Long, save: Boolean, name: String, extensions: Array<String>) {
            if (!documentResults.register(id)) return
            val activity = activeActivity?.get()
            if (activity == null) {
                documentResults.complete(id, "")
                return
            }
            val request = DocumentRequest(id, save, name, extensions)
            activity.runOnUiThread {
                // The Rust picker future may have been dropped while this UI-thread task was
                // queued. Do not launch a picker for a request that no longer has a consumer.
                if (!documentResults.isPending(id)) return@runOnUiThread
                if (!activeRequest.begin(request)) {
                    documentResults.complete(id, "")
                    return@runOnUiThread
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
                    activity.startActivityForResult(intent, request.kind.requestCode)
                } catch (error: Exception) {
                    activeRequest.clearIf { it.id == id }
                    documentResults.complete(id, "")
                    Toast.makeText(activity, "SolidCraft: ${error.message ?: "No document picker is available"}", Toast.LENGTH_LONG).show()
                }
            }
        }

        /** Share a staged STL/3MF through Android's chooser, granting the receiving app read access. */
        @JvmStatic
        fun shareMesh(path: String, mimeType: String): Boolean {
            val activity = activeActivity?.get() ?: return false
            val file = File(path)
            val shareDirectory = File(activity.cacheDir, "solidcraft-share")
            if (!file.isFile || !file.canonicalPath.startsWith(shareDirectory.canonicalPath + File.separator)) return false
            val cutoff = System.currentTimeMillis() - SHARE_CACHE_RETENTION_MILLIS
            shareDirectory.listFiles()?.forEach { cached ->
                if (cached != file && cached.isFile && cached.lastModified() < cutoff) cached.delete()
            }
            val uri = try {
                FileProvider.getUriForFile(activity, "${activity.packageName}.files", file)
            } catch (_: Exception) {
                return false
            }
            activity.runOnUiThread {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = android.content.ClipData.newUri(activity.contentResolver, file.name, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(send, "Open exported mesh")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    activity.startActivity(chooser)
                } catch (error: Exception) {
                    Toast.makeText(activity, "SolidCraft: ${error.message ?: "No compatible app is available"}", Toast.LENGTH_LONG).show()
                }
            }
            return true
        }

        /** The private cache folder exposed by the narrowly scoped FileProvider. */
        @JvmStatic
        fun meshShareDirectory(): String? {
            val activity = activeActivity?.get() ?: return null
            val directory = File(activity.cacheDir, "solidcraft-share")
            return if (directory.isDirectory || directory.mkdirs()) directory.absolutePath else null
        }

        /** Null means pending; empty string means the user cancelled or the picker failed. */
        @JvmStatic
        fun takeDocumentResult(id: Long): String? = documentResults.take(id)

        /** Called when the Rust picker future is dropped before Android returns a result. */
        @JvmStatic
        fun cancelDocumentRequest(id: Long) {
            // Keep an active picker request as a tombstone until its result arrives. Reusing the
            // request code before then could misapply the old picker result to a newer request.
            documentResults.cancel(id)
        }

        /** Synchronously checkpoint the small recent-path list before reporting Save as complete. */
        @JvmStatic
        fun persistRecentDocuments(serialized: String): String {
            val context = processContext ?: return "The Android application context is no longer available"
            val persisted = context.getSharedPreferences("solidcraft_documents", Context.MODE_PRIVATE)
                .edit().putString("recent_documents", serialized).commit()
            return if (persisted) {
                ""
            } else {
                android.util.Log.e("SolidCraftRecent", "Could not persist recent CAD paths")
                "Android could not persist recent-file history"
            }
        }

        /** Last durable CAD recents checkpoint, merged with eframe's regular preferences. */
        @JvmStatic
        fun loadRecentDocuments(): String? = processContext
            ?.getSharedPreferences("solidcraft_documents", Context.MODE_PRIVATE)
            ?.getString("recent_documents", null)

        /** Copies a staged CAD save back to its persisted document-provider URI. Empty means success. */
        @JvmStatic
        fun writeBackDocument(path: String): String {
            val context = processContext ?: return "The Android application context is no longer available"
            val encoded = context.getSharedPreferences("solidcraft_documents", Context.MODE_PRIVATE)
                .getString(path, null) ?: return ""
            return try {
                val mapping = JSONObject(encoded)
                if (!mapping.optBoolean("writable", false)) return "This source document is read-only. Use Save As to choose a writable location."
                val staged = File(path)
                val uri = Uri.parse(mapping.getString("uri"))
                val backup = File(staged.parentFile, ".${staged.name}.save-backup")
                DocumentWriteBack.persist(staged, backup, object : DocumentStreams {
                    override fun openRead() = context.contentResolver.openInputStream(uri)
                    override fun openWrite() = context.contentResolver.openOutputStream(uri, "wt")
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
