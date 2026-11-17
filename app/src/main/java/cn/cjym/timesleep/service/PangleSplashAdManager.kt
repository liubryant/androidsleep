package cn.cjym.timesleep.service

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import cn.cjym.timesleep.AppConstants
import cn.cjym.timesleep.SplashActivity
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk

/** Created only after AppRoot has read the persisted privacy agreement. */
object PangleSplashAdManager {
    private var attemptedThisProcess = false
    private const val TAG = "TimeSleepSplash"

    class Attempt(private val activity: SplashActivity, onFinish: () -> Unit) {
        private val main = Handler(Looper.getMainLooper())
        private val timeout = Runnable { finish("timeout") }
        private var container: FrameLayout? = null
        private var completed: (() -> Unit)? = onFinish
        private var rendered = false

        fun start(view: FrameLayout) {
            if (completed == null) return
            if (attemptedThisProcess || AppConstants.isAdDisabled || !canShow()) {
                finish("skipped")
                return
            }
            attemptedThisProcess = true
            container = view
            main.postDelayed(timeout, 3500)
            try {
                PangleAdManager.initialize(activity) { ready ->
                    dispatch {
                        if (!ready || !canShow()) finish("SDK unavailable")
                        else load(view)
                    }
                }
            } catch (exception: Exception) {
                finish("SDK initialization failed: ${exception.javaClass.simpleName}")
            }
        }

        private fun load(view: FrameLayout) {
            val metrics = view.resources.displayMetrics
            val slot = AdSlot.Builder()
                .setCodeId(AppConstants.SPLASH_SLOT_ID)
                .setImageAcceptedSize(
                    view.width.takeIf { it > 0 } ?: metrics.widthPixels,
                    view.height.takeIf { it > 0 } ?: metrics.heightPixels,
                )
                .build()
            TTAdSdk.getAdManager().createAdNative(activity).loadSplashAd(
                slot,
                object : TTAdNative.CSJSplashAdListener {
                    override fun onSplashLoadSuccess(ad: CSJSplashAd?) = Unit
                    override fun onSplashLoadFail(error: CSJAdError?) =
                        dispatch { finish("load failed: ${error?.code}") }
                    override fun onSplashRenderFail(ad: CSJSplashAd?, error: CSJAdError?) =
                        dispatch { finish("render failed: ${error?.code}") }
                    override fun onSplashRenderSuccess(ad: CSJSplashAd?) = dispatch {
                        if (rendered) return@dispatch
                        if (ad == null || !canShow() || !view.isAttachedToWindow) {
                            finish("ad unavailable")
                            return@dispatch
                        }
                        rendered = true
                        main.removeCallbacks(timeout)
                        main.postDelayed(timeout, 8000)
                        ad.setSplashAdListener(object : CSJSplashAd.SplashAdListener {
                            override fun onSplashAdShow(ad: CSJSplashAd?) {
                                Log.d(TAG, "Splash shown")
                            }
                            override fun onSplashAdClick(ad: CSJSplashAd?) = Unit
                            override fun onSplashAdClose(ad: CSJSplashAd?, closeType: Int) =
                                dispatch { finish("closed") }
                        })
                        ad.showSplashView(view)
                    }
                },
                3000,
            )
        }

        private fun canShow() = !activity.isFinishing && !activity.isDestroyed &&
            activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

        private fun dispatch(block: () -> Unit) {
            main.post {
                if (completed != null) {
                    try {
                        block()
                    } catch (exception: Exception) {
                        finish("ad error: ${exception.javaClass.simpleName}")
                    }
                }
            }
        }

        private fun finish(reason: String) {
            val callback = completed ?: return
            dispose()
            Log.d(TAG, reason)
            callback()
        }

        fun dispose() {
            completed = null
            main.removeCallbacks(timeout)
            container?.removeAllViews()
            container = null
        }
    }
}
