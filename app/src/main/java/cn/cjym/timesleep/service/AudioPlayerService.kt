package cn.cjym.timesleep.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import cn.cjym.timesleep.AppConstants
import cn.cjym.timesleep.MainActivity
import cn.cjym.timesleep.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 助眠音效播放前台服务，对应 iOS `AudioPlayerService` 的
 * `MPNowPlayingInfoCenter` / `MPRemoteCommandCenter`：播放期间在
 * 通知栏与锁屏展示当前播放场景及播放/暂停状态与控制按钮。
 */
class AudioPlayerService : Service() {

    private lateinit var mediaSession: MediaSessionCompat
    private var collectJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        ensureChannel()

        mediaSession = MediaSessionCompat(this, "TimeSleepAudioSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = AudioPlayerManager.resume()
                override fun onPause() = AudioPlayerManager.pause()
                override fun onStop() = AudioPlayerManager.stop()
            })
            isActive = true
        }

        startForegroundCompat(buildNotification(AudioPlayerManager.nowPlaying.value))

        collectJob = scope.launch {
            AudioPlayerManager.nowPlaying.collect { nowPlaying ->
                if (nowPlaying == null) {
                    stopSelf()
                    return@collect
                }
                updateMediaSession(nowPlaying)
                postNotification(buildNotification(nowPlaying))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> AudioPlayerManager.resume()
            ACTION_PAUSE -> AudioPlayerManager.pause()
            ACTION_STOP -> AudioPlayerManager.stop()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        collectJob?.cancel()
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(notification: Notification) {
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun updateMediaSession(nowPlaying: NowPlaying) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, nowPlaying.scene.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, AppConstants.APP_NAME)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, nowPlaying.scene.subtitle)
            .build()
        mediaSession.setMetadata(metadata)

        val state = if (nowPlaying.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP,
                )
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build(),
        )
    }

    private fun buildNotification(nowPlaying: NowPlaying?): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )

        val isPlaying = nowPlaying?.isPlaying == true
        val toggleAction = if (isPlaying) {
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "暂停", actionPendingIntent(ACTION_PAUSE))
        } else {
            NotificationCompat.Action(android.R.drawable.ic_media_play, "播放", actionPendingIntent(ACTION_PLAY))
        }
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel, "停止", actionPendingIntent(ACTION_STOP),
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(nowPlaying?.scene?.title ?: AppConstants.APP_NAME)
            .setContentText(nowPlaying?.scene?.subtitle ?: "正在准备播放")
            .setSmallIcon(R.drawable.launcher_icon)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .addAction(toggleAction)
            .addAction(stopAction)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1),
            )
            .build()
    }

    private fun actionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, AudioPlayerService::class.java).setAction(action)
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "助眠音效播放", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "audio_player"
        private const val NOTIFICATION_ID = 2001
        private const val ACTION_PLAY = "cn.cjym.timesleep.action.PLAY"
        private const val ACTION_PAUSE = "cn.cjym.timesleep.action.PAUSE"
        private const val ACTION_STOP = "cn.cjym.timesleep.action.STOP"
    }
}
