package cn.cjym.timesleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.ui.theme.TimeSleepTheme

class HealthPermissionActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {TimeSleepTheme {Surface(Modifier.fillMaxSize()){Column(Modifier.statusBarsPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Text("健康数据使用说明",style=MaterialTheme.typography.headlineSmall)
            Text("时光睡眠仅在你授权后读取健康连接中的睡眠记录，以展示时长和阶段。数据只在本机处理，不上传服务器，不用于广告。你可以随时在健康连接中撤销授权。")
            TextButton(onClick={startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(LegalLinks.PRIVACY_POLICY_URL)))}){Text("查看隐私政策")}
            Button(onClick={finish()}){Text("返回")}
        }}}}
    }
}
