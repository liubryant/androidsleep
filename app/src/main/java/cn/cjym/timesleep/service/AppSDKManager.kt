/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.service

import android.content.Context

/**
 * SDK 启动编排，对应 iOS `AppSDKManager`。
 * 在用户同意隐私协议后完成统计与广告 SDK 的初始化。
 */
object AppSDKManager {
    private var didStartSDKFlow = false

    fun startIfAllowed(context: Context, agreementAccepted: Boolean, onComplete: () -> Unit = {}) {
        if (!agreementAccepted || didStartSDKFlow) {
            onComplete()
            return
        }
        didStartSDKFlow = true

        UMengAnalytics.initialize(context)

        PangleAdManager.initialize(context) { onComplete() }
    }
}
