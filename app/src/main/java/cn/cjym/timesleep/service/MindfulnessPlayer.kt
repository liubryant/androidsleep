package cn.cjym.timesleep.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable data class MeditationTrack(val id: String, val title: String, val subtitle: String, val cover: Int, val asset: String = "", val downloadUrl: String = "", val requiresVIP: Boolean = false, val coverAsset: String = "")
@Serializable data class RemoteMeditation(val id: Int, val title: String, val downloadUrl: String, val sizeBytes: Long? = null)
@Serializable data class MeditationGroup(val id: Int, val name: String, val description: String? = null, val sortOrder: Int? = null, val items: List<RemoteMeditation>)
data class MeditationPlayback(val track: MeditationTrack, val playing: Boolean = false, val loading: Boolean = true, val position: Float = 0f, val duration: Float = 0f, val remainingSeconds: Int? = null, val error: String? = null)

object MeditationLibrary {
    private fun local(id: String, title: String, subtitle: String, cover: Int, file: String, vip: Boolean = true) = MeditationTrack(id, title, subtitle, cover, "SoundResources/Mindfulness/$file.mp3", requiresVIP = vip)
    val bundled = listOf(
        local("deep-sleep-healing", "深度睡眠疗愈", "正念引导 · 安稳入眠", 1, "深度睡眠", false),
        local("daytime-energy", "日间提神", "正念引导 · 清醒专注", 20, "日间提神", false),
        local("walking-meditation", "散步冥想", "正念引导 · 感受脚下", 21, "散步冥想"),
        local("emotional-release", "睡前释放情绪", "正念引导 · 温柔放下", 28, "睡前释放情绪"),
        local("calming-sleep", "安定身心助眠", "正念引导 · 舒缓身心", 4, "rachel安定身心助眠"),
        local("fatigue-relief", "缓解疲倦", "正念引导 · 恢复能量", 8, "caroline缓解疲倦给身心一次温柔的休息"),
        local("quick-sleep", "快速入睡", "正念引导 · 沉入梦乡", 22, "suyue快速入睡｜让意识慢慢沉入夜晚"),
        local("exam-sleep", "考前失眠解放", "正念引导 · 缓解紧张", 13, "nan考前失眠解放｜放下焦虑，安心入眠"))
    val groups = MutableStateFlow<List<Pair<String, List<MeditationTrack>>>>(emptyList())
    val groupDescriptions = MutableStateFlow<Map<String,String>>(emptyMap())
    private val natureTracks = mutableMapOf<String,MeditationTrack>()
    fun natureTrack(scene: cn.cjym.timesleep.data.model.SoundScene): MeditationTrack {
        val track = MeditationTrack("nature-${scene.id}",scene.title,scene.subtitle.ifBlank { "聆听自然，安然入眠" },1,
            asset=if(scene.isBundledAudio) scene.audioAssetPath else "",
            downloadUrl="${cn.cjym.timesleep.AppConstants.SOUND_SERVER_BASE_URL}/${scene.id}/download",
            requiresVIP=scene.requiresVIP,coverAsset=scene.coverAssetPath)
        natureTracks[track.id]=track
        return track
    }
    val error = MutableStateFlow<String?>(null)
    private val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }
    fun find(id: String) = (bundled + groups.value.flatMap { it.second }).firstOrNull { it.id == id } ?: natureTracks[id]
    suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        if (groups.value.isNotEmpty() && error.value == null) return@withContext
        val cache = File(context.filesDir, "meditation-catalog.json")
        fun request(path: String): String = client.newCall(Request.Builder().url("https://www.cjym123.cn/api/mindfulness$path").build()).execute().use {
            check(it.isSuccessful) { "HTTP ${it.code}" }; it.body!!.string()
        }
        try {
            val raw = try { request("/grouped") } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                val legacy = json.decodeFromString<List<RemoteMeditation>>(request(""))
                json.encodeToString(kotlinx.serialization.builtins.ListSerializer(MeditationGroup.serializer()), listOf(MeditationGroup(0, "正念冥想", items = legacy)))
            }
            val parsed = json.decodeFromString<List<MeditationGroup>>(raw)
            cache.writeText(raw); setGroups(parsed); error.value = null
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            if (cache.exists()) runCatching { setGroups(json.decodeFromString(cache.readText())) }
            error.value = "云端内容加载失败，可继续播放本地内容，点击重试。"
        }
    }
    private fun setGroups(items: List<MeditationGroup>) {
        groupDescriptions.value = items.associate { it.name.trim() to (it.description?.trim()?.takeIf(String::isNotEmpty) ?: "跟随声音，把注意力带回当下") }
        val covers = listOf(2,5,9,10,12,14,15,17,18,19,23,24,25,26,27,30)
        groups.value = items.sortedWith(compareBy<MeditationGroup> { it.sortOrder ?: 0 }.thenBy { it.id }).filter { it.name.isNotBlank() }.map { g ->
            g.name.trim() to g.items.filter { it.title.isNotBlank() && it.downloadUrl.isNotBlank() }.mapIndexed { i, t ->
                MeditationTrack("remote-${t.id}", t.title, "云端正念", covers[Math.floorMod(t.id, covers.size)], downloadUrl = t.downloadUrl, requiresVIP = !(g.name.trim() == "冥想小白课程" && i == 0))
            }
        }.filter { it.second.isNotEmpty() }
    }
}

