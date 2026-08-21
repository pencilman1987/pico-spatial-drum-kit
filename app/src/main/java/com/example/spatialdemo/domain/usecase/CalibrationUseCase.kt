package com.example.spatialdemo.domain.usecase

import com.example.spatialdemo.calibration.CalibrationProfile
import com.example.spatialdemo.data.repository.CalibrationRepository

class CalibrationUseCase(private val repository: CalibrationRepository) {
    fun load(): CalibrationProfile = repository.load().sanitized()

    fun save(profile: CalibrationProfile): CalibrationProfile {
        val sanitized = profile.sanitized()
        repository.save(sanitized)
        return sanitized
    }

    fun reset(): CalibrationProfile = repository.reset().sanitized()
}
