package com.example.spatialdemo.domain.usecase

import com.example.spatialdemo.calibration.CalibrationProfile
import com.example.spatialdemo.data.repository.CalibrationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationUseCaseTest {
    @Test
    fun loadSanitizesPersistedThresholds() {
        val repository = FakeCalibrationRepository(CalibrationProfile(minimumHitSpeed = -4f))

        val loaded = CalibrationUseCase(repository).load()

        assertEquals(0.1f, loaded.minimumHitSpeed)
    }

    @Test
    fun savePersistsTheSanitizedProfile() {
        val repository = FakeCalibrationRepository(CalibrationProfile())

        val saved = CalibrationUseCase(repository).save(CalibrationProfile(virtualStickLength = 2f))

        assertEquals(0.5f, saved.virtualStickLength)
        assertEquals(saved, repository.current)
        assertTrue(repository.saveCalled)
    }

    private class FakeCalibrationRepository(
        var current: CalibrationProfile,
    ) : CalibrationRepository {
        var saveCalled = false

        override fun load(): CalibrationProfile = current

        override fun save(profile: CalibrationProfile) {
            saveCalled = true
            current = profile
        }

        override fun reset(): CalibrationProfile {
            current = CalibrationProfile()
            return current
        }
    }
}
