package cn.coderpig.cp_fast_accessibility

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlin.random.Random

/**
 * Author: zpj
 * Date: 2023-06-12 14:16
 * Desc:
 */
class FastNotification {
    companion object {
        var foregroundNotification: Notification? = null    // 前台服务，用于保活
        var channelId = Random.nextInt(1, 1001)    // 渠道id
        var channelName = "FastAccessibilityService"    // 渠道名称

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
            val notificationManager = FastAccessibilityService.instance?.getSystemService(AccessibilityService.NOTIFICATION_SERVICE) as? NotificationManager
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
            foregroundNotification = NotificationCompat.Builder(FastAccessibilityService.appContext, "$channelId").apply {
                setSmallIcon(iconRes ?: R.drawable.ic_default_foreground_notification) // 设置小图标
                setContentTitle(title)
                setContentText(content)
                setTicker(ticker)
                activityClass?.let {
                    setContentIntent(
                        PendingIntent.getActivity(
                            FastAccessibilityService.instance!!, 0, Intent(FastAccessibilityService.instance, it),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }.build()
            FastAccessibilityService.instance?.startForeground(channelId, foregroundNotification!!)
        }

        /**
         * 关闭前台服务
         * */
        fun closeForegroundNotification() {
            foregroundNotification?.let { FastAccessibilityService.instance?.stopForeground(true) }
        }
    }
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
) = FastNotification.showForegroundNotification(title, content, ticker, iconRes, activityClass)

/**
 * 隐藏前台服务
 * */
fun closeForegroundNotification() = FastNotification.closeForegroundNotification()
