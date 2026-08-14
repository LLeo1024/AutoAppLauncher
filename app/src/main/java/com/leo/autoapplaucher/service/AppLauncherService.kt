package com.leo.autoapplaucher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.leo.autoapplaucher.MainActivity
import com.leo.autoapplaucher.R
import com.leo.autoapplaucher.data.AppDatabase
import com.leo.autoapplaucher.data.ExecutionLogEntity
import com.leo.autoapplaucher.ui.LaunchBridgeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * App 拉起前台服务
 *
 * 工作流程（熄屏适配版）：
 * 1. AlarmReceiver 启动此服务（已持有 PARTIAL_WAKE_LOCK）
 * 2. 创建高优先级前台通知 + setFullScreenIntent（系统会自动点亮屏幕）
 * 3. 获取 SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP（兜底点亮屏幕）
 * 4. 轮询 PowerManager.isInteractive() 等待屏幕真正点亮（最多 5 秒）
 * 5. 启动 LaunchBridgeActivity（透明 Activity，具有 showWhenLocked + turnScreenOn）
 * 6. BridgeActivity 拉起目标 App 并 finish 自身
 * 7. 记录执行日志
 * 8. 1 秒后停止服务（确保 BridgeActivity 有时间完成）
 *
 * 为什么不直接从 Service startActivity？
 * - MIUI 在熄屏时即使前台 Service 也可能拦截后台 startActivity
 * - 通过透明 BridgeActivity 桥接，利用 Activity 的 setShowWhenLocked/turnScreenOn
 * - 这是在 MIUI 上最可靠的熄屏拉起方案
 */
class AppLauncherService : Service() {

    companion object {
        private const val TAG = "AppLauncherService"
        private const val CHANNEL_ID = "app_launcher_channel"
        private const val CHANNEL_ID_HIGH = "app_launcher_high_priority"
        private const val NOTIFICATION_ID = 10001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val targetPackage = intent?.getStringExtra("target_package")
        val targetAppName = intent?.getStringExtra("target_app_name") ?: "未知应用"
        val taskId = intent?.getLongExtra("task_id", -1) ?: -1L

        if (targetPackage == null) {
            Log.e(TAG, "目标包名为空，停止服务")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.i(TAG, "开始拉起目标App: $targetAppName ($targetPackage)")

        // 1. 创建通知渠道并启动前台服务
        createNotificationChannels()
        val notification = createFullScreenNotification(targetAppName, targetPackage)
        startForeground(NOTIFICATION_ID, notification)

        // 2. 点亮屏幕（SCREEN_BRIGHT_WAKE_LOCK + ACQUIRE_CAUSES_WAKEUP）
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val screenWakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AutoAppLauncher::WakeScreen"
        )
        try {
            screenWakeLock.acquire(10_000)
            Log.i(TAG, "屏幕 WakeLock 已获取，屏幕正在点亮")
        } catch (e: Exception) {
            Log.w(TAG, "获取屏幕 WakeLock 失败: ${e.message}")
        }

        // 3. 等待屏幕真正点亮后再启动桥接 Activity。
        //    熄屏状态下屏幕点亮需要 1~3 秒，固定 500ms 不够，
        //    若屏幕未亮就启动 Activity，MIUI 会判定为后台启动并拦截。
        //    轮询 PowerManager.isInteractive()，最多等 5 秒。
        val waitScreenStart = SystemClock.uptimeMillis()
        val launchRunnable = object : Runnable {
            override fun run() {
                val screenOn = powerManager.isInteractive
                val timedOut = SystemClock.uptimeMillis() - waitScreenStart > 5_000
                if (!screenOn && !timedOut) {
                    Handler(Looper.getMainLooper()).postDelayed(this, 200)
                    return
                }
                if (!screenOn) {
                    Log.w(TAG, "等待屏幕点亮超时(5s)，强制启动桥接 Activity")
                } else {
                    Log.i(TAG, "屏幕已点亮，启动桥接 Activity")
                }
                launchViaBridgeActivity(targetPackage, targetAppName, taskId, screenWakeLock)
            }
        }
        Handler(Looper.getMainLooper()).postDelayed(launchRunnable, 300)

        return START_NOT_STICKY
    }

    /**
     * 通过桥接 Activity 拉起目标 App
     */
    private fun launchViaBridgeActivity(
        targetPackage: String,
        targetAppName: String,
        taskId: Long,
        screenWakeLock: PowerManager.WakeLock
    ) {
        var result = "success"
        var errorMsg: String? = null

        try {
            val bridgeIntent = Intent(this, LaunchBridgeActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                putExtra(LaunchBridgeActivity.EXTRA_TARGET_PACKAGE, targetPackage)
                putExtra(LaunchBridgeActivity.EXTRA_TARGET_APP_NAME, targetAppName)
            }
            startActivity(bridgeIntent)
            Log.i(TAG, "桥接 Activity 已启动，等待拉起: $targetAppName")
        } catch (e: Exception) {
            result = "failed"
            errorMsg = e.message
            Log.e(TAG, "启动桥接 Activity 失败: ${e.message}", e)
        }

        // 记录执行日志（后台线程）
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (taskId != -1L) {
                    val db = AppDatabase.getDatabase(this@AppLauncherService)
                    db.executionLogDao().insert(
                        ExecutionLogEntity(
                            taskId = taskId,
                            targetPackage = targetPackage,
                            result = result,
                            errorMessage = errorMsg
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "记录日志失败: ${e.message}")
            } finally {
                // 释放屏幕 WakeLock
                if (screenWakeLock.isHeld) {
                    screenWakeLock.release()
                }
            }
        }

        // 1 秒后停止服务，确保 BridgeActivity 有时间完成拉起
        Handler(Looper.getMainLooper()).postDelayed({
            stopSelf()
        }, 1000)
    }

    /**
     * 创建通知渠道（普通 + 高优先级）
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // 普通渠道（常驻通知）
            val normalChannel = NotificationChannel(
                CHANNEL_ID,
                "定时任务执行",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "定时拉起App时显示的通知"
                setShowBadge(false)
            }

            // 高优先级渠道（全屏 Intent 用）
            val highChannel = NotificationChannel(
                CHANNEL_ID_HIGH,
                "定时任务提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "定时任务触发时的高优先级提醒，用于点亮屏幕"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            manager.createNotificationChannel(normalChannel)
            manager.createNotificationChannel(highChannel)
        }
    }

    /**
     * 创建带全屏 Intent 的高优先级通知
     * setFullScreenIntent 在屏幕熄灭时会自动点亮屏幕并显示全屏通知
     */
    private fun createFullScreenNotification(targetAppName: String, targetPackage: String): Notification {
        // 全屏 Intent 指向桥接 Activity，点击或屏幕点亮时自动启动
        val fullScreenIntent = Intent(this, LaunchBridgeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(LaunchBridgeActivity.EXTRA_TARGET_PACKAGE, targetPackage)
            putExtra(LaunchBridgeActivity.EXTRA_TARGET_APP_NAME, targetAppName)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            targetPackage.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 点击通知的 Intent（打开本 App 主界面）
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_HIGH)
            .setContentTitle("定时任务触发")
            .setContentText("正在打开 $targetAppName ...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}
