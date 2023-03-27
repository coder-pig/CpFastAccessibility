package cn.coderpig.cp_fast_accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import org.w3c.dom.Node
import kotlin.random.Random

/**
 * Author: CoderPig
 * Date: 2023-03-24
 * Desc: 快速调用API
 */

/**
 * 无障碍服务是否可用
 * */
val isAccessibilityEnable get() = FastAccessibilityService.isServiceEnable

/**
 * 是否监听App
 * */
var isListenApp: Boolean
    get() = FastAccessibilityService.enableListenApp
    set(value) {
        FastAccessibilityService.enableListenApp = value
    }

/**
 * 显示前台服务
 * */
@RequiresApi(Build.VERSION_CODES.O)
fun showForegroundNotification(
    title: String = "通知标题",
    content: String = "通知内容",
    ticker: String = "通知提示语",
    iconRes: Int? = null,
    activityClass: Class<*>? = null
) = FastAccessibilityService.showForegroundNotification(title, content, ticker, iconRes, activityClass)

/**
 * 隐藏前台服务
 * */
fun closeForegroundNotification() = FastAccessibilityService.closeForegroundNotification()

/**
 * 请求无障碍服务
 * */
fun requireAccessibility() = FastAccessibilityService.requireAccessibility()

/**
 * 全局操作快速调用
 * */
fun performAction(action: Int) = FastAccessibilityService.require.performGlobalAction(action)

// 返回
fun back() = performAction(AccessibilityService.GLOBAL_ACTION_BACK)

// Home键
fun home() = performAction(AccessibilityService.GLOBAL_ACTION_HOME)

// 最近任务
fun recent() = performAction(AccessibilityService.GLOBAL_ACTION_RECENTS)

// 电源菜单
fun powerDialog() = performAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)

// 通知栏
fun notificationBar() = performAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)

// 通知栏 → 快捷设置
fun quickSettings() = performAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)

// 锁屏
@RequiresApi(Build.VERSION_CODES.P)
fun lockScreen() = performAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)

// 应用分屏
fun splitScreen() = performAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)

// 休眠
fun sleep(millis: Long) = Thread.sleep(millis)

/**
 * 手势模拟相关
 * */

// 快速生成GestureDescription的方法
fun fastGestureDescription(operate: (Path) -> Unit, startTime: Long = 0L, duration: Long = 50L): GestureDescription =
    GestureDescription.Builder().apply {
        addStroke(GestureDescription.StrokeDescription(Path().apply {
            operate.invoke(this)
        }, startTime, duration))
    }.build()

// 快速生成GestureResultCallback的方法
fun fastGestureCallback() = object : AccessibilityService.GestureResultCallback() {
    override fun onCompleted(gestureDescription: GestureDescription?) {
        super.onCompleted(gestureDescription)
        // 手势执行完成回调
    }
}

/**
 * 使用手势模拟点击，长按的话，传入的Duration大一些就好，比如1000(1s)
 *
 * @param x 点击坐标点的x坐标
 * @param y 点击坐标点的y坐标
 * @param delayTime 延迟多久进行本次点击，单位毫秒
 * @param duration 模拟触摸屏幕的时长(按下到抬起)，太短会导致部分应用下点击无效，单位毫秒
 * @param repeatCount 本次点击重复多少次，必须大于0
 * @param randomPosition 点击位置随机偏移距离，用于反检测
 * @param randomTime 在随机参数上加减延时时长，有助于防止点击器检测，单位毫秒
 *
 * */
fun click(
    x: Int,
    y: Int,
    delayTime: Long = 0,
    duration: Long = 200,
    repeatCount: Int = 1,
    randomPosition: Int = 0,
    randomTime: Long = 0
) {
    repeat(repeatCount) {
        // 生成(-randomPosition，randomPosition]间的随机数
        val tempX = x + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempY = y + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempDuration = duration + Random.nextLong(0 - randomTime, randomTime + 1)
        FastAccessibilityService.require.dispatchGesture(
            fastGestureDescription({
                it.moveTo(
                    if (tempX < 0) x.toFloat() else tempX.toFloat(),
                    if (tempY < 0) y.toFloat() else tempY.toFloat()
                )
            }, delayTime, tempDuration), fastGestureCallback(), null
        )
    }
}


/**
 * 使用手势模拟滑动
 *
 * @param startX 滑动起始坐标点的x坐标
 * @param startY 滑动起始坐标点的y坐标
 * @param endX 滑动终点坐标点的x坐标
 * @param endY 滑动终点坐标点的y坐标
 * @param duration 滑动持续时间，一般在300~500ms效果会好一些，太快会导致滑动不可用
 * @param repeatCount 滑动重复次数
 * @param randomPosition 点击位置随机偏移距离，用于反检测
 * @param randomTime 在随机参数上加减延时时长，有助于防止点击器检测，单位毫秒
 * */
