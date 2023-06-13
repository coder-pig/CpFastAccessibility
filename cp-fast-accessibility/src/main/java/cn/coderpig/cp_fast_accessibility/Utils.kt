package cn.coderpig.cp_fast_accessibility

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.lang.Exception

/**
 * Author: CoderPig
 * Date: 2023-03-24
 * Desc: 工具代码
 */

/**
 * 跳转无障碍服务设置页
 * */
fun jumpAccessibilityServiceSettings(
    cls: Class<*> = FastAccessibilityService.specificServiceClass,
    ctx: Context = FastAccessibilityService.appContext
) {
    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val cs = ComponentName(FastAccessibilityService.appContext.packageName, cls.name).flattenToString()
        putExtra(":settings:fragment_args_key", cs)
        putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", cs) })
    })
}


fun String?.blankOrThis(blankStr: String = "") = if (this.isNullOrBlank()) blankStr else this

fun CharSequence?.blankOrThis(blankStr: String = "") = if (this.isNullOrBlank()) blankStr else this.toString()

fun <I> I?.expressionResult(expression: (I) -> Boolean, correctCallback: (I) -> Unit) {
    this?.let{ if (expression.invoke(it)) correctCallback.invoke(it) }
}

@SuppressLint("UseCompatLoadingForDrawables")
fun Context.getDrawableRes(resId: Int): Drawable =
    applicationContext.resources.getDrawable(resId, null)

fun Context.getStringRes(resId: Int): String = applicationContext.resources.getString(resId, "")

fun Context.shortToast(msg: String) =
    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()





const val TAG = "FastAccessibilityService"
const val SEGMENT_SIZE = 3072

/**
 * 支持超长日志输出的工具方法
 * */
fun logD(content: String) {
    if (content.length < SEGMENT_SIZE) {
        Log.d(TAG, content)
        return
    } else {
        Log.d(TAG, content.substring(0, SEGMENT_SIZE))
        logD(content.substring(SEGMENT_SIZE))
    }
}
