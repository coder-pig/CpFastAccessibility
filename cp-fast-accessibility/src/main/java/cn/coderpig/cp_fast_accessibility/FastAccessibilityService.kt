package cn.coderpig.cp_fast_accessibility

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random
import cn.coderpig.cp_fast_accessibility.R

/**
 * Author: CoderPig
 * Date: 2023-03-24
 * Desc: 无障碍服务基类
 */
abstract class FastAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "FastAccessibilityService"
        var instance: FastAccessibilityService? = null  // 无障碍服务对象实例，暴露给外部调用
        val isServiceEnable: Boolean get() = instance != null   // 无障碍服务是否可用
        private var _appContext: Context? = null    // 幕后属性，对外表现为只读，对内表现为可读写
        val appContext get() = _appContext ?: throw NullPointerException("需要在Application的onCreate()中调用init()")
        lateinit var specificServiceClass: Class<*> // 具体无障碍服务实现类的类类型
        private var listenEventTypeList = arrayListOf(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) // 监听的event类型列表
        val currentEvent get() = instance?.currentEventWrapper  // 获取当前Event
        var enableListenApp: Boolean = true    // 是否监听APP的标记，默认监听

        var foregroundNotification: Notification? = null    // 前台服务，用于保活
        var channelId = Random.nextInt(1, 1001)    // 渠道id
        var channelName = "FastAccessibilityService"    // 渠道名称

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

        /**
         * 显示前台服务
         *
         * @param title 通知标题
         * @param content 通知内容
         * @param content 通知提示语
         * @param iconRes 通知小图标
         * */
        @RequiresApi(Build.VERSION_CODES.O)
        fun showForegroundNotification(
            title: String = "通知标题",
            content: String = "通知内容",
            ticker: String = "通知提示语",
            iconRes: Int? = null,
            activityClass: Class<*>? = null
        ) {
            // 有前台服务权限直接创建并开始
            val notificationManager = instance?.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            // 创建通知渠道，一定要写在创建显示通知之前，创建通知渠道的代码只有在第一次执行才会创建
            // 以后每次执行创建代码检测到该渠道已存在，因此不会重复创建
            notificationManager?.createNotificationChannel(
                NotificationChannel("$channelId", channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                    // 下述都是非必要的，看自己需求配置
                    enableLights(true)  // 如果设备有指示灯，开启指示灯
                    lightColor = Color.GREEN    // 设置指示灯颜色
                    enableVibration(true)   // 开启震动
                    vibrationPattern = longArrayOf(100, 200, 300, 400)  // 设置震动频率
                    setShowBadge(true)  // 是否显示角标
                    setBypassDnd(true)  // 是否绕过免打扰模式
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE  // 是否在锁屏屏幕上显示此频道的通知
                }
            )
            foregroundNotification = NotificationCompat.Builder(appContext, "$channelId").apply {
                setSmallIcon(iconRes ?: R.drawable.ic_default_foreground_notification) // 设置小图标
                setContentTitle(title)
                setContentText(content)
                setTicker(ticker)
                activityClass?.let {
                    setContentIntent(
                        PendingIntent.getActivity(
                            instance!!, 0, Intent(instance, it),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }.build()
            instance?.startForeground(channelId, foregroundNotification!!)
        }

        /**
         * 关闭前台服务
         * */
        fun closeForegroundNotification() {
            foregroundNotification?.let { instance?.stopForeground(true) }
        }
    }

    var currentEventWrapper: EventWrapper? = null   // 当前Event
        private set
    var executor: ExecutorService = Executors.newFixedThreadPool(4) // 执行任务的线程池
    var windowService: WindowManager? = null
    var floatWindow: View? = null

    override fun onServiceConnected() {
        if (this::class.java == specificServiceClass) instance = this
    }

    override fun onDestroy() {
        if (this::class.java == specificServiceClass) instance = null
        closeForegroundNotification()
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
                analyzeSource(EventWrapper(packageName, className, eventType), callback = ::analyzeCallBack)
        }
    }

    /**
     * 解析当前页面结点
     *
     * @param wrapper Event包装类
     * @param waitTime 延迟获取结点Source的时间
     * @param callback 处理结点信息的回调
     * */
    fun analyzeSource(
        wrapper: EventWrapper? = null,
        waitTime: Long = 100,
        callback: ((EventWrapper?, AnalyzeSourceResult) -> Unit)? = null
    ) {
        executor.execute {
            Thread.sleep(waitTime)   // 休眠200毫秒避免获取到错误的source
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

    /**
     * 展示悬浮框的方法，可按需重写
     * */
    open fun showFloatWindow() {
        if (windowService == null) windowService = getSystemService(WINDOW_SERVICE) as? WindowManager
        if (floatWindow == null) {
            val lp = WindowManager.LayoutParams().apply {
                width = 200
                height = 200
                type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY // 因为此权限才能展示处理
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                flags = FLAG_NOT_TOUCH_MODAL
                format = PixelFormat.TRANSLUCENT
            }
            // 通过 LayoutInflater 创建 View

            val rootView = LayoutInflater.from(this).inflate(R.layout.fly_float_layer, null)
            rootView.findViewById<TextView>(R.id.tv_test).setOnClickListener {
                (getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager).sendAccessibilityEvent(AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED))
                val analyzeSourceResult = AnalyzeSourceResult(arrayListOf())
                analyzeNode(windows.first { it.title == "安全考试" }.root, analyzeSourceResult.nodes)
                analyzeSourceResult.findNodesByExpression {
                     it.className == "android.widget.TextView" &&  it.text!!.matches(Regex("^[0-9]+、.*"))
                }.nodes.forEach { logD("$currentEventWrapper | $it ") }
            }
            rootView.findViewById<RelativeLayout>(R.id.rly_root).apply {
                var startX = 0.0f
                var startY = 0.0f
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // 获取触摸点在屏幕上的坐标
                            startX = event.rawX
                            startY = event.rawY
                            false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - startX
                            val dy = event.rawY - startY
                            startX = event.rawX
                            startY = event.rawY
                            lp.x += dx.toInt()
                            lp.y += dy.toInt()
                            windowService?.updateViewLayout(this, lp)
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            performClick()
                            true
                        }
                        else -> false
                    }
                }
            }
            windowService?.addView(rootView, lp)
        }
    }

    open fun closeFloatWindow() {
        if (windowService == null) windowService = getSystemService(WINDOW_SERVICE) as? WindowManager
        if (floatWindow != null) {
            windowService!!.removeView(floatWindow)
            floatWindow = null
        }
    }


}