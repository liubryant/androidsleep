package cn.cjym.timesleep

import android.app.Activity
import android.os.Bundle

/** Return target only; entitlement is always confirmed by the server. */
class AlipayResultActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
