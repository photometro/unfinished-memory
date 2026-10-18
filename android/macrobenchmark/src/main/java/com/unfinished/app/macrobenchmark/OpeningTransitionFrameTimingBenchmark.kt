package com.unfinished.app.macrobenchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMacrobenchmarkApi::class)
class OpeningTransitionFrameTimingBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldOpenThroughTidalHandoff() = benchmarkRule.measureRepeated(
        packageName = TARGET_APPLICATION_ID,
        metrics = listOf(FrameTimingMetric()),
        // Flyme blocks Macrobenchmark's default uninstall/reinstall compilation reset.
        // Reuse the already-installed isolated target; do not touch the user's app.
        compilationMode = CompilationMode.Ignore(),
        iterations = 10,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
        val homeHeader = device.wait(Until.findObject(By.res("home.header")), HOME_READY_TIMEOUT_MILLIS)
        assertNotNull("Home did not become accessible after the automatic shoreline transition", homeHeader)
    }

    private companion object {
        const val TARGET_APPLICATION_ID = "com.unfinished.app.native11benchmark"
        const val HOME_READY_TIMEOUT_MILLIS = 8_000L
    }
}
