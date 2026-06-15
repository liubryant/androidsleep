package cn.cjym.timesleep.service

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import cn.cjym.timesleep.data.model.SleepSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Health Connect 接入，对应 iOS `HealthKitService`：授权状态管理与
 * 睡眠数据写入。
 */
object HealthConnectManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _statusText = MutableStateFlow("未授权")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
    )

    /** Compose 中通过 `rememberLauncherForActivityResult` 注册的授权请求 contract。 */
    fun requestPermissionsContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    fun permissionsToRequest(): Set<String> = permissions

    /** 检测 Health Connect 是否可用，并刷新当前授权状态。 */
    fun refreshAuthorization(context: Context) {
        val available = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        _isAvailable.value = available
        if (!available) {
            _isAuthorized.value = false
            _statusText.value = "当前设备不可用"
            return
        }

        scope.launch {
            val granted = HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
            applyGranted(granted)
        }
    }

    /** 授权请求结果回调。 */
    fun onPermissionResult(granted: Set<String>) {
        applyGranted(granted)
    }

    private fun applyGranted(granted: Set<String>) {
        val authorized = granted.containsAll(permissions)
        _isAuthorized.value = authorized
        _statusText.value = if (authorized) "已授权" else "未授权"
    }

    /** 将一次完整的睡眠记录写入 Health Connect。 */
    suspend fun save(context: Context, session: SleepSession) {
        if (!_isAuthorized.value) return
        val endMillis = session.endTime ?: return
        if (endMillis <= session.startTime) return

        val start = Instant.ofEpochMilli(session.startTime)
        val end = Instant.ofEpochMilli(endMillis)
        val zone = ZoneId.systemDefault()

        val record = SleepSessionRecord(
            startTime = start,
            startZoneOffset = zone.rules.getOffset(start),
            endTime = end,
            endZoneOffset = zone.rules.getOffset(end),
        )

        runCatching {
            HealthConnectClient.getOrCreate(context).insertRecords(listOf(record))
        }
    }
}
