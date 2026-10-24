package org.solidcraft.mobile

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLifecycleTest {
    @Test
    fun systemBarsAreHiddenAfterActivityResumes() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(500)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.onActivity { activity ->
            val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
            assertNotNull("Window insets should be available after resume", insets)
            assertFalse("Status bar should remain hidden", insets!!.isVisible(WindowInsetsCompat.Type.statusBars()))
            assertFalse("Navigation bar should remain hidden", insets.isVisible(WindowInsetsCompat.Type.navigationBars()))
        }

        // Keep GameActivity at STARTED; its native surface does not reliably transition to
        // DESTROYED through ActivityScenario.close() on all physical devices.
        scenario.moveToState(Lifecycle.State.STARTED)
    }
}