object MindfulnessPlayer {
    private lateinit var context: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var job: Job? = null
    private var ticker: Job? = null
    private var deadline: Long? = null
    private var prepared = false
    private var focusRequest: AudioFocusRequest? = null
    private var resumeOnFocus = false
    private val _state = MutableStateFlow<MeditationPlayback?>(null)
    val state = _state.asStateFlow()
    val downloadProgress = MutableStateFlow<Float?>(null)
    private val client = OkHttpClient.Builder().callTimeout(180, TimeUnit.SECONDS).build()
    fun init(context: Context) { this.context = context.applicationContext }
    fun play(track: MeditationTrack, preserveTimer: Boolean = false) {
        if (track.requiresVIP && !MembershipAccess.active.value) return
        if (_state.value?.track?.id == track.id && prepared) {resume();return}
        val savedDeadline = if(preserveTimer) deadline else null
        stop(); AudioPlayerManager.stop(); StoryPlayerManager.stop()
        deadline = savedDeadline
        _state.value = MeditationPlayback(track)
        ContextCompat.startForegroundService(context, Intent(context, MindfulnessService::class.java))
        job = scope.launch {
            try {
                val file = resolve(track)
                ensureActive()
                val p = MediaPlayer()
                player = p
                p.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                p.setDataSource(file.absolutePath)
                p.setOnPreparedListener {
                    if (player !== it) return@setOnPreparedListener
                    prepared = true
                    _state.update { s -> s?.copy(loading = false, duration = it.duration / 1000f) }
                    resume()
                }
                p.setOnCompletionListener { _state.update { s -> s?.copy(playing = false, position = s.duration) } }
                p.setOnErrorListener { _, _, _ -> fail("音频无法播放，请重试"); true }
                p.prepareAsync()
                ticker = scope.launch {
                    while (isActive) {
                        delay(500)
                        if (prepared) _state.update { s -> s?.copy(position = player?.currentPosition?.div(1000f) ?: 0f, remainingSeconds = deadline?.let { ((it-SystemClock.elapsedRealtime())/1000).toInt().coerceAtLeast(0) }) }
                        if (deadline?.let { SystemClock.elapsedRealtime() >= it } == true) { pause(); deadline = null; _state.update { it?.copy(remainingSeconds = null) } }
                    }
                }
            } catch (e: CancellationException) { throw e } catch (e: Exception) { fail("播放失败，请检查网络后重试") }
        }
    }
    private fun fail(message: String) {
        player?.release(); player = null; prepared = false; ticker?.cancel()
        _state.update { it?.copy(loading = false, playing = false, error = message) }
    }
    fun toggle() { if (_state.value?.playing == true) pause() else resume() }
    fun pause() { if (prepared) player?.pause(); _state.update { it?.copy(playing = false) } }
    fun resume() {
        if (!prepared) return
        AudioPlayerManager.stop(); StoryPlayerManager.stop()
        val audio = context.getSystemService(AudioManager::class.java)
        if (focusRequest == null) focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener { change ->
                scope.launch {
                    if (change == AudioManager.AUDIOFOCUS_GAIN && resumeOnFocus) { resumeOnFocus = false; resume() }
                    else if (change < 0) { resumeOnFocus = change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT && _state.value?.playing == true; pause() }
                }
            }.build()
        if (audio.requestAudioFocus(focusRequest!!) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        if (_state.value!!.position >= _state.value!!.duration) player?.seekTo(0)
        player?.start(); _state.update { it?.copy(playing = true) }
    }
    fun seek(seconds: Float) { if (prepared) { val value = seconds.coerceIn(0f, _state.value!!.duration); player?.seekTo((value*1000).toInt()); _state.update { it?.copy(position = value) } } }
    fun skip(seconds: Float) { seek((_state.value?.position ?: 0f) + seconds) }
    fun timer(minutes: Int?) { deadline = minutes?.let { SystemClock.elapsedRealtime()+it*60_000L }; _state.update { it?.copy(remainingSeconds = minutes?.times(60)) } }
    fun stop() {
        job?.cancel(); job = null; ticker?.cancel(); ticker = null
        player?.release(); player = null; prepared = false; deadline = null; resumeOnFocus = false
        if (::context.isInitialized) focusRequest?.let { context.getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        _state.value = null; downloadProgress.value = null
    }
    fun cached(track: MeditationTrack): Boolean = track.asset.isNotBlank() ||
        File(context.cacheDir,"Mindfulness/${track.id}.audio").let {it.exists() && it.length()>0}
    suspend fun prepare(track: MeditationTrack) {
        check(!track.requiresVIP || MembershipAccess.active.value) { "请先开通会员" }
        resolve(track)
    }
    private suspend fun resolve(track: MeditationTrack): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "Mindfulness").apply { mkdirs() }
        val destination = File(dir, "${track.id}.audio")
        if (destination.exists() && destination.length() > 0) return@withContext destination
        val temporary = File.createTempFile(track.id, ".download", dir)
        try {
            if (track.asset.isNotEmpty()) context.assets.open(track.asset).use { input -> temporary.outputStream().use { input.copyTo(it) } }
            else {
                val url = java.net.URI("https://www.cjym123.cn/").resolve(track.downloadUrl).toString()
                require(url.startsWith("https://"))
                val call = client.newCall(Request.Builder().url(url).build())
                val completion = currentCoroutineContext().job.invokeOnCompletion { if (it is CancellationException) call.cancel() }
                try { call.execute().use { response ->
                    check(response.isSuccessful) { "HTTP ${response.code}" }
                    val body = response.body ?: error("Empty audio")
                    check(body.contentType()?.type != "text")
                    val total = body.contentLength(); var bytes = 0L
                    body.byteStream().use { input -> temporary.outputStream().use { out ->
                        val buffer = ByteArray(32768)
                        while (true) { ensureActive(); val count = input.read(buffer); if (count < 0) break; out.write(buffer,0,count); bytes += count; downloadProgress.value = if(total>0) bytes.toFloat()/total else null }
                    } }
                    check(bytes>0 && (total<0 || bytes==total))
                } } finally { completion.dispose() }
            }
            ensureActive(); check(temporary.length()>0); check(temporary.renameTo(destination)); destination
        } finally { temporary.delete(); downloadProgress.value = null }
    }
}

