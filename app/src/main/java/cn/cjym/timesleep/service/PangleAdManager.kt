package cn.cjym.timesleep.service

import cn.cjym.timesleep.AppConstants

/**
 * 穿山甲 / GroMore 广告 SDK 占位实现，对应 iOS `PangleAdManager`。
 * `AppConstants.isAdDisabled` 默认为 true，此时不会真正初始化广告 SDK。
 */
object PangleAdManager {
    private var initialized = false

    fun initialize(onComplete: (Boolean) -> Unit = {}) {
        if (AppConstants.isAdDisabled) {
            onComplete(false)
            return
        }

        // 接入穿山甲 / GroMore SDK 后在此处调用其初始化方法。
        initialized = true
        onComplete(true)
    }

    fun isSDKInitialized(): Boolean = initialized
}
