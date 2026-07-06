/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import cn.cjym.timesleep.service.PangleAdManager
import cn.cjym.timesleep.service.UMengAnalytics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk

class SplashActivity : ComponentActivity() {
    private lateinit var splashAdContainer: FrameLayout
    private var hasNavigated = false
    private var forceGoMain = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        window.setBackgroundDrawableResource(android.R.color.white)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.also {
                it.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        setContentView(buildSplashView())

        lifecycleScope.launch {
            val accepted = (applicationContext as TimeSleepApp).settings.agreementAccepted.first()
            delay(350)
            if (!accepted) {
                navigateToMain()
                return@launch
            }
            UMengAnalytics.initialize(applicationContext)
            PangleAdManager.initialize(this@SplashActivity) { success ->
                if (success) loadSplashAd() else navigateToMain()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        if (forceGoMain) navigateToMain()
    }

    override fun onStop() {
        super.onStop()
        forceGoMain = true
    }

    private fun buildSplashView(): FrameLayout {
        return FrameLayout(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@SplashActivity, android.R.color.white))

            val iconSize = (96 * resources.displayMetrics.density).toInt()
            val icon = ImageView(this@SplashActivity).apply {
                setImageResource(R.drawable.launcher_icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            addView(
                icon,
                FrameLayout.LayoutParams(
                    iconSize,
                    iconSize,
                    Gravity.CENTER,
                ),
            )

            splashAdContainer = FrameLayout(this@SplashActivity)
            addView(
                splashAdContainer,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
    }

    private fun loadSplashAd() {
        val adNativeLoader = TTAdSdk.getAdManager().createAdNative(this)
        val (screenW, screenH) = realScreenSize()
        val adSlot = AdSlot.Builder()
            .setCodeId(AppConstants.SPLASH_SLOT_ID)
            .setImageAcceptedSize(screenW, screenH)
            .build()

        Log.d(TAG, "loadSplashAd slot=${AppConstants.SPLASH_SLOT_ID} size=${screenW}x$screenH")
        adNativeLoader.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
            override fun onSplashLoadSuccess(ad: CSJSplashAd?) = Unit

            override fun onSplashLoadFail(error: CSJAdError?) {
                Log.e(TAG, "splash load fail code=${error?.code} msg=${error?.msg}")
                runOnUiThread { navigateToMain() }
            }

            override fun onSplashRenderSuccess(ad: CSJSplashAd?) {
                runOnUiThread { showSplashAd(ad) }
            }

            override fun onSplashRenderFail(ad: CSJSplashAd?, error: CSJAdError?) {
                Log.e(TAG, "splash render fail code=${error?.code} msg=${error?.msg}")
                runOnUiThread { navigateToMain() }
            }
        }, 3500)
    }

    private fun showSplashAd(ad: CSJSplashAd?) {
        if (ad == null) {
            navigateToMain()
            return
        }
        val (screenW, screenH) = realScreenSize()
        splashAdContainer.layoutParams = FrameLayout.LayoutParams(screenW, screenH)
        ad.setSplashAdListener(object : CSJSplashAd.SplashAdListener {
            override fun onSplashAdShow(ad: CSJSplashAd?) = Unit
            override fun onSplashAdClick(ad: CSJSplashAd?) = Unit
            override fun onSplashAdClose(ad: CSJSplashAd?, closeType: Int) = navigateToMain()
        })
        ad.showSplashView(splashAdContainer)
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun realScreenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    private fun navigateToMain() {
        if (hasNavigated || isFinishing || isDestroyed) return
        hasNavigated = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "TimeSleepSplash"
    }
}
