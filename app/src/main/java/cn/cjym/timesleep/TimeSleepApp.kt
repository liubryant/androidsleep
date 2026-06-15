package cn.cjym.timesleep

import android.app.Application
import cn.cjym.timesleep.data.settings.AppSettings
import cn.cjym.timesleep.data.sound.SoundLibrary
import cn.cjym.timesleep.service.AudioPlayerManager
import cn.cjym.timesleep.service.SleepMonitorManager

class TimeSleepApp : Application() {
    val settings: AppSettings by lazy { AppSettings(this) }
    val soundLibrary: SoundLibrary by lazy { SoundLibrary(this) }

    override fun onCreate() {
        super.onCreate()
        AudioPlayerManager.init(this)
        SleepMonitorManager.init(this)
    }
}
