/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.service

import android.content.Context
import com.umeng.commonsdk.UMConfigure
import com.umeng.analytics.MobclickAgent
import cn.cjym.timesleep.AppConstants
import cn.cjym.timesleep.BuildConfig

/**
 * 友盟统计 Android 实现，对应 iOS `UMengAnalytics`。
 * 只能在用户同意《用户协议》《隐私政策》后调用 [initialize]。
 */
object UMengAnalytics {
    private var initialized = false
    private var appContext: Context? = null

    /**
     * 不采集任何个人信息，可在用户同意隐私协议前调用，在 [TimeSleepApp.onCreate] 里无条件执行。
     */
    fun preInit(context: Context) {
        UMConfigure.setLogEnabled(BuildConfig.DEBUG)
        UMConfigure.preInit(context.applicationContext, AppConstants.UMENG_APP_KEY, AppConstants.UMENG_CHANNEL)
    }

    /** 只能在用户同意《用户协议》《隐私政策》后调用，会触发设备信息采集。 */
    fun initialize(context: Context) {
        if (initialized) return
        val application = context.applicationContext
        appContext = application

        UMConfigure.submitPolicyGrantResult(application, true)
        UMConfigure.init(
            application,
            AppConstants.UMENG_APP_KEY,
            AppConstants.UMENG_CHANNEL,
            UMConfigure.DEVICE_TYPE_PHONE,
            null,
        )
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.MANUAL)
        initialized = true
    }

    fun logEvent(eventId: String, attributes: Map<String, String>? = null) {
        val context = appContext ?: return
        if (!initialized) return
        if (attributes != null) {
            MobclickAgent.onEvent(context, eventId, attributes)
        } else {
            MobclickAgent.onEvent(context, eventId)
        }
    }

    fun pageBegin(pageName: String) {
        if (!initialized) return
        MobclickAgent.onPageStart(pageName)
    }

    fun pageEnd(pageName: String) {
        if (!initialized) return
        MobclickAgent.onPageEnd(pageName)
    }
}
