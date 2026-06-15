package cn.cjym.timesleep.service

/**
 * SDK 启动编排，对应 iOS `AppSDKManager`。
 * 在用户同意隐私协议后调用一次，依次完成统计与广告 SDK 的占位初始化，
 * 并可选地尝试展示开屏广告。
 */
object AppSDKManager {
    private var didStartSDKFlow = false

    fun startIfAllowed(agreementAccepted: Boolean, showSplash: Boolean = true, onComplete: () -> Unit = {}) {
        if (!agreementAccepted || didStartSDKFlow) {
            onComplete()
            return
        }
        didStartSDKFlow = true

        UMengAnalytics.initialize()

        PangleAdManager.initialize { success ->
            if (showSplash && success) {
                PangleSplashAdManager.loadAndShowDefaultSplashAd { _, _ -> onComplete() }
            } else {
                onComplete()
            }
        }
    }
}
