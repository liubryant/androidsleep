/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import cn.cjym.timesleep.AppConstants
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTCustomController
import com.bytedance.sdk.openadsdk.mediation.init.MediationPrivacyConfig

/**
 * 穿山甲 / GroMore 广告 SDK 初始化。合规要求：只能在用户同意隐私协议后调用。
 */
object PangleAdManager {
    private var initialized = false
    private var initializing = false
    private val pendingCallbacks = mutableListOf<(Boolean) -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(context: Context, onComplete: (Boolean) -> Unit = {}) {
        if (AppConstants.isAdDisabled) {
            onComplete(false)
            return
        }
        if (initialized) {
            onComplete(true)
            return
        }

        pendingCallbacks += onComplete
        if (initializing) return
        initializing = true

        Log.d(TAG, "GroMore init start appId=${AppConstants.PANGLE_APP_ID}")
        TTAdSdk.init(context.applicationContext, buildConfig())
        TTAdSdk.start(object : TTAdSdk.Callback {
            override fun success() {
                initialized = true
                initializing = false
                Log.d(TAG, "GroMore init success")
                finishCallbacks(true)
            }

            override fun fail(code: Int, msg: String?) {
                initializing = false
                Log.e(TAG, "GroMore init fail code=$code msg=$msg")
                finishCallbacks(false)
            }
        })
    }

    fun isSDKInitialized(): Boolean = initialized

    private fun buildConfig(): TTAdConfig {
        return TTAdConfig.Builder()
            .appId(AppConstants.PANGLE_APP_ID)
            .appName(AppConstants.APP_NAME)
            .useMediation(true)
            .debug(false)
            .themeStatus(0)
            .supportMultiProcess(false)
            .customController(buildPrivacyController())
            .build()
    }

    private fun buildPrivacyController(): TTCustomController {
        return object : TTCustomController() {
            override fun isCanUseLocation(): Boolean = true
            override fun isCanUsePhoneState(): Boolean = true
            override fun isCanUseWifiState(): Boolean = true
            override fun isCanUseWriteExternal(): Boolean = true
            override fun isCanUseAndroidId(): Boolean = true

            override fun getMediationPrivacyConfig(): MediationPrivacyConfig {
                return object : MediationPrivacyConfig() {
                    override fun isLimitPersonalAds(): Boolean = false
                    override fun isProgrammaticRecommend(): Boolean = true
                }
            }
        }
    }

    private fun finishCallbacks(success: Boolean) {
        val callbacks = pendingCallbacks.toList()
        pendingCallbacks.clear()
        mainHandler.post {
            callbacks.forEach { it(success) }
        }
    }

    private const val TAG = "TimeSleepAd"
}
