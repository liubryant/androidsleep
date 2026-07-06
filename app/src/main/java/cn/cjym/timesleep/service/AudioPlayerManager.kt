/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.service

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.core.content.ContextCompat
import cn.cjym.timesleep.AppConstants
import cn.cjym.timesleep.data.model.SoundScene
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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class NowPlaying(
    val scene: SoundScene,
    val isPlaying: Boolean,
    val sleepTimerText: String? = null,
)

object AudioPlayerManager {
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client by lazy { OkHttpClient() }

    private var mediaPlayer: MediaPlayer? = null
    private var playJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun toggle(scene: SoundScene) {
        val current = _nowPlaying.value
        if (current?.scene?.id == scene.id) {
            if (current.isPlaying) pause() else resume()
            return
        }

        playJob?.cancel()
        playJob = scope.launch { play(scene) }
    }

    fun pause() {
        mediaPlayer?.pause()
        _nowPlaying.update { it?.copy(isPlaying = false) }
    }

    fun resume() {
        mediaPlayer?.start()
        _nowPlaying.update { it?.copy(isPlaying = true) }
        startPlaybackService()
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        releasePlayer()
        _nowPlaying.value = null
        cancelSleepTimer()
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _nowPlaying.update { it?.copy(sleepTimerText = "$minutes 分钟后停止") }
        sleepTimerJob = scope.launch {
            delay(minutes * 60_000L)
            stop()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _nowPlaying.update { it?.copy(sleepTimerText = null) }
    }

    fun cachedAudioExists(scene: SoundScene): Boolean {
        return cachedAudioFile(scene).exists()
    }

    fun isDownloading(scene: SoundScene): Boolean {
        return _downloadProgress.value.containsKey(scene.id)
    }

    private suspend fun play(scene: SoundScene) {
        val sleepTimerText = _nowPlaying.value?.scene?.id?.let { if (it == scene.id) _nowPlaying.value?.sleepTimerText else null }
        withContext(Dispatchers.Main) {
            releasePlayer()
            _nowPlaying.value = NowPlaying(scene = scene, isPlaying = false, sleepTimerText = sleepTimerText)
            startPlaybackService()
        }

        val source = runCatching { resolveSource(scene) }.getOrNull()
        if (source == null) {
            withContext(Dispatchers.Main) {
                if (_nowPlaying.value?.scene?.id == scene.id) _nowPlaying.value = null
            }
            return
        }

        withContext(Dispatchers.Main) {
            if (_nowPlaying.value?.scene?.id != scene.id) return@withContext
            try {
                mediaPlayer = MediaPlayer().apply {
                    isLooping = true
                    when (source) {
                        is PlaybackSource.Asset -> {
                            val afd = appContext.assets.openFd(source.path)
                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                        }
                        is PlaybackSource.File -> setDataSource(source.file.absolutePath)
                    }
                    setOnPreparedListener {
                        it.start()
                        _nowPlaying.update { current -> current?.copy(isPlaying = true) }
                    }
                    setOnCompletionListener {
                        _nowPlaying.update { current -> current?.copy(isPlaying = false) }
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                _nowPlaying.value = null
            }
        }
    }

    private sealed interface PlaybackSource {
        data class Asset(val path: String) : PlaybackSource
        data class File(val file: java.io.File) : PlaybackSource
    }

    private suspend fun resolveSource(scene: SoundScene): PlaybackSource {
        if (scene.isBundledAudio) {
            return PlaybackSource.Asset(scene.audioAssetPath)
        }

        val cached = cachedAudioFile(scene)
        if (cached.exists()) {
            return PlaybackSource.File(cached)
        }

        return PlaybackSource.File(download(scene, cached))
    }

    private fun cachedAudioFile(scene: SoundScene): File {
        val dir = File(appContext.cacheDir, "Sounds")
        return File(dir, "${scene.id}.${scene.audioExtension}")
    }

    private suspend fun download(scene: SoundScene, destination: File): File = withContext(Dispatchers.IO) {
        destination.parentFile?.mkdirs()
        val tempFile = File(destination.parentFile, "${destination.name}.download")

        _downloadProgress.update { it + (scene.id to 0f) }
        try {
            val url = "${AppConstants.SOUND_SERVER_BASE_URL}/${scene.id}/download"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("download failed: ${response.code}")
                val body = response.body ?: error("empty body")
                val total = body.contentLength()
                var received = 0L

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            received += read
                            val progress = if (total > 0) (received.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0.1f
                            _downloadProgress.update { it + (scene.id to progress) }
                        }
                    }
                }
            }

            if (destination.exists()) destination.delete()
            tempFile.renameTo(destination)
            destination
        } finally {
            _downloadProgress.update { it - scene.id }
        }
    }

    /** 启动播放前台服务，使锁屏与通知栏展示当前播放状态。 */
    private fun startPlaybackService() {
        ContextCompat.startForegroundService(appContext, Intent(appContext, AudioPlayerService::class.java))
    }

    private fun releasePlayer() {
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null
    }
}
