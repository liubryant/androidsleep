package cn.cjym.timesleep.service

import cn.cjym.timesleep.AppConstants

/**
 * 友盟统计占位实现，对应 iOS `UMengAnalytics`。
 * 接入友盟 Android SDK 后在 [initialize] 中调用其 `UMConfigure.init`。
 */
object UMengAnalytics {
    private var initialized = false

    fun initialize() {
        if (initialized) return

        // 接入友盟 SDK 后：UMConfigure.init(context, AppConstants.UMENG_APP_KEY, AppConstants.UMENG_CHANNEL, UMConfigure.DEVICE_TYPE_PHONE, null)
        initialized = true
    }

    fun logEvent(eventId: String, attributes: Map<String, String>? = null) {
        if (!initialized) return
        // 接入友盟 SDK 后：MobclickAgent.onEvent(context, eventId, attributes)
    }

    fun pageBegin(pageName: String) {
        if (!initialized) return
        // 接入友盟 SDK 后：MobclickAgent.onPageStart(pageName)
    }

    fun pageEnd(pageName: String) {
        if (!initialized) return
        // 接入友盟 SDK 后：MobclickAgent.onPageEnd(pageName)
    }
}
