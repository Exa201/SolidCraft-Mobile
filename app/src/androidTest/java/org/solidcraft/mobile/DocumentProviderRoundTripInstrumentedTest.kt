package org.solidcraft.mobile

import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DocumentProviderRoundTripInstrumentedTest {
    @Test
    fun providerDocumentOpensEditsSavesBackAndReopensAfterActivityIsDestroyed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val providerDirectory = File(context.cacheDir, "solidcraft-share").apply { assertTrue(isDirectory || mkdirs()) }
        val destination = File(providerDirectory, "round-trip.solidcraft").apply { writeText("original provider document") }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", destination)
        val staged = File(context.filesDir, "solidcraft-documents/round-trip/design.solidcraft")
        val reopened = File(context.filesDir, "solidcraft-documents/round-trip-reopened/design.solidcraft")
        val preferences = context.getSharedPreferences("solidcraft_documents", android.content.Context.MODE_PRIVATE)
        val previousMapping = preferences.getString(staged.absolutePath, null)
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                DocumentStaging.replaceFrom(requireNotNull(input), staged)
            }
            assertEquals("original provider document", staged.readText())
            preferences.edit().putString(
                staged.absolutePath,
                JSONObject().put("uri", uri.toString()).put("writable", true).toString(),
            ).commit()

            // The native save worker may outlive its GameActivity, so invoke the same bridge after
            // closing the activity before verifying the provider contents and reopening them.
            ActivityScenario.launch(MainActivity::class.java).close()
            staged.writeText("edited complete document")
            assertEquals("", MainActivity.writeBackDocument(staged.absolutePath))
            assertEquals("edited complete document", destination.readText())

            context.contentResolver.openInputStream(uri).use { input ->
                DocumentStaging.replaceFrom(requireNotNull(input), reopened)
            }
            assertEquals(
                "The saved provider document should reopen byte-for-byte",
                staged.readBytes().toList(),
                reopened.readBytes().toList(),
            )
        } finally {
            val restore = preferences.edit()
            if (previousMapping == null) restore.remove(staged.absolutePath)
            else restore.putString(staged.absolutePath, previousMapping)
            restore.commit()
            staged.parentFile?.deleteRecursively()
            reopened.parentFile?.deleteRecursively()
            destination.delete()
            providerDirectory.delete()
        }
    }
}
