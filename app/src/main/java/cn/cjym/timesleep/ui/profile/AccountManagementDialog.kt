package cn.cjym.timesleep.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Matches iOS SleepAccountManagementDialog, including the light card in dark mode. */
@Composable
internal fun AccountManagementDialog(phoneNumber: String, onDismiss: () -> Unit, onLogout: () -> Unit) {
    var action by remember { mutableStateOf<String?>(null) }
    val ink = Color(0xFF111827)
    val orange = Color(0xFFC2400C)
    val shape = RoundedCornerShape(12.dp)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0.18f) }
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // A full-window transparent dialog provides the same outside-tap dismissal.
            Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
            val compact = maxWidth < 560.dp
            val cardWidth = if (compact) maxWidth - 20.dp else minOf(maxWidth - 48.dp, 520.dp)
            val cardHeight = if (compact) minOf(maxOf(maxHeight * .62f, 440.dp), maxHeight - 20.dp)
                else minOf(maxOf(maxHeight * .62f, 420.dp), 500.dp)
            Column(
                Modifier.width(cardWidth).height(cardHeight)
                    .shadow(18.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                    .background(Color.White).clickable(enabled = false) {}
                    .padding(28.dp)
                    .then(if (cardHeight < 440.dp) Modifier.verticalScroll(rememberScrollState()).height(384.dp) else Modifier),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("账号管理", fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, color = ink)
                        Text("管理当前登录账号", fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFF6B7380))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, "关闭", Modifier.size(18.dp), tint = Color.Black)
                    }
                }
                Spacer(Modifier.height(26.dp))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 82.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF7F5FF)).border(1.dp, Color(0xFFE8E3FF), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Brush.linearGradient(
                        listOf(Color(0xFF6650F5), Color(0xFF9178FF)))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, Modifier.size(25.dp), tint = Color.White)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("已登录", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF16A16B))
                        Text(if (phoneNumber.length >= 7) phoneNumber.take(3) + " **** " + phoneNumber.takeLast(4) else phoneNumber,
                            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    }
                    Icon(Icons.Default.Verified, "账号已登录", Modifier.size(20.dp), tint = Color(0xFF16A16B))
                }
                Spacer(Modifier.weight(1f).heightIn(min = 28.dp))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(shape)
                        .background(Color(0xFFFFF7F2)).border(1.dp, orange.copy(alpha = .45f), shape)
                        .clickable(role = Role.Button) { action = "delete" }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Default.PersonRemove, null, Modifier.size(20.dp), tint = orange)
                    Text("注销账号", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = orange)
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = orange)
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(shape)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF171A24), Color(0xFF364059))))
                        .clickable(role = Role.Button) { action = "password" }.padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("设置或修改密码", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(shape)
                        .background(Color(0xFFF0EBE3)).border(1.dp, orange, shape)
                        .clickable(role = Role.Button, onClick = onLogout).padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("退出登录", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = orange) }
            }
        }
    }
    action?.let { page ->
        Dialog(onDismissRequest = { action = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BackHandler { action = null }
            Box(Modifier.fillMaxSize()) {
                if (page == "password") SetPasswordScreen(onBack = { action = null })
                else AccountDeletionScreen(onBack = { action = null }, onDeleted = { action = null; onDismiss() })
            }
        }
    }
}
