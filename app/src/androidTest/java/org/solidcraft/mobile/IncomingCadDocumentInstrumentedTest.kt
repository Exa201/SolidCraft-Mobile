package org.solidcraft.mobile

import android.content.Intent
import android.os.SystemClock
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class IncomingCadDocumentInstrumentedTest {
    @Test
    fun sharedCadFileIsStagedOpenedAndAddedToRecent() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val shareDirectory = File(context.cacheDir, "solidcraft-share").apply { check(isDirectory || mkdirs()) }
        val source = File(shareDirectory, "shared-intent.solidcraft")
        val sendSource = File(shareDirectory, "shared-send.solidcraft")
        instrumentation.context.assets.open("intent-fixture.solidcraft").use { input ->
            source.outputStream().use(input::copyTo)
        }
        instrumentation.context.assets.open("intent-fixture.solidcraft").use { input ->
            sendSource.outputStream().use(input::copyTo)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", source)
        val sendUri = FileProvider.getUriForFile(context, "${context.packageName}.files", sendSource)
        val uriKey = MessageDigest.getInstance("SHA-256")
            .digest(uri.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        val stagedDirectory = File(context.filesDir, "solidcraft-documents/$uriKey")
        val staged = File(stagedDirectory, source.name)
        val sendKey = MessageDigest.getInstance("SHA-256")
            .digest(sendUri.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        val sendStagedDirectory = File(context.filesDir, "solidcraft-documents/$sendKey")
        val sendStaged = File(sendStagedDirectory, sendSource.name)
        val preferences = context.getSharedPreferences("solidcraft_documents", android.content.Context.MODE_PRIVATE)
        val oldRecent = preferences.getString("recent_documents", null)
        val intent = Intent(Intent.ACTION_VIEW)
            .setClass(context, MainActivity::class.java)
            .setDataAndType(uri, "application/x-solidcraft")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        try {
            val deadline = SystemClock.uptimeMillis() + 30_000
            while (!staged.isFile && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
            assertTrue("Incoming CAD content should be staged privately", staged.isFile)
            assertTrue("The staged fixture should remain a complete CAD archive", staged.length() > 250)

            var opened = false
            while (!opened && SystemClock.uptimeMillis() < deadline) {
                opened = preferences.getString("recent_documents", "[]").orEmpty().contains(staged.absolutePath)
                if (!opened) SystemClock.sleep(100)
            }
            assertTrue("Rust should open the shared design and persist it to Recent", opened)

            val sendIntent = Intent(Intent.ACTION_SEND)
                .setClass(context, MainActivity::class.java)
                .setType("application/x-solidcraft")
                .putExtra(Intent.EXTRA_STREAM, sendUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            scenario.onActivity { activity -> activity.handleIncomingDocument(sendIntent) }
            var shared = false
            while (!shared && SystemClock.uptimeMillis() < deadline) {
                shared = preferences.getString("recent_documents", "[]").orEmpty().contains(sendStaged.absolutePath)
                if (!shared) SystemClock.sleep(100)
            }
            assertTrue("ACTION_SEND should open the document and persist Recent", shared)
        } finally {
            scenario.close()
            val restore = preferences.edit().remove(staged.absolutePath)
                .remove(sendStaged.absolutePath)
            if (oldRecent == null) restore.remove("recent_documents") else restore.putString("recent_documents", oldRecent)
            restore.commit()
            stagedDirectory.deleteRecursively()
            sendStagedDirectory.deleteRecursively()
            source.delete()
            sendSource.delete()
            shareDirectory.delete()
        }
    }
}
