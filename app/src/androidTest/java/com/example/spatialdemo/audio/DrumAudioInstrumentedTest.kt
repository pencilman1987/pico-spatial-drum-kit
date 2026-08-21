package com.example.spatialdemo.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.spatialdemo.domain.model.DrumId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrumAudioInstrumentedTest {
    @Test
    fun everyLicensedSampleLoadsAndSubmitsForPlayback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audio = DrumAudioEngine(context)

        try {
            val deadlineMillis = System.currentTimeMillis() + LOAD_TIMEOUT_MILLIS
            while (!audio.status().isReady && System.currentTimeMillis() < deadlineMillis) {
                Thread.sleep(25)
            }

            val status = audio.status()
            assertEquals(DrumId.entries.size, status.licensedSampleCount)
            assertEquals(DrumId.entries.size, status.loadedSampleCount)
            DrumId.entries.forEach { drumId ->
                assertTrue("$drumId did not submit to SoundPool", audio.preview(drumId).submitted)
            }
        } finally {
            audio.release()
        }
    }

    private companion object {
        const val LOAD_TIMEOUT_MILLIS = 5_000L
    }
}
