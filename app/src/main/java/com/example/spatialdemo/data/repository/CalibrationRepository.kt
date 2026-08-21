package com.example.spatialdemo.data.repository

import android.content.Context
import com.example.spatialdemo.calibration.CalibrationProfile
import com.example.spatialdemo.calibration.CalibrationStore

interface CalibrationRepository {
    fun load(): CalibrationProfile

    fun save(profile: CalibrationProfile)

    fun reset(): CalibrationProfile
}

class SharedPreferencesCalibrationRepository(context: Context) : CalibrationRepository {
    private val store = CalibrationStore(context.applicationContext)

    override fun load(): CalibrationProfile = store.load()

    override fun save(profile: CalibrationProfile) = store.save(profile)

    override fun reset(): CalibrationProfile = store.reset()
}
