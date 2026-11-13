package cn.cjym.timesleep.service

import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request

internal data class MembershipProduct(val id:String,val name:String,val description:String,val price:String)

internal object MembershipProducts {
    const val URL = "https://www.cjym123.cn/im/bot/sleep/vip/products"
    private val client=OkHttpClient.Builder().callTimeout(15,TimeUnit.SECONDS).build()
    suspend fun load():List<MembershipProduct> = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(URL).get().build()).execute().use {response->
            if(!response.isSuccessful) throw IllegalStateException("商品接口 HTTP ${response.code}")
            parse(response.body?.string() ?: throw IllegalStateException("商品接口返回空响应"))
        }
    }
    internal fun parse(body:String):List<MembershipProduct> {
        val root=try {Json.parseToJsonElement(body).jsonObject} catch(e:Exception) {throw IllegalStateException("商品接口返回格式错误",e)}
        val code=(root["code"] as? JsonPrimitive)?.contentOrNull
        if(code!="0") {
            val message=(root["msg"] as? JsonPrimitive)?.contentOrNull ?: (root["message"] as? JsonPrimitive)?.contentOrNull ?: "请求失败"
            throw IllegalStateException("${message.take(100)}（code: ${code ?: "缺失"}）")
        }
        val data=root["data"]
        val items=when(data) {
            is JsonArray->data
            is JsonObject->listOf("list","products","items").firstNotNullOfOrNull {data[it] as? JsonArray}
            else->null
        } ?: throw IllegalStateException("商品列表格式错误")
        val products=items.map {item->
            val obj=item as? JsonObject ?: throw IllegalStateException("商品信息格式错误")
            fun field(name:String)=(obj[name] as? JsonPrimitive)?.contentOrNull.orEmpty().trim()
            val id=field("id");val name=field("name");val amount=field("price").toBigDecimalOrNull()
            if(id.isBlank() || name.isBlank() || amount==null || amount<BigDecimal.ZERO) throw IllegalStateException("商品信息缺少编号、名称或有效价格")
            MembershipProduct(id,name,field("description"),amount.stripTrailingZeros().toPlainString())
        }
        if(products.distinctBy {it.id}.size!=products.size) throw IllegalStateException("商品编号重复")
        return products
    }
}
