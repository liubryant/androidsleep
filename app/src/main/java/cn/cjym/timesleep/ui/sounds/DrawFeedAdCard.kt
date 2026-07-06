/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.sounds

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cn.cjym.timesleep.AppConstants
import cn.cjym.timesleep.service.PangleAdManager
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTDrawFeedAd
import com.bytedance.sdk.openadsdk.TTNativeAd
import kotlinx.coroutines.delay

@Composable
fun DrawFeedAdCard(modifier: Modifier = Modifier) {
    if (AppConstants.isAdDisabled) return

    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var isHidden by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var drawAd by remember { mutableStateOf<TTDrawFeedAd?>(null) }
    var adView by remember { mutableStateOf<View?>(null) }
    val hideAd: (String) -> Unit = { reason ->
        Log.w(TAG, reason)
        isLoading = false
        isHidden = true
    }

    val adToDispose = drawAd
    DisposableEffect(adToDispose) {
        onDispose {
            adToDispose?.destroy()
        }
    }

    LaunchedEffect(Unit) {
        if (drawAd != null || isLoading) return@LaunchedEffect
        Log.d(TAG, "draw feed compose start codeId=${AppConstants.DRAW_FEED_SLOT_ID}")
        val activity = context.findActivity()
        if (activity == null) {
            hideAd("draw feed hidden: activity is null")
            return@LaunchedEffect
        }

        isLoading = true
        Log.d(TAG, "draw feed init sdk activity=${activity.javaClass.simpleName}")
        PangleAdManager.initialize(activity) { success ->
            if (!success) {
                hideAd("draw feed hidden: sdk init failed")
                return@initialize
            }
            if (isHidden) {
                Log.w(TAG, "draw feed skip request: card already hidden")
                return@initialize
            }

            val adSlot = AdSlot.Builder()
                .setCodeId(AppConstants.DRAW_FEED_SLOT_ID)
                .setAdCount(1)
                .setImageAcceptedSize(1080, 1920)
                .build()

            Log.d(TAG, "draw feed request start codeId=${AppConstants.DRAW_FEED_SLOT_ID}")
            TTAdSdk.getAdManager()
                .createAdNative(activity)
                .loadDrawFeedAd(adSlot, object : TTAdNative.DrawFeedAdListener {
                    override fun onError(code: Int, message: String?) {
                        mainHandler.post {
                            hideAd("draw feed hidden: load error code=$code message=$message")
                        }
                    }

                    override fun onDrawFeedAdLoad(ads: MutableList<TTDrawFeedAd>?) {
                        Log.d(TAG, "draw feed callback count=${ads?.size ?: 0}")
                        val ad = ads?.firstOrNull()
                        val view = ad?.adView
                        mainHandler.post {
                            if (ad == null || view == null) {
                                hideAd("draw feed hidden: ad or view is null")
                                return@post
                            }
                            if (isHidden) {
                                Log.w(TAG, "draw feed loaded after hidden, destroy ad")
                                ad.destroy()
                                return@post
                            }
                            Log.d(TAG, "draw feed loaded view=${view.javaClass.simpleName}")
                            drawAd = ad
                            adView = view
                            isLoading = false
                        }
                    }
                })
        }

        delay(8_000)
        if (isLoading && adView == null) {
            hideAd("draw feed hidden: load timeout")
        }
    }

    if (isHidden) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.52f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        val currentAd = drawAd
        val currentView = adView

        if (currentAd != null && currentView != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    FrameLayout(viewContext).apply {
                        attachDrawAd(currentAd, currentView)
                    }
                },
                update = { container ->
                    container.attachDrawAd(currentAd, currentView)
                },
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun FrameLayout.attachDrawAd(ad: TTDrawFeedAd, view: View) {
    if (view.parent !== this) {
        (view.parent as? ViewGroup)?.removeView(view)
        removeAllViews()
        addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        Log.d(TAG, "draw feed view attached")
    }
    ad.registerViewForInteraction(
        this,
        view,
        object : TTNativeAd.AdInteractionListener {
            override fun onAdClicked(view: View?, nativeAd: TTNativeAd?) = Unit
            override fun onAdCreativeClick(view: View?, nativeAd: TTNativeAd?) = Unit
            override fun onAdShow(nativeAd: TTNativeAd?) = Unit
        },
    )
}

private const val TAG = "TimeSleepDrawAd"