fun swipe(
    startX: Int,
    startY: Int,
    endX: Int,
    endY: Int,
    duration: Long = 1000L,
    repeatCount: Int = 1,
    randomPosition: Int = 0,
    randomTime: Long = 0
) {
    repeat(repeatCount) {
        // 生成(-randomPosition，randomPosition]间的随机数
        val tempStartX = startX + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempStartY = startY + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempEndX = endX + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempEndY = endY + Random.nextInt(0 - randomPosition, randomPosition + 1)
        val tempDuration = duration + Random.nextLong(0 - randomTime, randomTime + 1)
        FastAccessibilityService.require.dispatchGesture(fastGestureDescription({
            it.moveTo(
                if (tempStartX < 0) startX.toFloat() else tempStartX.toFloat(),
                if (tempStartY < 0) startY.toFloat() else tempStartY.toFloat()
            )
            it.lineTo(
                if (tempEndX < 0) endX.toFloat() else tempEndX.toFloat(),
                if (tempEndY < 0) endY.toFloat() else tempEndY.toFloat()
            )
        }, tempDuration), fastGestureCallback(), null)
    }
}

/**
 * 结点操作相关
 * */

/**
 * 快速查找到操作结点并执行操作的方法
 *
 * @param condition 结点判断表达式
 * @param action 执行的操作
 * */
fun AccessibilityNodeInfo?.fastFindAction(
    condition: (AccessibilityNodeInfo) -> Boolean,
    action: (AccessibilityNodeInfo) -> Unit
) {
    if (this == null) return
    var depthCount = 0  // 查找深度
    var tempNode: AccessibilityNodeInfo? = this // 临时结点
    while (true) {
        if (depthCount < 10 && tempNode != null) {
            if (condition.invoke(tempNode)) {
                action.invoke(tempNode)
            } else {
                tempNode = tempNode.parent
                depthCount++
            }
        } else break
    }
}


/**
 * 结点点击，不过现在很多APP都屏蔽了结点点击，所以默认使用手势模拟
 *
 * @param gestureClick 是否使用手势模拟点击，默认true
 * @param duration 点击时长，默认200ms
 * */
fun NodeWrapper?.click(gestureClick: Boolean = true, duration: Long = 200L) {
    if (this == null) return
    if (gestureClick) {
        bounds?.let {
            val centerX = (it.left + it.right) / 2
            val centerY = (it.left + it.right) / 2
            if (centerX >= 0 && centerY >= 0) click(centerX, centerY, 0, duration)
        }
    } else {
        nodeInfo.fastFindAction({ it.isClickable }, {
            it.performAction(if (duration >= 1000L) AccessibilityNodeInfo.ACTION_LONG_CLICK else AccessibilityNodeInfo.ACTION_CLICK)
        })
    }
}

/**
 * 结点长按，不过现在很多APP都屏蔽了结点点击，所以默认使用手势模拟
 *
 * @param gestureClick 是否使用手势模拟点击，默认true
 * @param duration 点击时长，默认时长1000ms
 * */
fun NodeWrapper?.longClick(gestureClick: Boolean = true, duration: Long = 1000L) {
    if (this == null) return
    click(gestureClick, duration)
}

/**
 * 向前滑动
 * */
fun NodeWrapper?.forward(isForward: Boolean = true) {
    if (this == null) return
    nodeInfo.fastFindAction({ it.isScrollable }, {
        it.performAction(if (isForward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    })
}

/**
 * 向后滑动
 * */
fun NodeWrapper?.backward() = forward(false)


/**
 * 文本填充
 *
 * @param content 要填充的文本
 * */
fun NodeWrapper?.input(content: String) {
    if (this == null) return
    nodeInfo.fastFindAction({ it.isEditable }, {
        it.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, content)
        })
    })
}


/**
 * 结点解析结果快速调用
 * */

/**
 * 根据文本查找结点
 *
 * @param text 匹配的文本
 * @param textAllMatch 文本全匹配
 * @param includeDesc 同时匹配desc
 * @param descAllMatch desc全匹配
 * @param enableRegular 是否启用正则
 * */
