/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.service

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import cn.cjym.timesleep.data.model.StoryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

data class StoryNowPlaying(
    val story: StoryItem,
    val isPlaying: Boolean,
    val currentTime: Double = 0.0,
    val duration: Double = 0.0,
    val isSpeechMode: Boolean = false,
)

object StoryPlayerManager {
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var progressJob: Job? = null
    private var activeSpeechText = ""
    private var speechStartCharacter = 0
    private var speechCurrentCharacter = 0
    private var activeUtteranceId: String? = null

    private val _nowPlaying = MutableStateFlow<StoryNowPlaying?>(null)
    val nowPlaying: StateFlow<StoryNowPlaying?> = _nowPlaying.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        textToSpeech = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                textToSpeech?.language = Locale.CHINA
                textToSpeech?.setSpeechRate(0.9f)
                textToSpeech?.setPitch(0.98f)
            }
        }.also { tts ->
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    if (utteranceId != activeUtteranceId) return
                    scope.launch {
                        _nowPlaying.update { current ->
                            current?.copy(isPlaying = false, currentTime = current.duration)
                        }
                        stopProgress()
                    }
                }

                override fun onError(utteranceId: String?) {
                    if (utteranceId != activeUtteranceId) return
                    scope.launch { pause() }
                }

                override fun onRangeStart(
                    utteranceId: String?,
                    start: Int,
                    end: Int,
                    frame: Int,
                ) {
                    if (utteranceId != activeUtteranceId) return
                    speechCurrentCharacter = (speechStartCharacter + start)
                        .coerceIn(0, activeSpeechText.length)
                    scope.launch {
                        _nowPlaying.update { current ->
                            current?.copy(currentTime = timeForCharacter(speechCurrentCharacter, current.duration))
                        }
                    }
                }
            })
        }
    }

    fun isBundledOrCached(story: StoryItem): Boolean {
        return story.isBundled || cachedAudioFile(story).exists()
    }

    fun toggle(story: StoryItem, text: String) {
        val current = _nowPlaying.value
        if (current?.story?.id == story.id) {
            if (current.isPlaying) pause() else resume(story, text)
            return
        }

        if (isBundledOrCached(story)) {
            scope.launch { playAudio(story) }
        } else {
            playText(story, text)
        }
    }

    fun play(story: StoryItem, text: String) {
        val current = _nowPlaying.value
        if (current?.story?.id == story.id) {
            if (!current.isPlaying) resume(story, text)
            return
        }

        if (isBundledOrCached(story)) {
            scope.launch { playAudio(story) }
        } else {
            playText(story, text)
        }
    }

    fun playText(story: StoryItem, text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        stopInternal(clearState = false)
        AudioPlayerManager.stop()

        activeSpeechText = trimmed
        speechStartCharacter = 0
        speechCurrentCharacter = 0
        val duration = estimatedSpeechDuration(trimmed)
        _nowPlaying.value = StoryNowPlaying(
            story = story,
            isPlaying = true,
            currentTime = 0.0,
            duration = duration,
            isSpeechMode = true,
        )
        startPlaybackService()
        speakFrom(0)
    }

    fun pause() {
        val current = _nowPlaying.value ?: return
        if (current.isSpeechMode) {
            textToSpeech?.stop()
        } else {
            mediaPlayer?.pause()
        }
        _nowPlaying.update { it?.copy(isPlaying = false) }
        stopProgress()
    }

    fun resume(story: StoryItem, text: String) {
        val current = _nowPlaying.value
        if (current?.story?.id != story.id) {
            toggle(story, text)
            return
        }
        if (current.isSpeechMode) {
            speakFrom(speechCurrentCharacter)
        } else {
            mediaPlayer?.start()
            _nowPlaying.update { it?.copy(isPlaying = true) }
            startPlaybackService()
            startProgress()
        }
    }

    fun seek(seconds: Double) {
        val current = _nowPlaying.value ?: return
        val bounded = seconds.coerceIn(0.0, current.duration.coerceAtLeast(0.0))
        if (current.isSpeechMode) {
            val character = characterOffsetFor(bounded, current.duration)
            speechCurrentCharacter = character
            _nowPlaying.update { it?.copy(currentTime = bounded) }
            if (current.isPlaying) {
                speakFrom(character)
            }
        } else {
            mediaPlayer?.seekTo((bounded * 1000).toInt())
            _nowPlaying.update { it?.copy(currentTime = bounded) }
        }
    }

    fun stop() {
        stopInternal(clearState = true)
    }

    private suspend fun playAudio(story: StoryItem) {
        stopInternal(clearState = false)
        AudioPlayerManager.stop()
        val file = withContext(Dispatchers.IO) { resolveAudioFile(story) } ?: run {
            _nowPlaying.value = null
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { prepared ->
                    prepared.start()
                    _nowPlaying.update { it?.copy(isPlaying = true, duration = prepared.duration / 1000.0) }
                    startProgress()
                }
                setOnCompletionListener {
                    it.seekTo(0)
                    _nowPlaying.update { current -> current?.copy(isPlaying = false, currentTime = 0.0) }
                    stopProgress()
                }
                prepareAsync()
            }
            _nowPlaying.value = StoryNowPlaying(story = story, isPlaying = false)
            startPlaybackService()
        } catch (_: Exception) {
            _nowPlaying.value = null
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun speakFrom(character: Int) {
        val text = activeSpeechText
        if (text.isBlank() || !ttsReady) {
            _nowPlaying.update { it?.copy(isPlaying = false) }
            return
        }
        val start = character.coerceIn(0, text.length)
        if (start >= text.length) {
            _nowPlaying.update { current -> current?.copy(isPlaying = false, currentTime = current.duration) }
            stopProgress()
            return
        }
        speechStartCharacter = start
        speechCurrentCharacter = start
        activeUtteranceId = UUID.randomUUID().toString()
        textToSpeech?.stop()
        textToSpeech?.speak(
            text.substring(start),
            TextToSpeech.QUEUE_FLUSH,
            null,
            activeUtteranceId,
        )
        _nowPlaying.update { it?.copy(isPlaying = true) }
        startProgress()
    }

    private fun startProgress() {
        stopProgress()
        progressJob = scope.launch {
            while (true) {
                delay(200)
                val current = _nowPlaying.value ?: continue
                if (!current.isPlaying) continue
                if (current.isSpeechMode) {
                    _nowPlaying.update {
                        it?.copy(currentTime = timeForCharacter(speechCurrentCharacter, current.duration))
                    }
                } else {
                    val player = mediaPlayer ?: continue
                    _nowPlaying.update { it?.copy(currentTime = player.currentPosition / 1000.0) }
                }
            }
        }
    }

    private fun stopProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun stopInternal(clearState: Boolean) {
        stopProgress()
        activeUtteranceId = null
        textToSpeech?.stop()
        runCatching { mediaPlayer?.stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        activeSpeechText = ""
        speechStartCharacter = 0
        speechCurrentCharacter = 0
        if (clearState) {
            _nowPlaying.value = null
            appContext.stopService(Intent(appContext, StoryPlayerService::class.java))
        }
    }

    private fun startPlaybackService() {
        ContextCompat.startForegroundService(appContext, Intent(appContext, StoryPlayerService::class.java))
    }

    private fun resolveAudioFile(story: StoryItem): File? {
        val cached = cachedAudioFile(story)
        if (cached.exists()) return cached
        if (!story.isBundled) return null

        return runCatching {
            cached.parentFile?.mkdirs()
            appContext.assets.open(story.audioAssetPath).use { input ->
                FileOutputStream(cached).use { output -> input.copyTo(output) }
            }
            cached
        }.getOrNull()
    }

    private fun cachedAudioFile(story: StoryItem): File {
        val extension = story.audioFile.substringAfterLast('.', "m4a")
        return File(File(appContext.cacheDir, "Stories"), "${story.id}.$extension")
    }

    private fun estimatedSpeechDuration(text: String): Double {
        return maxOf(text.length / 4.2, 1.0)
    }

    private fun characterOffsetFor(time: Double, duration: Double): Int {
        if (duration <= 0 || activeSpeechText.isEmpty()) return 0
        val progress = (time / duration).coerceIn(0.0, 1.0)
        return (activeSpeechText.length * progress).toInt().coerceIn(0, activeSpeechText.length)
    }

    private fun timeForCharacter(character: Int, duration: Double): Double {
        if (activeSpeechText.isEmpty()) return 0.0
        val progress = (character.toDouble() / activeSpeechText.length.toDouble()).coerceIn(0.0, 1.0)
        return duration * progress
    }
}
