/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.profile

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import cn.cjym.timesleep.LegalLinks

/** 用户协议 / 隐私政策页面，对应 iOS `LegalTextView`（内嵌 WebView）。 */
@Composable
fun LegalWebViewScreen(type: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val title = if (type == "privacy") "隐私政策" else "用户协议"
    val url = if (type == "privacy") LegalLinks.PRIVACY_POLICY_URL else LegalLinks.USER_AGREEMENT_URL

    Scaffold(
        topBar = { ProfileTopBar(title = title, onBack = onBack) },
        modifier = modifier,
    ) { innerPadding ->
        LegalWebView(url = url, modifier = Modifier.fillMaxSize().padding(innerPadding))
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LegalWebView(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                loadUrl(url)
            }
        },
    )
}
