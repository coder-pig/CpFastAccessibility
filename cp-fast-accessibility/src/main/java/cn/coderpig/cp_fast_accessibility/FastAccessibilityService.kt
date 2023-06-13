package cn.coderpig.cp_fast_accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.security.KeyChainAliasCallback
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random


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
        private var mListenEventTypeList = arrayListOf(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) // 监听的event类型列表
        private var mListenPackageList = arrayListOf<String>()  // 要监听的event的包名列表，不传默认监听所有应用的包名
        var enableListenApp: Boolean = true    // 是否监听APP的标记，默认监听
        /**
         * 库初始化方法，必须在Application的OnCreate()中调用
         *
         * @param context Context上下文
         * @param clazz 无障碍服务的类类型
         * @param typeList 监听的事件类型列表，不传默认只监听TYPE_WINDOW_STATE_CHANGED类型
         * @param packageList 要监听的应用包名
         * */
        fun init(
            context: Context,
            clazz: Class<*>,
            typeList: ArrayList<Int>? = null,
            packageList: ArrayList<String>? = null
        ) {
            _appContext = context.applicationContext
            specificServiceClass = clazz
            typeList?.let { mListenEventTypeList = it }
            packageList?.let { mListenPackageList = it }
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
        val require get() = run { requireAccessibility(); instance }
    }

    private var executor: ExecutorService = Executors.newFixedThreadPool(4) // 执行任务的线程池


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
        if (mListenEventTypeList.isNotEmpty()) {
            if (event.eventType in mListenEventTypeList) {
                val className = event.className.blankOrThis()
                val packageName = event.packageName.blankOrThis()
                val eventType = event.eventType
                // 监听列表为空，或者要监听的package列表里有此package的event才分发
                if (mListenPackageList.isEmpty() || (mListenPackageList.isNotEmpty() && packageName in mListenPackageList)) {
                    if (className.isNotBlank() && packageName.isNotBlank())
                        analyzeSource(EventWrapper(packageName, className, eventType),
                            noAnalyzeCallback = ::noAnalyzeCallBack,
                            analyzeCallback = ::analyzeCallBack)
                }
            }
        }
    }

    /**
     * 解析当前页面结点
     *
     * @param wrapper Event包装类
     * @param waitTime 延迟获取结点Source的时间
     * @param getWindow 通过getWindows()获得窗口，然后过滤返回所需窗口的方法
     * @param callback 处理结点信息的回调
     * */
    fun analyzeSource(
        wrapper: EventWrapper? = null,
        waitTime: Long = 100,
        getWindow: ((List<AccessibilityWindowInfo>) -> AccessibilityWindowInfo)? = null,
        noAnalyzeCallback: ((EventWrapper?, AccessibilityNodeInfo?) -> Unit)? = null,
        analyzeCallback: ((EventWrapper?, AnalyzeSourceResult) -> Unit)? = null
    ) {
        executor.execute {
            Thread.sleep(waitTime)   // 休眠100毫秒避免获取到错误的source
            // 遍历解析获得结点列表
            AnalyzeSourceResult(arrayListOf()).let { result ->
                getWindow?.invoke(windows)?.root ?: rootInActiveWindow?.let {
                    analyzeNode(it, 0, result.nodes)
                    noAnalyzeCallback?.invoke(wrapper, it)
                    analyzeCallback?.invoke(wrapper, result)
                }
            }
        }
    }


    /**
     * 递归遍历结点的方法
     * */
    private fun analyzeNode(node: AccessibilityNodeInfo?, nodeIndex: Int, list: ArrayList<NodeWrapper>) {
        if (node == null) return
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        list.add(
            NodeWrapper(
                index = nodeIndex,
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
            for (index in 0 until node.childCount) analyzeNode(node.getChild(index), index, list)
        }
    }



    // 监听Event的自定义回调，可按需重写
    open fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {}

    // 不解析结点的自定义回调，可按需重写
    open fun noAnalyzeCallBack(wrapper: EventWrapper?, node: AccessibilityNodeInfo?) { }

    override fun onInterrupt() {}

    // 重写获取当前页面节点信息，异常的话返回Null
    override fun getRootInActiveWindow() = try {
        super.getRootInActiveWindow()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }


}