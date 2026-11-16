package cn.cjym.timesleep.ui.wellness

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import cn.cjym.timesleep.service.MembershipProducts
import cn.cjym.timesleep.service.MembershipProduct
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import cn.cjym.timesleep.service.MembershipPayment
import cn.cjym.timesleep.service.MembershipAccess
import cn.cjym.timesleep.service.MembershipAuthException
import cn.cjym.timesleep.R
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.ui.profile.LoginScreen
import cn.cjym.timesleep.ui.profile.LegalWebViewScreen

@Composable fun MembershipScreen(back: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TimeSleepApp
    val activity = context as? android.app.Activity
    val scope = rememberCoroutineScope()
    val loggedIn by app.settings.isLoggedIn.collectAsState(initial = false)
    val phone by app.settings.phoneNumber.collectAsState(initial = "")
    val token by app.settings.accessToken.collectAsState(initial = "")
    val memberActive by MembershipAccess.active.collectAsState()
    var page by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var benefits by remember { mutableStateOf(false) }
    var agreement by remember { mutableStateOf(false) }
    var products by remember { mutableStateOf(MembershipProducts.cached(context)) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var paymentMessage by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(retry) {
        loading = true; loadError = null
        try {
            val selectedId = products.getOrNull(selected)?.id
            val updated = MembershipProducts.load(context)
            products = updated
            selected = updated.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 }
                ?: selected.coerceIn(0, (updated.size - 1).coerceAtLeast(0))
        }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { loadError = e.message ?: "网络连接失败" }
        finally { loading = false }
    }
    LaunchedEffect(token, refresh) {
        if (token.isNotBlank() && !busy) {
            try {
                val pending = app.settings.pendingOrder()
                if (pending != null) {
                    val status = MembershipPayment.orderStatus(token, pending)
                    if (status == "PAID") {
                        val active = MembershipAccess.refresh(token)
                        if (active) app.settings.clearPendingOrder(pending)
                        paymentMessage = if (active) "支付已确认，会员权益已同步" else "支付已确认，权益同步中，请稍后点击刷新"
                    } else if (status in listOf("CLOSED", "CANCELLED", "FAILED")) app.settings.clearPendingOrder(pending)
                }
                MembershipAccess.refresh(token)
            } catch (e: CancellationException) { throw e }
            catch (e: MembershipAuthException) { MembershipAccess.reset(); paymentMessage = e.message; page = "login" }
            catch (e: Exception) { paymentMessage = e.message ?: "会员状态同步失败，点击重试" }
        }
    }
    val resumeCallback by rememberUpdatedState { if (!busy) refresh++ }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumeCallback()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val retryCallback by rememberUpdatedState<() -> Unit>({ if (!loading && !busy) { retry++; refresh++; paymentMessage = null } })
    val selectCallback by rememberUpdatedState<(Int) -> Unit>({ if (!busy) selected = it })
    val checkCallback by rememberUpdatedState<() -> Unit>({ if (!busy) accepted = !accepted })
    val payCallback by rememberUpdatedState<() -> Unit>({
        when {
            busy || loading -> Unit
            !loggedIn || token.isBlank() -> { paymentMessage = "请先登录后开通会员"; page = "login" }
            !accepted -> { paymentMessage = "请先阅读并同意用户协议和隐私政策"; agreement = true }
            activity == null -> { paymentMessage = "无法打开支付，请重新进入会员页面" }
            else -> products.getOrNull(selected)?.let { product ->
                val paymentToken = token
                val paymentPhone = phone
                busy = true
                scope.launch {
                    try {
                        paymentMessage = "正在创建订单…"
                        val order = MembershipPayment.createOrder(paymentToken, product.id)
                        app.settings.savePendingOrder(order.id, paymentPhone)
                        paymentMessage = "请在支付宝中完成支付"
                        val result = MembershipPayment.pay(activity, order)
                        val attempts = if (result in listOf("9000", "8000", "6004")) 15 else 1
                        paymentMessage = "正在向服务器确认支付结果…"
                        var paid = false
                        for (attempt in 0 until attempts) {
                            if (app.settings.accessToken.first() != paymentToken) break
                            val status = MembershipPayment.orderStatus(paymentToken, order.id)
                            if (status == "PAID") {
                                val active = MembershipAccess.refresh(paymentToken)
                                if (active) app.settings.clearPendingOrder(order.id)
                                paymentMessage = if (active) "支付成功，会员权益已开通" else "支付已确认，权益同步中，请稍后点击刷新"
                                paid = true
                                break
                            }
                            if (status in listOf("CLOSED", "CANCELLED", "FAILED")) {
                                app.settings.clearPendingOrder(order.id)
                                break
                            }
                            if (attempt < attempts - 1) delay(2000)
                        }
                        if (!paid) paymentMessage = when (result) {
                            "6001" -> "已取消支付"
                            "9000", "8000", "6004" -> "支付结果待确认，请稍后点击刷新；如已扣款请勿重复支付"
                            else -> "支付未完成，请检查支付宝后重试"
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (e: MembershipAuthException) { MembershipAccess.reset(); paymentMessage = e.message; page = "login" }
                    catch (e: Exception) { paymentMessage = e.message ?: "支付失败，请稍后重试" }
                    finally { busy = false }
                }
            }
        }
    })
    BackHandler(enabled = busy) { paymentMessage = "正在确认支付，请稍候" }
    if (page.isNotEmpty()) {
        BackHandler { page = "" }
        if (page == "login") LoginScreen(onBack = { page = ""; refresh++ })
        else LegalWebViewScreen(type = page, onBack = { page = "" })
        return
    }
    AndroidView(modifier = Modifier.fillMaxSize(), factory = { viewContext ->
        val root = LayoutInflater.from(viewContext).inflate(R.layout.activity_vip, null, false)
        root.tag = MemberViewController(viewContext, root) { selectCallback(it) }
        root.findViewById<View>(R.id.vip_status).setOnClickListener { retryCallback() }
        root.findViewById<View>(R.id.vip_back).apply { contentDescription = "返回"; setOnClickListener { if (!busy) back() } }
        root.findViewById<View>(R.id.vip_login_button).setOnClickListener { page = "login" }
        root.findViewById<View>(R.id.vip_benefits_card).setOnClickListener { benefits = true }
        root.findViewById<View>(R.id.vip_agreement_check).setOnClickListener { checkCallback() }
        root.findViewById<View>(R.id.vip_agreement_text).setOnClickListener { agreement = true }
        root.findViewById<View>(R.id.vip_pay_button).setOnClickListener { payCallback() }
        root
    }, update = { root ->
        (root.tag as MemberViewController).render(products, loading, loadError, selected)
        root.findViewById<TextView>(R.id.vip_phone).text = if (loggedIn) (if (phone.length >= 7) phone.take(3) + "****" + phone.takeLast(4) else phone) else "登录后开通会员"
        root.findViewById<View>(R.id.vip_login_button).visibility = if (loggedIn && token.isNotBlank()) View.GONE else View.VISIBLE
        root.findViewById<View>(R.id.vip_agreement_check).apply { isSelected = accepted; contentDescription = if (accepted) "已同意协议" else "同意协议" }
        root.findViewById<View>(R.id.vip_pay_button).apply {
            isEnabled = !busy && !loading && loadError == null && products.isNotEmpty()
            contentDescription = if (memberActive) "立即续费" else "立即开通"
        }
        if (paymentMessage != null) root.findViewById<TextView>(R.id.vip_status).text = paymentMessage
        root.findViewById<TextView>(R.id.vip_bottom_hint).text = when {
            busy -> "处理中…"
            loading -> "加载中"
            products.isEmpty() -> "暂无套餐"
            memberActive -> "立即续费"
            else -> "立即开通"
        }
    })
    if (benefits) AlertDialog(onDismissRequest = { benefits = false }, title = { Text("会员套餐详情") },
        text = { Text("全部正念冥想\n解锁会员正念练习与课程\n\n全部助眠声音\n解锁更多自然声音\n\n内容持续更新\n更多会员音频陆续上线。") },
        confirmButton = { TextButton(onClick = { benefits = false }) { Text("关闭") } })
    if (agreement) AlertDialog(onDismissRequest = { agreement = false }, title = { Text("查看协议") },
        text = { Text("请选择要查看的协议，阅读后勾选同意即可支付。") },
        confirmButton = { TextButton(onClick = { agreement = false; page = "agreement" }) { Text("用户协议") } },
        dismissButton = { TextButton(onClick = { agreement = false; page = "privacy" }) { Text("隐私政策") } })
}

// Layout, assets and card construction follow AgentClaw VipActivity.
// Products use the shared Android/Harmony catalog; purchase integration remains separate.
private class MemberViewController(val context:Context,val root:View,val onSelect:(Int)->Unit) {
    private val badgeTexts=listOf("限时特惠","超值特惠","80%用户选择")
    private val badgeStartColors=listOf(Color.parseColor("#9C27B0"),Color.parseColor("#FFB300"),Color.parseColor("#FF6B35"))
    private val badgeEndColors=listOf(Color.parseColor("#6A1B9A"),Color.parseColor("#F57C00"),Color.parseColor("#E53935"))
    private val cards=mutableListOf<FrameLayout>()
    private var renderedProducts:List<MembershipProduct>?=null
    private var renderedEmptyState:String?=null
    private var currentProducts:List<MembershipProduct> = emptyList()
    init {
        root.findViewById<View>(R.id.vip_pay_channel_alipay).isSelected=true
        root.findViewById<TextView>(R.id.vip_alipay_text).setTextColor(Color.parseColor("#864F2D"))
        root.findViewById<View>(R.id.vip_pay_button).isEnabled=false
    }
    fun render(list:List<MembershipProduct>,loading:Boolean,error:String?,selected:Int) {
        currentProducts=list
        val container=root.findViewById<LinearLayout>(R.id.vip_products)
        // Match AgentClaw: reserve the product row while loading, without a fake product.
        val emptyState = if (list.isEmpty() && !loading) error ?: "empty" else null
        val rebuild = renderedProducts != list || renderedEmptyState != emptyState
        if(rebuild) {
            renderedProducts=list;renderedEmptyState=emptyState;cards.clear();container.removeAllViews()
            val dp=context.resources.displayMetrics.density
            list.forEachIndexed {index,plan->
                val wrapper=buildProductCard(plan,index,dp)
                wrapper.setOnClickListener {onSelect(index)}
                container.addView(wrapper,LinearLayout.LayoutParams((138*dp).toInt(),ViewGroup.LayoutParams.MATCH_PARENT).apply {setMargins((5*dp).toInt(),0,(5*dp).toInt(),0)})
                cards.add(wrapper)
            }
        }
        val status=when {
            loading->"正在加载会员套餐…"
            error!=null->"$error · 点击重试"
            list.isEmpty()->"暂无可购买的会员套餐 · 点击刷新"
            else->"请选择会员套餐 · 点击刷新会员状态"
        }
        root.findViewById<TextView>(R.id.vip_status).text=status
        if(rebuild && list.isEmpty() && !loading) {
            container.addView(TextView(context).apply {
                text=if(error!=null) "套餐加载失败\n\n点击重试" else "暂无会员套餐\n\n点击刷新"
                textSize=14f;gravity=Gravity.CENTER;setTextColor(Color.parseColor("#EBC783"))
                setBackgroundResource(R.drawable.selector_vip_product_bg)
                setOnClickListener {root.findViewById<View>(R.id.vip_status).performClick()}
            },LinearLayout.LayoutParams((280*context.resources.displayMetrics.density).toInt(),ViewGroup.LayoutParams.MATCH_PARENT))
        }
        root.findViewById<TextView>(R.id.vip_bottom_hint).text=if(loading) "加载中" else if(list.isEmpty()) "暂无套餐" else "立即开通"
        select(selected)
    }
    fun select(index:Int) {
        root.findViewById<TextView>(R.id.vip_bottom_price).text=currentProducts.getOrNull(index)?.let {"¥${it.price}"} ?: "¥--"
        cards.forEachIndexed {i,wrapper->
            val chosen=i==index
            (wrapper.getTag(R.id.vip_product_card) as View).isSelected=chosen
            // Preserve the source page's selected/unselected text palette.
            (wrapper.getTag(R.id.vip_product_price) as TextView).setTextColor(Color.parseColor(if(chosen) "#864F2D" else "#EBC783"))
            (wrapper.getTag(R.id.vip_product_name) as TextView).setTextColor(Color.parseColor(if(chosen) "#6B3820" else "#B6A58E"))
            (wrapper.getTag(R.id.vip_product_desc) as? TextView)?.setTextColor(Color.parseColor(if(chosen) "#9B5C38" else "#7A6B58"))
        }
    }
    private fun buildProductCard(product: MembershipProduct, index: Int, dp: Float): FrameLayout {
        val badgeText = badgeTexts.getOrNull(index)
        val badgeStartColor = badgeStartColors.getOrElse(index) { Color.parseColor("#666666") }
        val badgeEndColor = badgeEndColors.getOrElse(index) { Color.parseColor("#444444") }

        val wrapper = FrameLayout(context).apply { clipChildren = false }

        // Card body
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val topPad = if (badgeText != null) (18 * dp).toInt() else (20 * dp).toInt()
            setPadding((10 * dp).toInt(), topPad, (10 * dp).toInt(), (14 * dp).toInt())
            setBackgroundResource(R.drawable.selector_vip_product_bg)
        }

        // Price
        val priceView = TextView(context).apply {
            text = "¥${product.price}"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#EBC783"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            includeFontPadding = false
        }
        card.addView(priceView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Name
        val nameView = TextView(context).apply {
            text = product.name
            textSize = 12f
            gravity = Gravity.CENTER
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(Color.parseColor("#B6A58E"))
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = (5 * dp).toInt()
            }.let { layoutParams = it }
        }
        card.addView(nameView)

        // Description
        var descView: TextView? = null
        if (product.description.isNotBlank()) {
            descView = TextView(context).apply {
                text = product.description
                textSize = 10f
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(Color.parseColor("#7A6B58"))
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                    it.topMargin = (4 * dp).toInt()
                }.let { layoutParams = it }
            }
            card.addView(descView)
        }

        val cardParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            topMargin = (16 * dp).toInt()
        }
        wrapper.addView(card, cardParams)

        // Badge floating above card top
        if (badgeText != null) {
            val badgeBg = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(badgeStartColor, badgeEndColor)
            ).apply {
                cornerRadius = 20 * dp
            }
            val badge = TextView(context).apply {
                text = badgeText
                textSize = 9.5f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = badgeBg
                val ph = (5 * dp).toInt()
                val pv = (3 * dp).toInt()
                setPadding(ph * 2, pv, ph * 2, pv + (1 * dp).toInt())
                elevation = 3 * dp
            }
            val badgeParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = (4 * dp).toInt()
            }
            wrapper.addView(badge, badgeParams)
        }

        // Store child view refs in wrapper tags for later color updates
        wrapper.setTag(R.id.vip_product_card, card)
        wrapper.setTag(R.id.vip_product_price, priceView)
        wrapper.setTag(R.id.vip_product_name, nameView)
        wrapper.setTag(R.id.vip_product_desc, descView)

        return wrapper
    }

}
