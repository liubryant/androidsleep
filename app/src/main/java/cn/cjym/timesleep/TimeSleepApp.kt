/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep

import android.app.Application
import cn.cjym.timesleep.data.settings.AppSettings
import cn.cjym.timesleep.data.sound.SoundLibrary
import cn.cjym.timesleep.data.story.StoryLibrary
import cn.cjym.timesleep.service.AudioPlayerManager
import cn.cjym.timesleep.service.StoryPlayerManager

class TimeSleepApp : Application() {
    val wellness by lazy { cn.cjym.timesleep.data.wellness.WellnessRepository(this) }
    val settings: AppSettings by lazy { AppSettings(this) }
    val soundLibrary: SoundLibrary by lazy { SoundLibrary(this) }
    val storyLibrary: StoryLibrary by lazy { StoryLibrary(this) }

    override fun onCreate() {
        super.onCreate()
        cn.cjym.timesleep.service.MindfulnessPlayer.init(this)
        AudioPlayerManager.init(this)
        StoryPlayerManager.init(this)
    }
}
