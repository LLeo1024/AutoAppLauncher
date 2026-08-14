package com.leo.autoapplaucher.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager

/**
 * 透明桥接 Activity
 *
 * 作用：解决熄屏状态下 MIUI 阻止后台 Service 直接 startActivity 的问题。
 *
 * 原理：
 * 1. AlarmManager 触发 → AlarmReceiver → AppLauncherService
 * 2. Service 点亮屏幕（WakeLock）后启动此 Activity
 * 3. 此 Activity 具有 showWhenLocked + turnScreenOn，能在锁屏/熄屏上显示
 * 4. 此 Activity 立即拉起目标 App 的 LaunchIntent
 * 5. finish() 自身，用户看到的就是目标 App
 *
 * 为什么不直接从 Service startActivity？
 * - Android 10+ 限制后台启动 Activity
 * - MIUI 在熄屏时即使前台 Service 也可能被拦截
 * - 通过 Activity 桥接 + setShowWhenLocked/turnScreenOn 是最可靠的方式
 */
class LaunchBridgeActivity : Activity() {

    companion object {
        private const val TAG = "LaunchBridgeActivity"
        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_TARGET_APP_NAME = "target_app_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 关键：允许在锁屏上方显示 + 自动点亮屏幕
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // 额外的 Window 标志，双重保障
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        val targetAppName = intent.getStringExtra(EXTRA_TARGET_APP_NAME) ?: "未知应用"

        Log.i(TAG, "桥接 Activity 启动，目标: $targetAppName ($targetPackage)")

        if (targetPackage.isNullOrEmpty()) {
            Log.e(TAG, "目标包名为空，直接关闭")
            finish()
            return
        }

        // 额外点亮屏幕（兜底，某些 MIUI 版本 setShowWhenLocked 不够）
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "AutoAppLauncher::BridgeActivity"
            )
            wakeLock.acquire(5000)
            // 延迟 300ms 等待屏幕完全点亮后再启动目标 App
            Handler(Looper.getMainLooper()).postDelayed({
                launchTargetApp(targetPackage, targetAppName, wakeLock)
            }, 300)
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock 获取失败，直接尝试启动: ${e.message}")
            launchTargetApp(targetPackage, targetAppName, null)
        }
    }

    private fun launchTargetApp(
        targetPackage: String,
        targetAppName: String,
        wakeLock: PowerManager.WakeLock?
    ) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
                startActivity(launchIntent)
                Log.i(TAG, "成功拉起目标 App: $targetAppName")
            } else {
                Log.e(TAG, "无法获取 $targetAppName 的启动 Intent")
                // 打开目标 App 的应用信息页作为 fallback
                val detailIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:$targetPackage")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(detailIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "拉起目标 App 失败: ${e.message}", e)
        } finally {
            wakeLock?.takeIf { it.isHeld }?.release()
            finish()
        }
    }

    override fun onBackPressed() {
        // 禁止后退，防止用户误操作
    }
}
