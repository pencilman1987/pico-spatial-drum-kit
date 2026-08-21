package com.example.spatialdemo.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.spatialdemo.domain.model.DrumId
import com.example.spatialdemo.domain.model.DrumKit
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

data class AudioPlayTelemetry(
    val trackingToSubmissionMs: Float,
    val submitted: Boolean,
)

data class DrumAudioStatus(
    val licensedSampleCount: Int,
    val loadedSampleCount: Int,
    val totalSampleCount: Int,
) {
    val isReady: Boolean
        get() = loadedSampleCount == totalSampleCount

    val label: String
        get() =
            if (licensedSampleCount == totalSampleCount && isReady) {
                "授权 $licensedSampleCount/$totalSampleCount · 音频就绪"
            } else if (licensedSampleCount == totalSampleCount) {
                "授权 $licensedSampleCount/$totalSampleCount · 加载 $loadedSampleCount/$totalSampleCount"
            } else {
                "临时合成音 · 授权 $licensedSampleCount/$totalSampleCount · 加载 $loadedSampleCount/$totalSampleCount"
            }
}

class DrumAudioEngine(context: Context) {
    private val appContext = context.applicationContext
    private val soundPool =
        SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            ).build()
    private val sounds = mutableMapOf<DrumId, Int>()
    private val loaded = mutableSetOf<Int>()
    private val playCounters = mutableMapOf<DrumId, Int>()
    private var licensedSampleCount = 0
    @Volatile private var surfaceX = DrumKit.surfaces.associate { it.id to it.center.x }

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) synchronized(loaded) { loaded += sampleId }
        }
        val licensedSamples = readLicensedSamplePaths()
        DrumId.entries.forEach { id ->
            val licensedPath = licensedSamples[id]
            val sampleId =
                if (licensedPath != null) {
                    runCatching {
                        appContext.assets.openFd(licensedPath).use { descriptor ->
                            soundPool.load(descriptor, 1)
                        }
                    }.getOrNull()
                } else {
                    null
                }
            if (sampleId != null && sampleId != 0) {
                licensedSampleCount += 1
                sounds[id] = sampleId
            } else {
                val file = File(appContext.cacheDir, "spatial_drum_${id.name.lowercase()}.wav")
                if (!file.exists()) writeWave(file, synthesize(id))
                sounds[id] = soundPool.load(file.absolutePath, 1)
            }
        }
    }

    fun play(id: DrumId, intensity: Float, trackingTimestampNanos: Long): AudioPlayTelemetry {
        val soundId = sounds[id]
            ?: return AudioPlayTelemetry(elapsedSince(trackingTimestampNanos), submitted = false)
        if (synchronized(loaded) { soundId !in loaded }) {
            return AudioPlayTelemetry(elapsedSince(trackingTimestampNanos), submitted = false)
        }
        val x = (surfaceX[id] ?: 0f).coerceIn(-1f, 1f)
        val gain = (0.35f + intensity * 0.65f).coerceIn(0f, 1f)
        val left = gain * if (x > 0f) 1f - x * 0.45f else 1f
        val right = gain * if (x < 0f) 1f + x * 0.45f else 1f
        val rate = nextPlaybackRate(id)
        val streamId = soundPool.play(soundId, left, right, 1, 0, rate)
        return AudioPlayTelemetry(
            trackingToSubmissionMs = elapsedSince(trackingTimestampNanos),
            submitted = streamId != 0,
        )
    }

    fun preview(id: DrumId): AudioPlayTelemetry = play(id, intensity = 0.72f, trackingTimestampNanos = System.nanoTime())

    fun status(): DrumAudioStatus {
        val loadedCount = synchronized(loaded) { sounds.values.count(loaded::contains) }
        return DrumAudioStatus(licensedSampleCount, loadedCount, DrumId.entries.size)
    }

    fun updateSurfaces(surfaces: List<com.example.spatialdemo.domain.model.DrumSurface>) {
        surfaceX = surfaces.associate { it.id to it.center.x }
    }

    fun release() = soundPool.release()

    private fun nextPlaybackRate(id: DrumId): Float =
        synchronized(playCounters) {
            val next = (playCounters[id] ?: 0) + 1
            playCounters[id] = next
            when (next % 4) {
                0 -> 0.985f
                1 -> 1.0f
                2 -> 1.012f
                else -> 0.994f
            }
        }

    private fun elapsedSince(timestampNanos: Long) =
        ((System.nanoTime() - timestampNanos).coerceAtLeast(0L) / 1_000_000f)

    private fun readLicensedSamplePaths(): Map<DrumId, String> =
        runCatching {
            val manifest =
                appContext.assets.open(LICENSE_MANIFEST_PATH).bufferedReader().use { it.readText() }
            val samples = JSONObject(manifest).getJSONArray("samples")
            buildMap {
                repeat(samples.length()) { index ->
                    val sample = samples.getJSONObject(index)
                    val id = DrumId.valueOf(sample.getString("drumId"))
                    val file = sample.getString("file")
                    val license = sample.getString("license")
                    val sourceUrl = sample.getString("sourceUrl")
                    val licenseUrl = sample.getString("licenseUrl")
                    val sha256 = sample.getString("sha256").lowercase()
                    if (
                        file.startsWith("drums/") &&
                            license.isNotBlank() &&
                            sourceUrl.startsWith("https://") &&
                            licenseUrl.startsWith("https://") &&
                            sha256.matches(Regex("[0-9a-f]{64}")) &&
                            assetSha256(file) == sha256
                    ) {
                        put(id, file)
                    }
                }
            }
        }.getOrDefault(emptyMap())

    private fun assetSha256(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        appContext.assets.open(path).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun synthesize(id: DrumId): ShortArray {
        val sampleRate = 44_100
        val duration = if (id == DrumId.CRASH || id == DrumId.RIDE) 0.72f else 0.42f
        val count = (sampleRate * duration).toInt()
        val random = Random(id.ordinal * 41 + 7)
        var filteredNoise = 0f
        return ShortArray(count) { index ->
            val t = index / sampleRate.toFloat()
            val noise = random.nextFloat() * 2f - 1f
            filteredNoise = filteredNoise * 0.65f + noise * 0.35f
            val value =
                when (id) {
                    DrumId.KICK -> {
                        val frequency = 125f * exp(-t * 8f) + 43f
                        sin(2f * PI.toFloat() * frequency * t) * exp(-t * 11f)
                    }
                    DrumId.SNARE ->
                        (noise * 0.78f + sin(2f * PI.toFloat() * 185f * t) * 0.22f) * exp(-t * 15f)
                    DrumId.HI_HAT -> (noise - filteredNoise) * exp(-t * 32f)
                    DrumId.CRASH, DrumId.RIDE ->
                        (noise - filteredNoise * 0.4f) * exp(-t * if (id == DrumId.CRASH) 5.2f else 4.3f)
                    DrumId.TOM_HIGH -> sin(2f * PI.toFloat() * 205f * t) * exp(-t * 10f)
                    DrumId.TOM_MID -> sin(2f * PI.toFloat() * 155f * t) * exp(-t * 9f)
                    DrumId.TOM_FLOOR -> sin(2f * PI.toFloat() * 108f * t) * exp(-t * 8f)
                }
            (value.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.82f).toInt().toShort()
        }
    }

    private fun writeWave(file: File, samples: ShortArray) {
        val dataSize = samples.size * 2
        BufferedOutputStream(FileOutputStream(file)).use { out ->
            out.write("RIFF".encodeToByteArray())
            out.writeLittleEndian(36 + dataSize, 4)
            out.write("WAVEfmt ".encodeToByteArray())
            out.writeLittleEndian(16, 4)
            out.writeLittleEndian(1, 2)
            out.writeLittleEndian(1, 2)
            out.writeLittleEndian(44_100, 4)
            out.writeLittleEndian(44_100 * 2, 4)
            out.writeLittleEndian(2, 2)
            out.writeLittleEndian(16, 2)
            out.write("data".encodeToByteArray())
            out.writeLittleEndian(dataSize, 4)
            samples.forEach { out.writeLittleEndian(it.toInt(), 2) }
        }
    }

    private fun BufferedOutputStream.writeLittleEndian(value: Int, byteCount: Int) {
        repeat(byteCount) { shift -> write(value shr (shift * 8) and 0xFF) }
    }

    private companion object {
        const val LICENSE_MANIFEST_PATH = "drums/LICENSES.json"
    }
}
