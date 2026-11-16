/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cn.cjym.timesleep.ui.AppRoot
import cn.cjym.timesleep.ui.theme.TimeSleepTheme

open class MainActivity : ComponentActivity() {
    var pendingRoute by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set
    fun consumeRoute() { pendingRoute = null }
    private fun readRoute(intent: android.content.Intent) {
        pendingRoute = intent.getStringExtra("meditationId")?.let { "meditation/$it" }
            ?: if(intent.getBooleanExtra("openStories",false)) "stories" else null
    }
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readRoute(intent)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_TimeSleep)
        super.onCreate(savedInstanceState)
        readRoute(intent)
        enableEdgeToEdge(statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            TimeSleepTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}
