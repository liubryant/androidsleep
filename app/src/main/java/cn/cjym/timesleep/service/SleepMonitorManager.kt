package cn.cjym.timesleep.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import cn.cjym.timesleep.data.model.NoiseSample
import cn.cjym.timesleep.data.model.SleepEvent
import cn.cjym.timesleep.data.model.SleepEventType
import cn.cjym.timesleep.data.model.SleepSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * 睡眠监测的单例状态容器与音频采集逻辑，对应 iOS `SleepMonitorService`
 * （`@MainActor ObservableObject`）。实际的 `AudioRecord` 采集在前台
 * [SleepMonitorService] 启动期间运行。
 */
object SleepMonitorManager {
    private const val SAMPLE_RATE = 16_000
    private const val BUFFER_SIZE_SAMPLES = 4_096

    private const val BREATHING_AUDIBLE_THRESHOLD = 30.0
    private const val SILENCE_THRESHOLD = 24.0
    private const val BREATH_HOLDING_MIN_DURATION_MS = 8_000L
    private const val EVENT_COOLDOWN_MS = 20_000L
    private const val MAX_NOISE_SAMPLES = 720
    private const val PERSIST_INTERVAL_MS = 30_000L

    /** 睡眠时长不超过该阈值时，不生成睡眠报告。 */
    private const val MINIMUM_REPORT_DURATION_MS = 2 * 60 * 60 * 1_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val classifier: SoundClassifying = FeatureBasedSleepSoundClassifier()

    private lateinit var appContext: Context
    private var initialized = false

    private var recordJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var wavWriter: WavFileWriter? = null

    private val lastEventTimeByType = mutableMapOf<SleepEventType, Long>()
    private var lastPersistTime = 0L
    private var silenceStart: Long? = null
    private var wasBreathingAudible = false

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _currentSession = MutableStateFlow<SleepSession?>(null)
    val currentSession: StateFlow<SleepSession?> = _currentSession.asStateFlow()

    private val _latestSession = MutableStateFlow<SleepSession?>(null)
    val latestSession: StateFlow<SleepSession?> = _latestSession.asStateFlow()

    private val _sessions = MutableStateFlow<List<SleepSession>>(emptyList())
    val sessions: StateFlow<List<SleepSession>> = _sessions.asStateFlow()

