package com.leo.autoapplaucher.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.leo.autoapplaucher.data.AppDatabase
import com.leo.autoapplaucher.data.HolidayRepository
import com.leo.autoapplaucher.data.TaskEntity
import com.leo.autoapplaucher.service.AppLauncherService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 闹钟接收器
 * AlarmManager 到时间后发送广播，此类接收并启动前台服务拉起目标App
 *
 * 支持的重复模式：
 * - 0=仅一次, 1=每天, 2=每周指定日期
 * - 3=法定节假日（仅在法定节假日触发）
 * - 4=非法定节假日（仅在非法定节假日触发，含工作日+普通周末）
 * - 5=法定工作日（仅在实际工作日触发，含调休上班的周末，排除法定节假日和普通周末）
 *
 * 节假日检查流程：
 * 1. 查询 Room 缓存
 * 2. 缓存不存在则从 API 获取
 * 3. API 失败则放行执行（fail-open，避免漏触发）
 *
 * 重新调度：
 * - 无论本次执行成功与否，都会在 finally 中重新注册下一次闹钟（非一次性任务），
 *   避免异常导致第二天没有闹钟
 * - 重新调度使用 scheduleNextDay()，固定设为明天，保证随机时间段每天时间不同
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val DEFAULT_RETURN_DELAY = 120
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1)
        val targetPackage = intent.getStringExtra("target_package") ?: return
        val targetAppName = intent.getStringExtra("target_app_name") ?: return

        Log.i(TAG, "收到闹钟: $targetAppName ($targetPackage), taskId=$taskId")

        if (taskId == -1L) {
            Log.e(TAG, "无效的 taskId，忽略")
            return
        }

        // 获取 PARTIAL_WAKE_LOCK，防止 CPU 在处理过程中重新休眠
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AutoAppLauncher::AlarmReceiver")
        wakeLock.acquire(30_000)

        CoroutineScope(Dispatchers.IO).launch {
            var task: TaskEntity? = null
            try {
                val db = AppDatabase.getDatabase(context)
                task = db.taskDao().getTaskById(taskId)

                if (task == null || !task.enabled) {
                    Log.w(TAG, "任务不存在或已禁用，跳过")
                    return@launch
                }

                // 检查是否应该今天执行（每周模式星期过滤 + 节假日过滤）
                val shouldExecute = checkShouldExecuteToday(context, task)

                if (shouldExecute) {
                    Log.i(TAG, "今天应执行，启动服务拉起 $targetAppName")
                    startLauncherService(
                        context, taskId, targetPackage, targetAppName,
                        task.returnDelaySeconds
                    )
                } else {
                    Log.i(TAG, "今天不应执行（节假日过滤），跳过拉起")
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理闹钟失败: ${e.message}", e)
                // 出错时仍然尝试启动服务（fail-open）
                try {
                    startLauncherService(
                        context, taskId, targetPackage, targetAppName,
                        task?.returnDelaySeconds ?: DEFAULT_RETURN_DELAY
                    )
                } catch (e2: Exception) {
                    Log.e(TAG, "启动服务也失败: ${e2.message}")
                }
            } finally {
                // 无论成功与否，都重新注册下一次闹钟（非一次性任务）
                val currentTask = task
                if (currentTask != null && currentTask.enabled && currentTask.repeatMode != 0) {
                    try {
                        AlarmScheduler(context).scheduleNextDay(currentTask)
                    } catch (e: Exception) {
                        Log.e(TAG, "重新调度下一次闹钟失败: ${e.message}")
                    }
                }
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }

    /**
     * 检查今天是否应该执行任务
     * - 每周模式(2)：检查今天星期是否在选中范围内（shouldRunToday）
     * - 法定节假日(3)/非法定节假日(4)/法定工作日(5)：需要联网查询节假日数据
     */
    private suspend fun checkShouldExecuteToday(
        context: Context,
        task: TaskEntity
    ): Boolean {
        val repeatMode = task.repeatMode

        if (repeatMode == 2) {
            // 每周模式：检查今天星期是否匹配（bit0=周日...bit6=周六）
            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val matched = task.shouldRunToday(dayOfWeek)
            Log.i(TAG, "每周模式: 今天星期=$dayOfWeek (${weekdayName(dayOfWeek)}), " +
                    "weekDays=0b${task.weekDays.toString(2).padStart(7, '0')}, 匹配=$matched")
            return matched
        }

        if (repeatMode != 3 && repeatMode != 4 && repeatMode != 5) {
            return true // 非节假日/每周模式，总是执行
        }

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return try {
            val repository = HolidayRepository(context)
            val dayType = repository.getDayType(year, month, day)

            if (dayType == -1) {
                // 获取失败，fail-open
                Log.w(TAG, "节假日数据获取失败，放行执行")
                true
            } else if (repeatMode == 3) {
                // 法定节假日模式：仅在法定节假日(type==2)执行
                val isHoliday = dayType == 2
                Log.i(TAG, "法定节假日模式: 今天type=$dayType, isHoliday=$isHoliday")
                isHoliday
            } else if (repeatMode == 5) {
                // 法定工作日模式：仅在实际工作日(type==0)执行（含调休上班的周末）
                val isWorkday = dayType == 0
                Log.i(TAG, "法定工作日模式: 今天type=$dayType, isWorkday=$isWorkday")
                isWorkday
            } else {
                // 非法定节假日模式：仅在非法定节假日(type!=2)执行
                val isNotHoliday = dayType != 2
                Log.i(TAG, "非法定节假日模式: 今天type=$dayType, isNotHoliday=$isNotHoliday")
                isNotHoliday
            }
        } catch (e: Exception) {
            Log.e(TAG, "节假日检查异常: ${e.message}")
            true // fail-open
        }
    }

    /**
     * 星期数字转中文名（Calendar.DAY_OF_WEEK: 1=周日 ... 7=周六）
     */
    private fun weekdayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> "未知"
        }
    }

    /**
     * 启动前台服务拉起目标 App
     */
    private fun startLauncherService(
        context: Context,
        taskId: Long,
        targetPackage: String,
        targetAppName: String,
        returnDelaySeconds: Int
    ) {
        val serviceIntent = Intent(context, AppLauncherService::class.java).apply {
            putExtra("task_id", taskId)
            putExtra("target_package", targetPackage)
            putExtra("target_app_name", targetAppName)
            putExtra("return_delay_seconds", returnDelaySeconds)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动服务失败: ${e.message}", e)
        }
    }
}
