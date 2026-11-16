package cn.cjym.timesleep.service

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class MembershipPaymentTest {
    @Test fun acceptsSignedOrderAliasesFromReferenceApi() {
        for (field in listOf("aliPayOrderString", "orderString", "alipayOrderString")) {
            val data = Json.parseToJsonElement("""{"orderId":"HS123","$field":"signed-order"}""").jsonObject
            assertEquals(MemberOrder("HS123", "signed-order", ""), MembershipPayment.parseCreatedOrder(data))
        }
    }
    @Test fun refusesMockOrIncompleteOrders() {
        for (raw in listOf(
            """{"mock":true,"id":"HS1","orderString":"test"}""",
            """{"id":"HS1"}""", """{"orderString":"test"}""",
            """{"id":"../membership","orderString":"test"}""",
        )) {
            try { MembershipPayment.parseCreatedOrder(Json.parseToJsonElement(raw).jsonObject); fail("Invalid order accepted") }
            catch (_: IllegalArgumentException) {} catch (_: IllegalStateException) {}
        }
    }
    @Test fun rejectsExpiredLoginAndBusinessErrors() {
        try { MembershipPayment.parseResponse("""{"code":401,"msg":"expired"}"""); fail("Expected expired session") }
        catch (_: MembershipAuthException) {}
        try { MembershipPayment.parseResponse("""{"code":500,"msg":"支付未配置"}"""); fail("Expected business error") }
        catch (e: IllegalStateException) { assertEquals("支付未配置", e.message) }
    }
    @Test fun entitlementIsNotInferredFromPaymentStatus() {
        val inactive = MembershipPayment.parseStatus(Json.parseToJsonElement("""{"status":"PAID","active":false}""").jsonObject)
        assertFalse(inactive.active)
        val active = MembershipPayment.parseStatus(Json.parseToJsonElement("""{"active":true,"expiresAt":"2027-01-01 00:00:00"}""").jsonObject)
        assertTrue(active.active)
        assertEquals("2027-01-01 00:00:00", active.expiresAt)
    }
    @Test fun loginRetainsBothSupportedTokenFields() {
        val json = Json { ignoreUnknownKeys = true }
        assertEquals("access", json.decodeFromString<AppUserLoginResult>("""{"phone":"13800000000","accessToken":"access"}""").resolvedAccessToken)
        assertEquals("legacy", json.decodeFromString<AppUserLoginResult>("""{"phone":"13800000000","token":"legacy"}""").resolvedAccessToken)
        assertEquals("", json.decodeFromString<AppUserLoginResult>("""{"phone":"13800000000"}""").resolvedAccessToken)
    }
}
