package cn.coderpig.cpfastaccessibility

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import cn.coderpig.cp_fast_accessibility.FastAccessibilityService
import cn.coderpig.cp_fast_accessibility.isAccessibilityEnable
import cn.coderpig.cp_fast_accessibility.requireAccessibility

class MainActivity : AppCompatActivity() {
    private lateinit var mServiceStatusIv: ImageView
    private lateinit var mServiceStatusTv: TextView
    private lateinit var mOpenTargetAppBt: Button
    private val mClickListener = View.OnClickListener {
        when (it.id) {
            R.id.iv_service_status -> {
                if (isAccessibilityEnable) shortToast(getStringRes(R.string.service_is_enable_tips))
                else requireAccessibility()
            }
            R.id.bt_open_target_app -> startApp("com.tencent.mm", "com.tencent.mm.ui.LauncherUI", "未安装微信")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    private fun initView() {
        mServiceStatusIv = findViewById(R.id.iv_service_status)
        mServiceStatusTv = findViewById(R.id.tv_service_status)
        mOpenTargetAppBt = findViewById(R.id.bt_open_target_app)
        mServiceStatusIv.setOnClickListener(mClickListener)
        mOpenTargetAppBt.setOnClickListener(mClickListener)
    }

    override fun onResume() {
        super.onResume()
        if (isAccessibilityEnable) {
            mServiceStatusIv.setImageDrawable(getDrawableRes(R.drawable.ic_service_enable))
            mServiceStatusTv.text = getStringRes(R.string.service_status_enable)
            mOpenTargetAppBt.visibility = View.VISIBLE
        } else {
            mServiceStatusIv.setImageDrawable(getDrawableRes(R.drawable.ic_service_disable))
            mServiceStatusTv.text = getStringRes(R.string.service_status_disable)
            mOpenTargetAppBt.visibility = View.GONE
        };
    }
}