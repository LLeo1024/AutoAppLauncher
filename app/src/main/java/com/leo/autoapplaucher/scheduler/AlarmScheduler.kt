package com.leo.autoapplaucher.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.leo.autoapplaucher.data.TaskEntity
import java.util.Calendar
import java.util.Random

/**
 * 闹钟调度器
 * 负责向 AlarmManager 注册/取消精确闹钟
 *
 * 支持两种调度模式：
 * 1. 固定时间 — 使用 task.hour/minute
 * 2. 随机时间段 — 在 [timeRangeStart, timeRangeEnd] 内随机选一个时间点
 *    每次调用 schedule() 都会重新随机，所以每天触发时间不同
 *
 * MIUI 适配要点：
 * - 使用 setExactAndAllowWhileIdle 确保 Doze 模式下也能触发
 * - Android 11 不需要 SCHEDULE_EXACT_ALARM 权限（12+才需要）
 * - 但仍需用户关闭电池优化才能保证可靠触发
 */
class AlarmScheduler(private val context: Context) {

    companion object {
        private const val TAG = "AlarmScheduler"
        private const val REQUEST_CODE_BASE = 10000

        /**
         * 为单个任务创建唯一的 PendingIntent
         */
        private fun createPendingIntent(context: Context, task: TaskEntity): PendingIntent {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.leo.autoapplaucher.ALARM_FIRE"
                putExtra("task_id", task.id)
                putExtra("target_package", task.targetPackage)
                putExtra("target_app_name", task.targetAppName)
            }
            return PendingIntent.getBroadcast(
                context,
                (REQUEST_CODE_BASE + task.id).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    /**
     * 注册一个定时任务闹钟
     * 如果 useRandomTime=true，在时间段内随机选一个时间点
     */
    fun schedule(task: TaskEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, task)

        // 计算触发时间
        val (triggerHour, triggerMinute) = if (task.useRandomTime) {
            pickRandomTimeInRange(task)
        } else {
            Pair(task.hour, task.minute)
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, triggerHour)
            set(Calendar.MINUTE, triggerMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // 如果设定时间已过，设为明天
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val triggerAtMillis = calendar.timeInMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // setExactAndAllowWhileIdle: 即使设备处于 Doze 模式也能精确触发
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }

            val timeStr = String.format("%02d:%02d", triggerHour, triggerMinute)
            val modeStr = if (task.useRandomTime) "随机" else "固定"
            Log.i(TAG, "已注册闹钟[$modeStr]: ${task.targetAppName} at $timeStr, " +
                    "触发时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA)
                        .format(java.util.Date(triggerAtMillis))}")
        } catch (e: SecurityException) {
            Log.e(TAG, "注册闹钟失败 (权限不足): ${e.message}")
            // Android 12+ 可能需要用户手动授予 SCHEDULE_EXACT_ALARM 权限
        }
    }

    /**
     * 在时间段内随机选择一个时间点
     * 返回 (hour, minute)
     */
    private fun pickRandomTimeInRange(task: TaskEntity): Pair<Int, Int> {
        val startTotalMinutes = task.timeRangeStartHour * 60 + task.timeRangeStartMinute
        val endTotalMinutes = task.timeRangeEndHour * 60 + task.timeRangeEndMinute

        // 确保结束时间大于开始时间
        val rangeMinutes = (endTotalMinutes - startTotalMinutes).coerceAtLeast(1)

        val random = Random()
        val randomOffset = random.nextInt(rangeMinutes + 1) // 包含两端

        val resultTotalMinutes = startTotalMinutes + randomOffset
        val hour = resultTotalMinutes / 60
        val minute = resultTotalMinutes % 60

        Log.i(TAG, "随机时间选择: ${task.timeRangeStartHour}:${task.timeRangeStartMinute}" +
                " ~ ${task.timeRangeEndHour}:${task.timeRangeEndMinute}" +
                " → 选中 $hour:$minute")

        return Pair(hour, minute)
    }

    /**
     * 取消一个定时任务闹钟
     */
    fun cancel(task: TaskEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, task)
        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "已取消闹钟: ${task.targetAppName} at ${task.timeString}")
    }

    /**
     * 重新注册所有启用的任务闹钟
     * 用于开机后恢复、或应用更新后恢复
     */
    fun rescheduleAll(tasks: List<TaskEntity>) {
        tasks.filter { it.enabled }.forEach { schedule(it) }
        Log.i(TAG, "已重新注册 ${tasks.count { it.enabled }} 个闹钟")
    }
}
