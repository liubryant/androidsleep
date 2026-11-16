package cn.cjym.timesleep.service

import org.junit.Assert.*
import org.junit.Test

class MembershipProductsTest {
    @Test fun parsesSharedCatalogResponse() {
        val products = MembershipProducts.parse("""{"code":0,"data":{"items":[
            {"id":"week_vip","name":"周度会员","price":"9.90","durationDays":7,"description":"解锁全部会员权益"},
            {"id":"month_vip","name":"月度会员","price":"19.90","durationDays":30},
            {"id":"year_vip","name":"年度会员","price":"168.00","durationDays":365}
        ],"total":3}}""")
        assertEquals(listOf("week_vip", "month_vip", "year_vip"), products.map { it.id })
        assertEquals(listOf("9.9", "19.9", "168"), products.map { it.price })
    }

    @Test fun parsesServerProductFieldsAndPrices() {
        val list=MembershipProducts.parse("""{"code":0,"data":[{"id":7,"name":"月会员","price":"19.90","description":"30天"}]}""")
        assertEquals(MembershipProduct("7","月会员","30天","19.9"),list.single())
    }
    @Test fun acceptsSupportedListEnvelopes() {
        for(key in listOf("list","products","items")) assertTrue(MembershipProducts.parse("""{"code":"0","data":{"$key":[]}}""").isEmpty())
    }
    @Test fun reportsBusinessErrorEvenWhenHttpSucceeded() {
        try {MembershipProducts.parse("""{"code":500,"msg":"服务器错误"}""");fail("Expected error")}
        catch(e:IllegalStateException) {assertTrue(e.message!!.contains("500"))}
    }
    @Test fun rejectsInvalidOrMissingPrices() {
        for(price in listOf("-1","abc","")) {
            try {MembershipProducts.parse("""{"code":0,"data":[{"id":"p","name":"套餐","price":"$price"}]}""");fail("Expected invalid price")}
            catch(e:IllegalStateException) {assertTrue(e.message!!.contains("价格"))}
        }
    }
}
