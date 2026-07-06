/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.service

/**
 * 开屏广告由 [cn.cjym.timesleep.SplashActivity] 持有 Activity 与广告容器后加载展示。
 * 该对象仅保留会话状态重置入口，避免旧调用重复请求开屏。
 */
object PangleSplashAdManager {
    private var didRequestSplashThisSession = false

    fun resetSplashRequestState() {
        didRequestSplashThisSession = false
    }

    fun loadAndShowDefaultSplashAd(onComplete: (shown: Boolean, error: String?) -> Unit = { _, _ -> }) {
        didRequestSplashThisSession = true
        onComplete(false, null)
    }

    fun loadAndShowSplashAd(slotId: String, onComplete: (shown: Boolean, error: String?) -> Unit = { _, _ -> }) {
        didRequestSplashThisSession = true
        onComplete(false, null)
    }
}
