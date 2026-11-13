package cn.cjym.timesleep.service

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cn.cjym.timesleep.MainActivity
import cn.cjym.timesleep.R
import java.time.ZonedDateTime

object StoryReminder {
    fun enabled(context: Context) = context.getSharedPreferences("story_reminder",0).getBoolean("enabled",false)
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("story_reminder",0).edit().putBoolean("enabled",enabled).apply()
        val alarm = context.getSystemService(AlarmManager::class.java)
        for(hour in listOf(21,22)) {
            val pending = PendingIntent.getBroadcast(context,hour,Intent(context,StoryReminderReceiver::class.java).setAction("story-reminder-$hour"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarm.cancel(pending)
            if(enabled) {
                val now=ZonedDateTime.now()
                var next=now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
                if(!next.isAfter(now)) next=next.plusDays(1)
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.toInstant().toEpochMilli(),pending)
            }
        }
    }
}

class StoryReminderReceiver: BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        if(!StoryReminder.enabled(context)) return
        if(intent.action?.startsWith("story-reminder-")==true && NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("story_reminder","睡前故事提醒",NotificationManager.IMPORTANCE_DEFAULT))
            val open=PendingIntent.getActivity(context,2100,Intent(context,MainActivity::class.java).putExtra("openStories",true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification=NotificationCompat.Builder(context,"story_reminder").setSmallIcon(R.drawable.ic_notify_play).setContentTitle("留一点时间，听一个睡前故事").setContentText("今晚的故事已为你准备好，放松下来，慢慢入眠。").setContentIntent(open).setAutoCancel(true).build()
            context.getSystemService(NotificationManager::class.java).notify(2100,notification)
        }
        StoryReminder.setEnabled(context,true)
    }
}
