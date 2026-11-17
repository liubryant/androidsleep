package cn.cjym.timesleep.service

import android.app.Activity
import com.alipay.sdk.app.PayTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

internal data class MemberStatus(val active: Boolean, val expiresAt: String) {
    val expiryDateLabel: String?
        get() = runCatching { java.time.LocalDate.parse(expiresAt.take(10)) }.getOrNull()
            ?.let { "${it.year}年${it.monthValue}月${it.dayOfMonth}日" }
}
internal data class MemberOrder(val id: String, val orderString: String, val status: String)
internal class MembershipAuthException : IllegalStateException("登录已过期，请重新登录")

internal object MembershipPayment {
    const val BASE_URL = "https://www.cjym123.cn/im/bot/navi/vip"
    // Production server's dedicated TimeSleep app and callback; do not use AgentClaw's merchant app.
    const val ALIPAY_APP_ID = "2021006190660434"
    private val client = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
    private suspend fun request(path: String, token: String, body: JsonObject? = null): JsonObject = withContext(Dispatchers.IO) {
        if (token.isBlank()) throw MembershipAuthException()
        val builder = Request.Builder().url(BASE_URL + path).header("Authorization", "Bearer $token")
        if (body != null) builder.post(body.toString().toRequestBody("application/json".toMediaType()))
        client.newCall(builder.build()).execute().use { response ->
            if (response.code == 401) throw MembershipAuthException()
            if (!response.isSuccessful) throw IllegalStateException("会员接口 HTTP ${response.code}")
            parseResponse(response.body?.string().orEmpty())
        }
    }
    internal fun parseResponse(raw: String): JsonObject {
        val root = Json.parseToJsonElement(raw).jsonObject
        val code = root.string("code")
        if (code == "401") throw MembershipAuthException()
        if (code != "0") throw IllegalStateException(root.string("msg").ifBlank { "会员请求失败" }.take(100))
        return root["data"] as? JsonObject ?: throw IllegalStateException("会员接口返回格式错误")
    }
    internal fun parseStatus(data: JsonObject) = MemberStatus(
        listOf("active", "isVip", "isMember").any { data.string(it) in listOf("true", "1") }, data.string("expiresAt"),
    )
    suspend fun membership(token: String) = parseStatus(request("/membership", token))
    suspend fun createOrder(token: String, productId: String): MemberOrder {
        val data = request("/orders", token, buildJsonObject {
            put("productId", productId); put("payChannel", "alipay"); put("appid", ALIPAY_APP_ID); put("platform", "android")
        })
        return parseCreatedOrder(data)
    }
    internal fun parseCreatedOrder(data: JsonObject): MemberOrder {
        if (data.string("mock") == "true") throw IllegalStateException("服务器返回模拟订单，未发起真实支付")
        val id = data.string("orderId").ifBlank { data.string("id") }
        val signed = listOf("aliPayOrderString", "orderString", "alipayOrderString").firstNotNullOfOrNull {
            data.string(it).takeIf(String::isNotBlank)
        }.orEmpty()
        require(id.matches(Regex("[A-Za-z0-9_-]+")) && signed.isNotBlank()) { "支付订单参数不完整" }
        return MemberOrder(id, signed, data.string("status"))
    }
    suspend fun orderStatus(token: String, orderId: String): String {
        require(orderId.matches(Regex("[A-Za-z0-9_-]+")))
        return request("/orders/$orderId", token).string("status")
    }
    suspend fun pay(activity: Activity, order: MemberOrder): String = withContext(Dispatchers.IO) {
        PayTask(activity).payV2(order.orderString, true)["resultStatus"].orEmpty()
    }
    private fun JsonObject.string(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
}
