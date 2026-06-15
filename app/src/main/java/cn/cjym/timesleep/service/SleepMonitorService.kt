package cn.cjym.timesleep.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cn.cjym.timesleep.MainActivity
import cn.cjym.timesleep.R

/**
 * 睡眠监测前台服务，对应 iOS `SleepMonitorService` 在后台保持
 * `AVAudioEngine` tap 运行的能力。实际的音频采集逻辑由
 * [SleepMonitorManager] 在监测开始时启动；本服务仅负责持有
 * `FOREGROUND_SERVICE_MICROPHONE` 前台状态与通知，保证应用在后台时
 * 录音不会被系统中断。
 */
class SleepMonitorService : Service() {

    override fun onCreate() {
        super.onCreate()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "睡眠监测",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("时光睡眠")
            .setContentText("正在监测睡眠环境音")
            .setSmallIcon(R.drawable.launcher_icon)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "sleep_monitor"
        private const val NOTIFICATION_ID = 1001
    }
}