    private val _currentDecibel = MutableStateFlow(0.0)
    val currentDecibel: StateFlow<Double> = _currentDecibel.asStateFlow()

    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        _sessions.value = SleepSessionStore.loadSessions(appContext)
        _latestSession.value = _sessions.value.firstOrNull()
    }

    fun start(context: Context) {
        if (_isMonitoring.value) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _permissionDenied.value = true
            return
        }

        try {
            beginSession()
            ContextCompat.startForegroundService(context, Intent(context, SleepMonitorService::class.java))
        } catch (e: Exception) {
            stop(context)
        }
    }

    fun stop(context: Context) {
        if (!_isMonitoring.value && _currentSession.value == null) return
        endSession()
        context.stopService(Intent(context, SleepMonitorService::class.java))
    }

    fun select(session: SleepSession) {
        if (_isMonitoring.value) return
        _latestSession.value = session
    }

    fun dismissPermissionAlert() {
        _permissionDenied.value = false
    }

    /** 清除所有已保存的睡眠报告、事件数据和录音文件。 */
    fun clearAllData(context: Context) {
        if (_isMonitoring.value) stop(context)
        _sessions.value = emptyList()
        _latestSession.value = null
        SleepSessionStore.saveSessions(appContext, emptyList())
        SleepSessionStore.clearRecordings(appContext)
    }

    private fun beginSession() {
        val sessionId = java.util.UUID.randomUUID().toString()
        val recordingFile = SleepSessionStore.makeRecordingFile(appContext, sessionId)
        val session = SleepSession(id = sessionId, audioFileName = recordingFile.name)

        _currentSession.value = session
        _latestSession.value = session
        _sessions.value = SleepSessionStore.upsert(session, _sessions.value)
        persistSessions(force = true)

        lastEventTimeByType.clear()
        silenceStart = null
        wasBreathingAudible = false

        startCapture(recordingFile)
        _isMonitoring.value = true
    }

    private fun endSession() {
        stopCapture()
        val session = _currentSession.value
        if (session != null) {
            val finished = session.copy(endTime = System.currentTimeMillis())
            val durationMs = finished.endTime!! - finished.startTime
            if (durationMs > MINIMUM_REPORT_DURATION_MS) {
                _latestSession.value = finished
                _sessions.value = SleepSessionStore.upsert(finished, _sessions.value)
            } else {
                // 睡眠时长不超过 2 小时，不生成睡眠报告，丢弃本次记录与录音。
                _sessions.value = SleepSessionStore.remove(finished, _sessions.value)
                finished.audioFileName?.let { SleepSessionStore.deleteRecording(appContext, it) }
                _latestSession.value = _sessions.value.firstOrNull()
            }
            persistSessions(force = true)
        }
        _currentSession.value = null
        _isMonitoring.value = false
    }

    @SuppressLint("MissingPermission")
    private fun startCapture(recordingFile: java.io.File) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSizeBytes = maxOf(minBufferSize, BUFFER_SIZE_SAMPLES * 2)

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeBytes,
        )

        val writer = WavFileWriter(recordingFile, SAMPLE_RATE)
        writer.open()

        audioRecord = record
        wavWriter = writer
        record.startRecording()

        recordJob = scope.launch {
            val pcmBuffer = ShortArray(BUFFER_SIZE_SAMPLES)
            val floatBuffer = FloatArray(BUFFER_SIZE_SAMPLES)
            while (isActive) {
                val read = record.read(pcmBuffer, 0, pcmBuffer.size)
                if (read <= 0) continue

                writer.write(pcmBuffer, read)
                for (i in 0 until read) {
                    floatBuffer[i] = pcmBuffer[i] / 32_768f
                }

                val chunk = if (read == floatBuffer.size) floatBuffer else floatBuffer.copyOf(read)
                processChunk(chunk)
            }
        }
    }

    private fun stopCapture() {
        recordJob?.cancel()
        recordJob = null

        audioRecord?.let { record ->
            try {
                record.stop()
            } catch (e: IllegalStateException) {
                // 录音已经停止，忽略。
            }
            record.release()
        }
        audioRecord = null

        wavWriter?.close()
        wavWriter = null
    }

    private fun processChunk(samples: FloatArray) {
        val session = _currentSession.value ?: return
        val features = AudioFeatureExtractor.features(samples, SAMPLE_RATE.toFloat()) ?: return
        _currentDecibel.value = features.estimatedDecibel

        val now = System.currentTimeMillis()
        val noiseSamples = session.noiseSamples.toMutableList()
        noiseSamples.add(NoiseSample(time = now, decibel = features.estimatedDecibel))
        if (noiseSamples.size > MAX_NOISE_SAMPLES) {
            repeat(noiseSamples.size - MAX_NOISE_SAMPLES) { noiseSamples.removeAt(0) }
        }

        val events = session.events.toMutableList()

        val result = classifier.classify(features)
        if (result != null && canAppendEvent(result.type, now)) {
            events.add(
                SleepEvent(
                    type = result.type,
                    startTime = now - 2_000,
                    endTime = now,
                    confidence = result.confidence,
                    peakDecibel = features.estimatedDecibel,
                ),
            )
            lastEventTimeByType[result.type] = now
        }

        detectBreathHolding(features.estimatedDecibel, now, events)

        val updatedSession = session.copy(noiseSamples = noiseSamples, events = events)
        _currentSession.value = updatedSession
        _latestSession.value = updatedSession
        _sessions.value = SleepSessionStore.upsert(updatedSession, _sessions.value)
        persistSessions(force = false)
    }

    /** 在持续可听到呼吸声之后出现一段明显静音，视为一次憋气/呼吸暂停事件。 */
    private fun detectBreathHolding(decibel: Double, now: Long, events: MutableList<SleepEvent>) {
        if (decibel < SILENCE_THRESHOLD) {
            if (wasBreathingAudible && silenceStart == null) {
                silenceStart = now
            }
            return
        }

        val start = silenceStart
        if (start != null) {
            val holdDurationMs = now - start
            if (holdDurationMs >= BREATH_HOLDING_MIN_DURATION_MS && canAppendEvent(SleepEventType.breathHolding, now)) {
                val confidence = min(0.92, 0.5 + holdDurationMs / 30_000.0)
                events.add(
                    SleepEvent(
                        type = SleepEventType.breathHolding,
                        startTime = start,
                        endTime = now,
                        confidence = confidence,
                        peakDecibel = decibel,
                    ),
                )
                lastEventTimeByType[SleepEventType.breathHolding] = now
            }
            silenceStart = null
        }

        if (decibel > BREATHING_AUDIBLE_THRESHOLD) {
            wasBreathingAudible = true
        }
    }

    private fun canAppendEvent(type: SleepEventType, now: Long): Boolean {
        val last = lastEventTimeByType[type] ?: return true
        return now - last > EVENT_COOLDOWN_MS
    }

    private fun persistSessions(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastPersistTime <= PERSIST_INTERVAL_MS) return
        lastPersistTime = now
        SleepSessionStore.saveSessions(appContext, _sessions.value)
    }
}
