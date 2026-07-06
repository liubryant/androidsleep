/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.service

import android.content.Context

/**
 * SDK 启动编排，对应 iOS `AppSDKManager`。
 * 在用户同意隐私协议后调用一次，依次完成统计与广告 SDK 的初始化，
 * 并可选地尝试展示开屏广告。
 */
object AppSDKManager {
    private var didStartSDKFlow = false

    fun startIfAllowed(context: Context, agreementAccepted: Boolean, showSplash: Boolean = true, onComplete: () -> Unit = {}) {
        if (!agreementAccepted || didStartSDKFlow) {
            onComplete()
            return
        }
        didStartSDKFlow = true

        UMengAnalytics.initialize(context)

        PangleAdManager.initialize(context) { onComplete() }
    }
}
