package org.solidcraft.mobile

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.SystemClock
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MainActivityLifecycleTest {
    @Test
    fun meshShareProviderExposesOnlyTheDedicatedCacheFolder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val shareDirectory = File(context.cacheDir, "solidcraft-share").apply { assertTrue(isDirectory || mkdirs()) }
        val file = File(shareDirectory, "provider-test.stl").apply { writeText("solid test") }
        val privateFile = File(context.cacheDir, "not-shareable.txt").apply { writeText("private") }
        try {
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            assertEquals("content", uri.scheme)
            assertTrue(uri.path.orEmpty().contains("solidcraft_mesh_exports"))
            var privateFileRejected = false
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.files", privateFile)
            } catch (_: IllegalArgumentException) {
                privateFileRejected = true
            }
            assertTrue("Files outside the export cache must not be exposed", privateFileRejected)
        } finally {
            file.delete()
            privateFile.delete()
            shareDirectory.delete()
        }
    }

    @Test
    fun activityAndViewportHostSurviveRotationAndKeepSystemBarsHidden() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(500)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        var originalActivity: MainActivity? = null
        var originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            assertTrue("GameActivity's window should stay attached while paused", activity.window.decorView.isAttachedToWindow)
            assertTrue("Activity should remain started while paused", activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
            assertFalse("Activity should leave RESUMED while paused", activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.onActivity { activity ->
            originalActivity = activity
            originalRequestedOrientation = activity.requestedOrientation
            assertTrue("Activity should resume after the pause transition", activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            assertTrue("GameActivity's window should remain attached after resume", activity.window.decorView.isAttachedToWindow)
            assertSystemBarsHidden(activity, "after resume")
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        try {
            val deadline = SystemClock.uptimeMillis() + 10_000
            var rotated = false
            while (!rotated && SystemClock.uptimeMillis() < deadline) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { activity ->
                    rotated = activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                }
                if (!rotated) SystemClock.sleep(100)
            }
            assertTrue("Activity should receive the requested landscape configuration", rotated)

            scenario.onActivity { activity ->
                assertSame("Manifest configChanges should preserve the native host Activity", originalActivity, activity)
                assertTrue("GameActivity's window should remain attached", activity.window.decorView.isAttachedToWindow)
                assertTrue("Activity should resume after rotation", activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                assertSystemBarsHidden(activity, "after rotation")
            }
        } finally {
            scenario.onActivity { activity -> activity.requestedOrientation = originalRequestedOrientation }
            scenario.moveToState(Lifecycle.State.STARTED)
        }
    }

    private fun assertSystemBarsHidden(activity: MainActivity, moment: String) {
        val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
        assertNotNull("Window insets should be available $moment", insets)
        assertFalse("Status bar should remain hidden $moment", insets!!.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertFalse("Navigation bar should remain hidden $moment", insets.isVisible(WindowInsetsCompat.Type.navigationBars()))
    }
}
