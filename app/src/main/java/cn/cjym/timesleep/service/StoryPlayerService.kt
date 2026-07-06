/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

class StoryPlayerService : Service() {
    private lateinit var mediaSession: MediaSessionCompat
    private var collectJob: Job? = null
    private val coverCache = mutableMapOf<String, Bitmap?>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        ensureChannel()

        mediaSession = MediaSessionCompat(this, "TimeSleepStorySession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    StoryPlayerManager.nowPlaying.value?.let { StoryPlayerManager.resume(it.story, "") }
                }

                override fun onPause() = StoryPlayerManager.pause()
                override fun onStop() = StoryPlayerManager.stop()
            })
            isActive = true
        }

        startForegroundCompat(buildNotification(StoryPlayerManager.nowPlaying.value))

        collectJob = scope.launch {
            StoryPlayerManager.nowPlaying.collect { nowPlaying ->
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
            ACTION_PLAY -> StoryPlayerManager.nowPlaying.value?.let { StoryPlayerManager.resume(it.story, "") }
            ACTION_PAUSE -> StoryPlayerManager.pause()
            ACTION_STOP -> StoryPlayerManager.stop()
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

    private fun updateMediaSession(nowPlaying: StoryNowPlaying) {
        val cover = coverBitmap(nowPlaying.story.coverAssetPath)
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, nowPlaying.story.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, AppConstants.APP_NAME)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "睡前故事")
            .apply {
                if (cover != null) {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ART, cover)
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cover)
                    putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, cover)
                }
            }
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
                .setState(state, nowPlaying.currentTime.toLong() * 1000L, 1f)
                .build(),
        )
    }

    private fun buildNotification(nowPlaying: StoryNowPlaying?): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val isPlaying = nowPlaying?.isPlaying == true
        val toggleAction = if (isPlaying) {
            NotificationCompat.Action(R.drawable.ic_notify_pause, "暂停", actionPendingIntent(ACTION_PAUSE))
        } else {
            NotificationCompat.Action(R.drawable.ic_notify_play, "播放", actionPendingIntent(ACTION_PLAY))
        }
        val stopAction = NotificationCompat.Action(
            R.drawable.ic_notify_stop,
            "停止",
            actionPendingIntent(ACTION_STOP),
        )
        val cover = nowPlaying?.story?.coverAssetPath?.let { coverBitmap(it) }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(nowPlaying?.story?.title ?: AppConstants.APP_NAME)
            .setContentText("睡前故事")
            .setSmallIcon(R.drawable.launcher_icon)
            .setLargeIcon(cover)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setColorized(false)
            .addAction(toggleAction)
            .addAction(stopAction)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1),
            )
            .build()
    }

    private fun coverBitmap(assetPath: String): Bitmap? {
        return coverCache.getOrPut(assetPath) {
            runCatching {
                assets.open(assetPath).use { input ->
                    BitmapFactory.decodeStream(input)?.let { scaleCover(it) }
                }
            }.getOrNull()
        }
    }

    private fun scaleCover(bitmap: Bitmap): Bitmap {
        val targetSize = 512
        if (bitmap.width == targetSize && bitmap.height == targetSize) return bitmap
        val scale = maxOf(targetSize.toFloat() / bitmap.width, targetSize.toFloat() / bitmap.height)
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        val left = ((scaledWidth - targetSize) / 2).coerceAtLeast(0)
        val top = ((scaledHeight - targetSize) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(scaled, left, top, targetSize, targetSize)
    }

    private fun actionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, StoryPlayerService::class.java).setAction(action)
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "睡前故事播放", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "story_player"
        private const val NOTIFICATION_ID = 2002
        private const val ACTION_PLAY = "cn.cjym.timesleep.action.STORY_PLAY"
        private const val ACTION_PAUSE = "cn.cjym.timesleep.action.STORY_PAUSE"
        private const val ACTION_STOP = "cn.cjym.timesleep.action.STORY_STOP"
    }
}
