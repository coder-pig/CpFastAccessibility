package cn.coderpig.cp_fast_accessibility

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * Author: CoderPig
 * Date: 2023-03-24
 * Desc: 无障碍服务基类
 */
abstract class FastAccessibilityService : AccessibilityService() {
    companion object {
        var instance: FastAccessibilityService? = null  // 无障碍服务对象实例，暴露给外部调用
        val isServiceEnable: Boolean get() = instance != null   // 无障碍服务是否可用
        private var _appContext: Context? = null    // 幕后属性，对外表现为只读，对内表现为可读写
        val appContext get() = _appContext ?: throw NullPointerException("需要在Application的onCreate()中调用init()")
        lateinit var specificServiceClass: Class<*> // 具体无障碍服务实现类的类类型
        private var listenEventTypeList = arrayListOf(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) // 监听的event类型列表
        val currentEvent get() = instance?.currentEventWrapper  // 获取当前Event
        var enableListenApp: Boolean = true    // 是否监听APP的标记，默认监听

        /**
         * 库初始化方法，必须在Application的OnCreate()中调用
         *
         * @param context Context上下文
         * @param clazz 无障碍服务的类类型
         * @param typeList 监听的事件类型列表，不传默认只监听TYPE_WINDOW_STATE_CHANGED类型
         * */
        fun init(
            context: Context,
            clazz: Class<*>,
            typeList: ArrayList<Int>? = null,
        ) {
            _appContext = context.applicationContext
            specificServiceClass = clazz
            typeList?.let { listenEventTypeList = it }
        }

        /**
         * 请求无障碍服务权限，即跳转无障碍设置页
         * */
        fun requireAccessibility() {
            if (!isServiceEnable) jumpAccessibilityServiceSettings()
        }

        /**
         * 无障碍服务Action套一层，没权限直接跳设置
         * */
        val require get() = run { requireAccessibility(); instance!! }
    }

    var currentEventWrapper: EventWrapper? = null   // 当前Event
        private set
    var executor: ExecutorService = Executors.newFixedThreadPool(4) // 执行任务的线程池

    var foregroundNotification: Notification? = null    // 前台服务

    override fun onServiceConnected() {
        if (this::class.java == specificServiceClass) instance = this
    }

    override fun onDestroy() {
        if (this::class.java == specificServiceClass) instance = null
        executor.shutdown()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!enableListenApp || event == null) return
        if (event.eventType in listenEventTypeList) {
            val className = event.className.blankOrThis()
            val packageName = event.packageName.blankOrThis()
            val eventType = event.eventType
            if (className.isNotBlank() && packageName.isNotBlank())
                analyzeSource(EventWrapper(packageName, className, eventType), ::analyzeCallBack)
        }
    }

    /**
     * 解析当前页面结点
     *
     * @param wrapper Event包装类
     * @param
     * */
    fun analyzeSource(wrapper: EventWrapper? = null, callback: ((EventWrapper?, AnalyzeSourceResult) -> Unit)? = null) {
        executor.execute {
            Thread.sleep(100)   // 休眠200毫秒避免获取到错误的source
            currentEventWrapper = wrapper
            // 遍历解析获得结点列表
            val analyzeSourceResult = AnalyzeSourceResult(arrayListOf())
            analyzeNode(rootInActiveWindow, analyzeSourceResult.nodes)
            callback?.invoke(currentEventWrapper, analyzeSourceResult)
        }
    }

    /**
     * 递归遍历结点的方法
     * */
    private fun analyzeNode(node: AccessibilityNodeInfo?, list: ArrayList<NodeWrapper>) {
        if (node == null) return
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        list.add(
            NodeWrapper(
                text = node.text.blankOrThis(),
                id = node.viewIdResourceName.blankOrThis(),
                bounds = bounds,
                className = node.className.blankOrThis(),
                description = node.contentDescription.blankOrThis(),
                clickable = node.isClickable,
                scrollable = node.isScrollable,
                editable = node.isEditable,
                nodeInfo = node
            )
        )
        if (node.childCount > 0) {
            for (index in 0 until node.childCount) analyzeNode(node.getChild(index), list)
        }
    }

    // 监听Event的自定义回调，可按需重写
    open fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {}

    override fun onInterrupt() {}

    // 重写获取当前页面节点信息，异常的话返回Null
    override fun getRootInActiveWindow() = try {
        super.getRootInActiveWindow()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }

}