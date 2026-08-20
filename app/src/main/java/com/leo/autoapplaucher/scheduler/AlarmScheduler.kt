package com.leo.autoapplaucher.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.leo.autoapplaucher.data.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random

/**
 * 闹钟调度器
 * 负责向 AlarmManager 注册/取消精确闹钟
 *
 * 支持两种调度模式：
 * 1. 固定时间 — 使用 task.hour/minute
 * 2. 随机时间段 — 在 [timeRangeStart, timeRangeEnd] 内随机选一个时间点（分钟级）
 *
 * 调度规则：
 * - schedule()      首次调度：随机时间段若今天区间未过完，则今天随机；否则明天随机
 * - scheduleNextDay() 触发后的下一次调度：固定选下一天的同一时间段重新随机，
 *   保证每天触发时间都不同（不重复触发当天）
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
     * 注册一个定时任务闹钟（首次调度/重新启用时）
     *
     * 固定时间：今天未过则今天触发，已过则明天
     * 随机时间段：今天区间未过完 → 在剩余窗口内随机（保证今天触发）；
     *            今天已过完 → 明天在完整区间内随机
     */
    fun schedule(task: TaskEntity) {
        registerAlarm(task, computeFirstTrigger(task))
    }

    /**
     * 注册下一次闹钟（触发后重新调度）
     *
     * 固定时间：下一天同一时间
     * 随机时间段：下一天同一时间段内重新随机（分钟级）
     * 每周模式(2)：跳到下一个匹配的星期（可能不止一天后）
     *
     * 强制设为"下一个应执行日"，避免"今天随机到更晚时间导致当天重复触发"的 bug，
     * 保证每天在设定区间内触发一次，且每天时间都不同
     */
    fun scheduleNextDay(task: TaskEntity) {
        registerAlarm(task, computeNextTrigger(task))
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

    // ===== 私有辅助方法 =====

    /**
     * 计算首次触发的绝对时间（毫秒）
     *
     * 每周模式(2)：只有匹配的星期才会注册闹钟，非匹配星期直接跳过
     * 固定时间：今天匹配且未过则今天触发，已过则下一个匹配日
     * 随机时间段：今天匹配且区间未过完 → 在剩余窗口内随机（保证今天触发）；
     *            今天不匹配或已过完 → 下一个匹配日在完整区间内随机
     */
    private fun computeFirstTrigger(task: TaskEntity): Long {
        val now = Calendar.getInstance()
        val nowTotal = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val todayMatches = todayMatchesWeekday(task, now)

        if (task.useRandomTime) {
            val startTotal = task.timeRangeStartHour * 60 + task.timeRangeStartMinute
            val endTotal = task.timeRangeEndHour * 60 + task.timeRangeEndMinute

            if (todayMatches && endTotal > nowTotal) {
                // 今天是匹配星期且时间段还没过完：在剩余窗口内随机（保证今天触发一次）
                val from = maxOf(startTotal, nowTotal + 1)
                if (from <= endTotal) {
                    val (h, m) = pickRandomTimeBetween(from, endTotal)
                    return Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, h)
                        set(Calendar.MINUTE, m)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }
            }
            // 今天不匹配或区间已过完：下一个匹配日随机
            return computeNextTrigger(task)
        }

        // 固定时间：今天匹配且未过则今天，否则从下一个匹配日开始找
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, task.hour)
            set(Calendar.MINUTE, task.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!todayMatches || cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            skipToNextMatchingWeekday(cal, task)
        }
        return cal.timeInMillis
    }

    /**
     * 计算下一次应触发的日期（随机时间段重新随机）
     *
     * 默认从明天开始；每周模式(2)会跳过所有不匹配的星期，
     * 直到落在下一个匹配的星期上
     */
    private fun computeNextTrigger(task: TaskEntity): Long {
        val (h, m) = if (task.useRandomTime) {
            pickRandomTimeInRange(task)
        } else {
            Pair(task.hour, task.minute)
        }
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            skipToNextMatchingWeekday(this, task)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * 判断给定日期是否匹配任务的星期选择（仅每周模式(2)生效，其余恒为 true）
     *
     * Calendar.DAY_OF_WEEK: 1=周日 ... 7=周六
     * weekDays 位掩码: bit0=周日 ... bit6=周六，需减 1 对齐
     */
    private fun todayMatchesWeekday(task: TaskEntity, cal: Calendar): Boolean {
        if (task.repeatMode != 2) return true
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        return ((task.weekDays shr (dayOfWeek - 1)) and 1) == 1
    }

    /**
     * 从当前日期开始向后跳，直到落在匹配的星期上（仅每周模式(2)生效）
     * weekDays==0（异常数据）时最多跳 8 天兜底，由 Receiver 层过滤
     */
    private fun skipToNextMatchingWeekday(cal: Calendar, task: TaskEntity) {
        if (task.repeatMode != 2) return
        var guard = 0
        while (guard < 8 && !todayMatchesWeekday(task, cal)) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            guard++
        }
    }

    /**
     * 在任务的时间段内随机选择一个时间点（分钟级，含两端）
     * 返回 (hour, minute)
     */
    private fun pickRandomTimeInRange(task: TaskEntity): Pair<Int, Int> {
        val startTotal = task.timeRangeStartHour * 60 + task.timeRangeStartMinute
        val endTotal = task.timeRangeEndHour * 60 + task.timeRangeEndMinute
        return pickRandomTimeBetween(startTotal, endTotal)
    }

    /**
     * 在 [fromTotal, toTotal] 分钟之间随机选一个时间点（分钟级，含两端）
     */
    private fun pickRandomTimeBetween(fromTotal: Int, toTotal: Int): Pair<Int, Int> {
        val rangeMinutes = (toTotal - fromTotal).coerceAtLeast(1)
        val randomOffset = Random().nextInt(rangeMinutes + 1)
        val resultTotal = fromTotal + randomOffset
        val hour = resultTotal / 60
        val minute = resultTotal % 60
        Log.i(TAG, "随机时间选择: ${formatMinute(fromTotal)} ~ ${formatMinute(toTotal)}" +
                " → 选中 ${String.format("%02d:%02d", hour, minute)}")
        return Pair(hour, minute)
    }

    /**
     * 向 AlarmManager 注册精确闹钟
     */
    private fun registerAlarm(task: TaskEntity, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, task)

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
            Log.i(TAG, "已注册闹钟: ${task.targetAppName}, 触发时间: " +
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                        .format(Date(triggerAtMillis)))
        } catch (e: SecurityException) {
            Log.e(TAG, "注册闹钟失败 (权限不足): ${e.message}")
            // Android 12+ 可能需要用户手动授予 SCHEDULE_EXACT_ALARM 权限
        }
    }

    private fun formatMinute(totalMinutes: Int): String {
        return String.format("%02d:%02d", totalMinutes / 60, totalMinutes % 60)
    }
}
