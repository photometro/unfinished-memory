package com.unfinished.app.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.unfinished.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.runner.RunWith

/** Exercise MainActivity and WindowManager, not a mocked opacity flag. */
@RunWith(AndroidJUnit4::class)
class StartupWindowHandoffTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun rootTaskIsTranslucent(): Boolean? {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val name = Regex.escape(instrumentation.targetContext.packageName)
        val dump = instrumentation.uiAutomation.executeShellCommand("dumpsys activity activities")
            .use { descriptor ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)
                    .bufferedReader().use { it.readText() }
            }
        val task = Regex("(?m)^\\s*\\* Task\\{[^\\n]*A=\\d+:$name [^\\n]*translucent=(true|false)")
            .find(dump) ?: return null
        return task.groupValues[1].toBoolean()
    }

    @Test
    fun imageStartupHandsOffToAnOpaqueSystemTask() {
        compose.waitUntil(timeoutMillis = 10_000) { rootTaskIsTranslucent() == false }
        assertFalse("The image launch theme must not keep the live task translucent", rootTaskIsTranslucent()!!)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasTestTag("home.leave"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("home.leave").assertIsDisplayed()
        compose.onNodeWithTag("home.past").assertIsDisplayed()
    }
}
