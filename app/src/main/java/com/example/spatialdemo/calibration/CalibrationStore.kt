package com.example.spatialdemo.calibration

import android.content.Context
import com.example.spatialdemo.domain.model.DrumId
import com.pico.spatial.core.math.Vector3

class CalibrationStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): CalibrationProfile {
        val defaults = CalibrationProfile()
        val surfaceOffsets =
            DrumId.entries.associateWith { id ->
                Vector3(
                    preferences.getFloat("surface_${id.name}_x", 0f),
                    preferences.getFloat("surface_${id.name}_y", 0f),
                    preferences.getFloat("surface_${id.name}_z", 0f),
                )
            }.filterValues { it != Vector3.ZERO }

        return CalibrationProfile(
            minimumHitSpeed = preferences.getFloat("minimum_hit_speed", defaults.minimumHitSpeed),
            fullVelocitySpeed = preferences.getFloat("full_velocity_speed", defaults.fullVelocitySpeed),
            pinchGripThreshold = preferences.getFloat("pinch_grip_threshold", defaults.pinchGripThreshold),
            curlGripThreshold = preferences.getFloat("curl_grip_threshold", defaults.curlGripThreshold),
            virtualStickLength = preferences.getFloat("virtual_stick_length", defaults.virtualStickLength),
            rearmDistance = preferences.getFloat("rearm_distance", defaults.rearmDistance),
            cooldownMillis = preferences.getLong("cooldown_millis", defaults.cooldownMillis),
            kitOffset =
                Vector3(
                    preferences.getFloat("kit_offset_x", 0f),
                    preferences.getFloat("kit_offset_y", 0f),
                    preferences.getFloat("kit_offset_z", 0f),
                ),
            surfaceOffsets = surfaceOffsets,
            calibratedOnDevice = preferences.getBoolean("calibrated_on_device", false),
            measuredSoftwareLatencyMs =
                if (preferences.contains("software_latency_ms")) {
                    preferences.getFloat("software_latency_ms", 0f)
                } else {
                    null
                },
        ).sanitized()
    }

    fun save(profile: CalibrationProfile) {
        val value = profile.sanitized()
        preferences.edit().apply {
            putFloat("minimum_hit_speed", value.minimumHitSpeed)
            putFloat("full_velocity_speed", value.fullVelocitySpeed)
            putFloat("pinch_grip_threshold", value.pinchGripThreshold)
            putFloat("curl_grip_threshold", value.curlGripThreshold)
            putFloat("virtual_stick_length", value.virtualStickLength)
            putFloat("rearm_distance", value.rearmDistance)
            putLong("cooldown_millis", value.cooldownMillis)
            putFloat("kit_offset_x", value.kitOffset.x)
            putFloat("kit_offset_y", value.kitOffset.y)
            putFloat("kit_offset_z", value.kitOffset.z)
            putBoolean("calibrated_on_device", value.calibratedOnDevice)
            value.measuredSoftwareLatencyMs?.let { putFloat("software_latency_ms", it) }
                ?: remove("software_latency_ms")
            DrumId.entries.forEach { id ->
                val offset = value.surfaceOffsets[id] ?: Vector3.ZERO
                putFloat("surface_${id.name}_x", offset.x)
                putFloat("surface_${id.name}_y", offset.y)
                putFloat("surface_${id.name}_z", offset.z)
            }
            apply()
        }
    }

    fun reset(): CalibrationProfile {
        preferences.edit().clear().apply()
        return CalibrationProfile()
    }

    private companion object {
        const val PREFERENCES_NAME = "spatial_drum_calibration_v1"
    }
}
