package cn.cjym.timesleep.service

import cn.cjym.timesleep.AppConstants

/**
 * 开屏广告占位实现，对应 iOS `PangleSplashAdManager`。
 * `AppConstants.isAdDisabled` 默认为 true，不会真正加载/展示开屏广告。
 */
object PangleSplashAdManager {
    private var didRequestSplashThisSession = false

    fun resetSplashRequestState() {
        didRequestSplashThisSession = false
    }

    fun loadAndShowDefaultSplashAd(onComplete: (shown: Boolean, error: String?) -> Unit = { _, _ -> }) {
        loadAndShowSplashAd(slotId = AppConstants.SPLASH_SLOT_ID, onComplete = onComplete)
    }

    fun loadAndShowSplashAd(slotId: String, onComplete: (shown: Boolean, error: String?) -> Unit = { _, _ -> }) {
        if (AppConstants.isAdDisabled || didRequestSplashThisSession) {
            didRequestSplashThisSession = true
            onComplete(false, null)
            return
        }

        if (!PangleAdManager.isSDKInitialized()) {
            PangleAdManager.initialize { success ->
                if (success) {
                    loadAndShowSplashAd(slotId, onComplete)
                } else {
                    didRequestSplashThisSession = true
                    onComplete(false, "GroMore SDK 初始化失败")
                }
            }
            return
        }

        // 接入穿山甲 / GroMore SDK 后在此处加载并展示开屏广告。
        didRequestSplashThisSession = true
        onComplete(false, null)
    }
}
