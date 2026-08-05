package com.leo.autoapplaucher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.leo.autoapplaucher.data.AppDatabase
import com.leo.autoapplaucher.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 开机自启接收器
 *
 * 设备重启后，所有通过 AlarmManager 注册的闹钟都会丢失。
 * 此接收器在开机完成后重新注册所有启用的任务闹钟。
 *
 * MIUI 适配要点：
 * - 必须在 MIUI 设置中开启"自启动"权限，否则此接收器不会被调用
 * - 即使有自启动权限，MIUI 可能会延迟几分钟后才触发 BOOT_COMPLETED
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                Log.i(TAG, "收到开机广播，开始恢复闹钟任务")

                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    val tasks = db.taskDao().getAllEnabledTasks()
                    val scheduler = AlarmScheduler(context)
                    scheduler.rescheduleAll(tasks)
                    Log.i(TAG, "开机恢复完成，已注册 ${tasks.size} 个闹钟")
                }
            }
        }
    }
}
