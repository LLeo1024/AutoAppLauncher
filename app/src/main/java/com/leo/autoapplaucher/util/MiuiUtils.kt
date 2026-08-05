package com.leo.autoapplaucher.util

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * MIUI 适配工具类
 *
 * 小米手机的 MIUI 系统对后台执行有额外的限制，
 * 即使 Android 标准权限都通过了，MIUI 专属权限没开也一样不工作。
 *
 * 4项关键 MIUI 权限：
 * 1. 自启动 — 重启后 App 能否自动恢复闹钟
 * 2. 省电策略:无限制 — 待机时 App 不被冻结
 * 3. 后台弹出界面 — 后台能否拉起其他 App
 * 4. 锁屏不清理 — 锁屏后 App 是否被杀
 *
 * 注意：MIUI 的 Intent action 没有官方文档，不同版本路径可能不同。
 * 每个 Intent 都用 try-catch，失败后降级到通用设置页。
 */
object MiuiUtils {

    private const val TAG = "MiuiUtils"

    /**
     * 检测当前设备是否为 MIUI 系统
     */
    fun isMiui(): Boolean {
        return try {
            val prop = System.getProperty("ro.miui.ui.version.name") ?: ""
            prop.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取 MIUI 版本号（如 "12.5"）
     */
    fun getMiuiVersion(): String {
        return try {
            System.getProperty("ro.miui.ui.version.name") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // ==================== MIUI 专属权限跳转 ====================

    /**
     * 跳转到 MIUI 自启动管理页面
     * 设置 → 应用设置 → 应用管理 → [本App] → 自启动
     */
    fun openAutoStartSettings(context: Context): Boolean {
        val intents = listOf(
            // MIUI 12+ 自启动管理（安全中心）
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            },
            // 备选路径
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
            },
            // MIUI 旧版
            Intent("miui.intent.action.APP_AUTO_START")
        )

        return tryStartActivity(context, intents, "自启动管理")
    }

    /**
     * 跳转到 MIUI 省电策略页面
     * 设置 → 省电与电池 → 应用智能省电 → [本App] → 无限制
     */
    fun openBatterySaverSettings(context: Context): Boolean {
        val intents = listOf(
            Intent().apply {
                component = ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
            },
            Intent().apply {
                component = ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity"
                )
            },
            Intent("miui.intent.action.POWER_USAGE")
        )

        return tryStartActivity(context, intents, "省电策略")
    }

    /**
     * 跳转到后台弹出界面权限页面
     * 设置 → 应用设置 → 应用管理 → [本App] → 后台弹出界面
     */
    fun openBackgroundPopupSettings(context: Context): Boolean {
        val intents = listOf(
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("permission.name", "backgroundPopup")
            },
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
                )
            }
        )

        return tryStartActivity(context, intents, "后台弹出界面")
    }

    /**
     * 跳转到应用详情页（通用 fallback）
     * 用户可以在此页面手动管理所有权限
     */
    fun openAppDetailSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return tryStartActivity(context, listOf(intent), "应用详情")
    }

    // ==================== 标准 Android 权限 ====================

    /**
     * 检查是否已忽略电池优化
     * Android 标准权限，MIUI 也有自己的省电策略，两者都需要处理
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * 请求忽略电池优化
     */
    fun requestIgnoreBatteryOptimization(activity: Activity) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
    }

    /**
     * 跳转到精确闹钟权限页面（Android 12+）
     * Android 11 上不需要
     */
    fun openExactAlarmSettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            tryStartActivity(context, listOf(intent), "精确闹钟权限")
        } else {
            true // Android 11 不需要此权限
        }
    }

    /**
     * 检查通知权限（Android 13+）
     * 前台服务需要通知权限才能显示通知
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // ==================== 权限状态汇总 ====================

    /**
     * 获取当前所有权限的状态
     * 用于在 UI 上展示哪些权限还没开
     */
    data class PermissionStatus(
        val batteryOptimization: Boolean,
        val exactAlarm: Boolean,
        val notification: Boolean,
        val isMiui: Boolean,
        val miuiVersion: String
    )

    fun getPermissionStatus(context: Context): PermissionStatus {
        return PermissionStatus(
            batteryOptimization = isBatteryOptimizationIgnored(context),
            exactAlarm = true, // Android 11 上始终为 true
            notification = hasNotificationPermission(context),
            isMiui = isMiui(),
            miuiVersion = getMiuiVersion()
        )
    }

    // ==================== 内部工具 ====================

    /**
     * 尝试用多个 Intent 打开设置页面，第一个成功的就返回
     * 全部失败则打开应用详情页作为 fallback
     */
    private fun tryStartActivity(
        context: Context,
        intents: List<Intent>,
        label: String
    ): Boolean {
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.i(TAG, "成功打开: $label")
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Intent 失败 ($label): ${e.message}")
            }
        }

        // 所有 MIUI Intent 都失败，降级到应用详情页
        Log.w(TAG, "$label: 所有 MIUI Intent 失败，降级到应用详情页")
        return openAppDetailSettings(context)
    }
}
