package cn.cjym.timesleep.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import cn.cjym.timesleep.MainActivity
import cn.cjym.timesleep.R
import kotlinx.coroutines.*

class MindfulnessService : Service() {
    private val scope = CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private lateinit var session: MediaSessionCompat
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("meditation", "正念冥想", NotificationManager.IMPORTANCE_LOW))
        session = MediaSessionCompat(this,"Mindfulness").apply {
            setCallback(object: MediaSessionCompat.Callback() {
                override fun onPlay() = MindfulnessPlayer.resume()
                override fun onPause() = MindfulnessPlayer.pause()
                override fun onStop() = MindfulnessPlayer.stop()
                override fun onSeekTo(pos: Long) = MindfulnessPlayer.seek(pos/1000f)
                override fun onFastForward() = MindfulnessPlayer.skip(15f)
                override fun onRewind() = MindfulnessPlayer.skip(-15f)
            }); isActive = true
        }
        val initial = notification(MindfulnessPlayer.state.value)
        if (Build.VERSION.SDK_INT >= 29) startForeground(3103,initial,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(3103,initial)
        scope.launch { MindfulnessPlayer.state.collect { s ->
            if(s==null) { stopSelf(); return@collect }
            session.setMetadata(MediaMetadataCompat.Builder().putString(MediaMetadataCompat.METADATA_KEY_TITLE,s.track.title).putLong(MediaMetadataCompat.METADATA_KEY_DURATION,(s.duration*1000).toLong()).build())
            session.setPlaybackState(PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_FAST_FORWARD or PlaybackStateCompat.ACTION_REWIND).setState(if(s.playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,(s.position*1000).toLong(),1f).build())
            getSystemService(NotificationManager::class.java).notify(3103,notification(s))
        } }
    }
    private fun notification(s: MeditationPlayback?): Notification {
        val open = PendingIntent.getActivity(this,3103,Intent(this,MainActivity::class.java).putExtra("meditationId",s?.track?.id).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun action(name: String) = PendingIntent.getService(this,name.hashCode(),Intent(this,MindfulnessService::class.java).setAction(name),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this,"meditation").setSmallIcon(R.drawable.ic_notify_play).setContentTitle(s?.track?.title ?: "正念冥想").setContentText(if(s?.loading==true) "正在准备音频…" else s?.track?.subtitle).setContentIntent(open).setOnlyAlertOnce(true).setOngoing(s?.playing==true)
            .addAction(if(s?.playing==true) R.drawable.ic_notify_pause else R.drawable.ic_notify_play,if(s?.playing==true) "暂停" else "播放",action("toggle"))
            .addAction(R.drawable.ic_notify_stop,"停止",action("stop"))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0,1)).build()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { when(intent?.action) { "toggle" -> MindfulnessPlayer.toggle(); "stop" -> MindfulnessPlayer.stop() }; return START_NOT_STICKY }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); session.release(); super.onDestroy() }
}