/** Entitlements must come from the Android billing backend; never infer them from a local switch. */
object MembershipAccess {
    private val mutableActive = MutableStateFlow(false)
    val active: StateFlow<Boolean> = mutableActive.asStateFlow()
    private val mutableStatus = MutableStateFlow<MemberStatus?>(null)
    internal val status = mutableStatus.asStateFlow()
    private var currentToken = ""
    private var generation = 0
    private var requestCounter = 0
    private var expiryDeadline: Long? = null
    private var expiryJob: kotlinx.coroutines.Job? = null
    private val entitlementScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    @Synchronized fun reset() {
        generation++; currentToken = ""; mutableActive.value = false
        mutableStatus.value = null
        expiryJob?.cancel(); expiryJob = null; expiryDeadline = null
    }
    suspend fun refresh(token: String): Boolean {
        val (requestGeneration, requestId) = synchronized(this) {
            if (token != currentToken) { reset(); currentToken = token }
            generation to ++requestCounter
        }
        if (token.isBlank()) return false
        val result = MembershipPayment.membership(token)
        synchronized(this) {
            if (generation == requestGeneration && currentToken == token && requestId == requestCounter) {
                expiryJob?.cancel()
                val expires = runCatching {
                    java.time.LocalDateTime.parse(result.expiresAt,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
                }.getOrNull()
                val effective = result.copy(active = result.active && (expires == null || expires > System.currentTimeMillis()))
                mutableActive.value = effective.active
                mutableStatus.value = effective
                expiryDeadline = expires
                if (effective.active && expires != null) expiryJob = entitlementScope.launch {
                    kotlinx.coroutines.delay((expires - System.currentTimeMillis()).coerceAtLeast(0))
                    synchronized(this@MembershipAccess) {
                        if (generation == requestGeneration && expiryDeadline == expires) {
                            mutableActive.value = false
                            mutableStatus.value = effective.copy(active = false)
                        }
                    }
                }
            }
        }
        return result.active
    }
}