fun AnalyzeSourceResult.findNodeByText(
    text: String,
    textAllMatch: Boolean = false,
    includeDesc: Boolean = false,
    descAllMatch: Boolean = false,
    enableRegular: Boolean = false,
): NodeWrapper? {
    if (enableRegular) {
        val regex = Regex(text)
        nodes.forEach { node ->
            if (!node.text.isNullOrBlank()) {
                if (regex.find(node.text!!) != null) return node
            }
            if (includeDesc && !node.description.isNullOrBlank()) {
                if (regex.find(node.description!!) != null) return node
            }
        }
    } else {
        nodes.forEach { node ->
            if (!node.text.isNullOrBlank()) {
                if (textAllMatch) {
                    if (text == node.text) return node
                } else {
                    if (node.text!!.contains(text)) return node
                }
            }
            if (includeDesc && !node.description.isNullOrBlank()) {
                if (descAllMatch) {
                    if (text == node.description) return node
                } else {
                    if (node.description!!.contains(text)) return node
                }
            }
        }
    }
    return null
}

/**
 * 根据文本查找结点列表
 *
 * @param text 匹配的文本
 * @param textAllMatch 文本全匹配
 * @param includeDesc 同时匹配desc
 * @param descAllMatch desc全匹配
 * @param enableRegular 是否启用正则
 * */
fun AnalyzeSourceResult.findNodesByText(
    text: String,
    textAllMatch: Boolean = false,
    includeDesc: Boolean = false,
    descAllMatch: Boolean = false,
    enableRegular: Boolean = false,
): AnalyzeSourceResult {
    val result = AnalyzeSourceResult()
    if (enableRegular) {
        val regex = Regex(text)
        nodes.forEach { node ->
            if (!node.text.isNullOrBlank()) {
                if (regex.find(node.text!!) != null) {
                    result.nodes.add(node)
                    return@forEach
                }
            }
            if (includeDesc && !node.description.isNullOrBlank()) {
                if (regex.find(node.description!!) != null) {
                    result.nodes.add(node)
                    return@forEach
                }
            }
        }
    } else {
        nodes.forEach { node ->
            if (!node.text.isNullOrBlank()) {
                if (textAllMatch) {
                    if (text == node.text) {
                        result.nodes.add(node)
                        return@forEach
                    }
                } else {
                    if (node.text!!.contains(text)) {
                        result.nodes.add(node)
                        return@forEach
                    }
                }
            }
            if (includeDesc && !node.description.isNullOrBlank()) {
                if (descAllMatch) {
                    if (text == node.description) {
                        result.nodes.add(node)
                        return@forEach
                    }
                } else {
                    if (node.description!!.contains(text)) {
                        result.nodes.add(node)
                        return@forEach
                    }
                }
            }
        }
    }
    return result
}

/**
 * 根据id查找结点 (模糊匹配)
 *
 * @param ids 结点id，可传入多个
 * */
fun AnalyzeSourceResult.findNodeById(vararg ids: String): NodeWrapper? {
    nodes.forEach { node ->
        if (!node.id.isNullOrBlank()) {
            ids.forEach { id -> if (node.id!!.contains(id)) return node }
        }
    }
    return null
}

/**
 * 根据id查找结点列表 (模糊匹配)
 *
 * @param ids 结点id, 可传入多个
 * */
fun AnalyzeSourceResult.findNodesById(vararg ids: String): AnalyzeSourceResult {
    val result = AnalyzeSourceResult()
    nodes.forEach { node ->
        if (!node.id.isNullOrBlank()) {
            ids.forEach { id -> if (node.id!!.contains(id)) result.nodes.add(node) }
        }
    }
    return result
}

/**
 * 根据传入的表达式结果查找结点
 *
 * @param expression 匹配条件表达式
 * */
fun AnalyzeSourceResult.findNodeByExpression(expression: (NodeWrapper) -> Boolean): NodeWrapper? {
    nodes.forEach { node ->
        if (expression.invoke(node)) return node
    }
    return null
}

/**
 * 根据传入的表达式结果查找结点列表
 *
 * @param expression 匹配条件表达式
 * */
fun AnalyzeSourceResult.findNodesByExpression(expression: (NodeWrapper) -> Boolean): AnalyzeSourceResult {
    val result = AnalyzeSourceResult()
    nodes.forEach { node ->
        if (expression.invoke(node)) result.nodes.add(node)
    }
    return result
}

/**
 * 查找所有文本不为空的结点
 * */
fun AnalyzeSourceResult.findAllTextNode(includeDesc: Boolean = false): AnalyzeSourceResult {
    val result = AnalyzeSourceResult()
    nodes.forEach { node ->
        if (!node.text.isNullOrBlank()) {
            result.nodes.add(node)
            return@forEach
        }
        if (includeDesc && !node.description.isNullOrBlank()) {
            result.nodes.add(node)
            return@forEach
        }
    }
    return result
}

