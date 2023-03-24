package cn.coderpig.cpfastaccessibility

import android.util.Log
import cn.coderpig.cp_fast_accessibility.*

/**
 * Author: CoderPig
 * Date: 2023-03-24
 * Desc:
 */
class MyAccessibilityService : FastAccessibilityService() {
    companion object {
        private const val TAG = "CpFastAccessibility"
    }

    override val enableListenApp = true

    override fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        result.findNodeByText("搜索").click()
        result.findAllTextNode(true).nodes.forEach { Log.e(TAG, "$wrapper | $it ") }
    }
}