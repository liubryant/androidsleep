package cn.cjym.timesleep.service

import android.os.Build
import cn.cjym.timesleep.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** 登录相关接口异常，对应 iOS `AuthError`。 */
sealed class AuthException(message: String) : Exception(message) {
    class Server(message: String) : AuthException(message)
    object Network : AuthException("网络异常，请稍后重试")
}

@Serializable
data class AppUserLoginResult(val phone: String, val newUser: Boolean)

@Serializable
private class EmptyData

@Serializable
private data class ApiResponse<T>(val code: Int, val msg: String, val data: T? = null)

@Serializable
private data class SmsCodeData(val expiresIn: Int = 300)

/**
 * 手机号验证码 / 密码登录相关接口，对应 iOS `AuthAPI`，复用同一后端
 * `https://www.cjym123.cn`。
 */
object AuthRepository {
    private val client by lazy { OkHttpClient() }
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json".toMediaType()

    /** 请求后台发送短信验证码，返回验证码有效期（秒）。 */
    suspend fun requestVerificationCode(phone: String): Int {
        val response = post<SmsCodeData>("im/bot/login-code", mapOf("phone" to phone))
        if (response.code != 0) throw AuthException.Server(response.msg)
        return response.data?.expiresIn ?: 300
    }

    /** 手机号 + 短信验证码登录，账号不存在时后台会自动注册。 */
    suspend fun loginByCode(phone: String, code: String): AppUserLoginResult {
        val response = post<AppUserLoginResult>("im/bot/login-by-code", mapOf("phone" to phone, "code" to code) + deviceInfo)
        if (response.code != 0 || response.data == null) throw AuthException.Server(response.msg)
        return response.data
    }

    /** 手机号 + 密码登录，账号不存在时后台会用该密码自动注册。 */
    suspend fun loginByPassword(phone: String, password: String): AppUserLoginResult {
        val response = post<AppUserLoginResult>("im/bot/login-by-password", mapOf("phone" to phone, "password" to password) + deviceInfo)
        if (response.code != 0 || response.data == null) throw AuthException.Server(response.msg)
        return response.data
    }

    /** 通过短信验证码设置/重置登录密码，账号不存在时会自动注册。 */
    suspend fun setPassword(phone: String, code: String, password: String): AppUserLoginResult {
        val response = post<AppUserLoginResult>("im/bot/set-password", mapOf("phone" to phone, "code" to code, "password" to password))
        if (response.code != 0 || response.data == null) throw AuthException.Server(response.msg)
        return response.data
    }

    /** 验证短信验证码后注销账号，后台将永久删除账号及关联数据。 */
    suspend fun deleteAccount(phone: String, code: String) {
        val response = post<EmptyData>("im/bot/remove_account", mapOf("phone" to phone, "code" to code))
        if (response.code != 0) throw AuthException.Server(response.msg)
    }

    /** 登录时上报的设备信息：手机型号、Android 版本、App 名称。 */
    private val deviceInfo: Map<String, String>
        get() = mapOf(
            "deviceModel" to Build.MODEL,
            "osVersion" to Build.VERSION.RELEASE,
            "appName" to AppConstants.APP_NAME,
        )

    private suspend inline fun <reified T> post(path: String, body: Map<String, String>): ApiResponse<T> = withContext(Dispatchers.IO) {
        val requestBody = json.encodeToString(body).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${AppConstants.AUTH_BASE_URL}/$path")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string()
                if (!response.isSuccessful || text == null) throw AuthException.Network
                json.decodeFromString<ApiResponse<T>>(text)
            }
        } catch (e: AuthException) {
            throw e
        } catch (e: Exception) {
            throw AuthException.Network
        }
    }
}
