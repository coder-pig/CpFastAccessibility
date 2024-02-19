package cn.coderpig.cpfastaccessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import cn.coderpig.cp_fast_accessibility.*

/**
 * Author: CoderPig
 * Date: 2023-03-24
 * Desc:
 */
class MyAccessibilityService : FastAccessibilityService() {
    override fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
//        result.findNodeByText("搜索").click()
        result.findAllTextNode(true).nodes.forEach { logD("$wrapper | $it ") }
    }

}