package cn.coderpig.cp_fast_accessibility

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

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


fun String?.blankOrThis() = if (this.isNullOrBlank()) "" else this

fun CharSequence?.blankOrThis() = if (this.isNullOrBlank()) "" else this.toString()