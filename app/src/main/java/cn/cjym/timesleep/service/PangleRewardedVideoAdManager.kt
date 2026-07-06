/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.service

import cn.cjym.timesleep.AppConstants

/**
 * 激励视频广告占位实现，对应 iOS `PangleRewardedVideoAdManager`。
 * 用于"观看激励视频后可播放睡眠录音"的流程；`AppConstants.isAdDisabled`
 * 默认为 true 时直接放行。
 */
object PangleRewardedVideoAdManager {
    fun showForRecordingAccess(context: android.content.Context, onResult: (granted: Boolean) -> Unit) {
        if (AppConstants.isAdDisabled) {
            onResult(true)
            return
        }

        if (!PangleAdManager.isSDKInitialized()) {
            PangleAdManager.initialize(context) { success ->
                if (success) {
                    showForRecordingAccess(context, onResult)
                } else {
                    onResult(false)
                }
            }
            return
        }

        // 接入穿山甲 / GroMore SDK 后在此处加载并展示激励视频广告。
        onResult(false)
    }
}
