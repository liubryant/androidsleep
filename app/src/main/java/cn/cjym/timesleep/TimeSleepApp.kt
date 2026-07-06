/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep

import android.app.Application
import cn.cjym.timesleep.data.settings.AppSettings
import cn.cjym.timesleep.data.sound.SoundLibrary
import cn.cjym.timesleep.data.story.StoryLibrary
import cn.cjym.timesleep.service.AudioPlayerManager
import cn.cjym.timesleep.service.SleepMonitorManager
import cn.cjym.timesleep.service.StoryPlayerManager
import cn.cjym.timesleep.service.UMengAnalytics

class TimeSleepApp : Application() {
    val settings: AppSettings by lazy { AppSettings(this) }
    val soundLibrary: SoundLibrary by lazy { SoundLibrary(this) }
    val storyLibrary: StoryLibrary by lazy { StoryLibrary(this) }

    override fun onCreate() {
        super.onCreate()
        AudioPlayerManager.init(this)
        StoryPlayerManager.init(this)
        SleepMonitorManager.init(this)
        // 不采集个人信息，合规要求可在用户同意隐私协议前调用。
        UMengAnalytics.preInit(this)
    }
}
