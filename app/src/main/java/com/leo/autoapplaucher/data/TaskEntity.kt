package com.leo.autoapplaucher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 定时任务实体
 * 每条记录代表一个"在指定时间打开指定App"的任务
 *
 * 支持两种触发模式：
 * 1. 固定时间 — 使用 hour/minute 字段
 * 2. 随机时间段 — 使用 useRandomTime + timeRangeStart/End 字段，
 *    系统在时间段内随机选一个时间点触发，每次重复都会重新随机
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 目标App包名，例如 com.tencent.mm */
    val targetPackage: String,

    /** 目标App名称（用于UI显示） */
    val targetAppName: String,

    /** 触发时间：小时 (0-23) */
    val hour: Int,

    /** 触发时间：分钟 (0-59) */
    val minute: Int,

    /** 重复模式：0=一次, 1=每天, 2=每周指定日期, 3=法定节假日, 4=非法定节假日, 5=法定工作日 */
    val repeatMode: Int = 1,

    /** 当 repeatMode=2 时，存储星期几的位掩码 (bit0=周日, bit1=周一, ...) */
    val weekDays: Int = 0b1111111,

    /** 任务是否启用 */
    val enabled: Boolean = true,

    /** 创建时间戳 */
    val createdAt: Long = System.currentTimeMillis(),

    // ===== 随机时间段功能 =====

    /** 是否使用随机时间段模式 */
    val useRandomTime: Boolean = false,

    /** 随机时间段：开始时间 - 小时 (0-23) */
    val timeRangeStartHour: Int = 9,

    /** 随机时间段：开始时间 - 分钟 (0-59) */
    val timeRangeStartMinute: Int = 0,

    /** 随机时间段：结束时间 - 小时 (0-23) */
    val timeRangeEndHour: Int = 11,

    /** 随机时间段：结束时间 - 分钟 (0-59) */
    val timeRangeEndMinute: Int = 0
) {
    /**
     * 根据重复模式判断今天是否应该执行
     * 注意：法定节假日(3)、非法定节假日(4)和法定工作日(5)的判断在 AlarmReceiver 中异步完成
     *
     * @param dayOfWeek Calendar.DAY_OF_WEEK 值（1=周日, 2=周一, ..., 7=周六），
     *                  与 weekDays 位掩码 bit0=周日..bit6=周六 对应，需要减 1 对齐
     */
    fun shouldRunToday(dayOfWeek: Int): Boolean {
        return when (repeatMode) {
            0 -> true // 一次性任务（调度时已判断日期）
            1 -> true // 每天
            2 -> (weekDays shr (dayOfWeek - 1)) and 1 == 1 // 按星期
            3 -> true // 法定节假日（AlarmReceiver 中检查）
            4 -> true // 非法定节假日（AlarmReceiver 中检查）
            5 -> true // 法定工作日（AlarmReceiver 中检查）
            else -> true
        }
    }

    /**
     * 返回可读的时间字符串
     * 固定时间：如 "08:30"
     * 随机时间段：如 "09:00 ~ 11:00"
     */
    val timeString: String
        get() = if (useRandomTime) {
            String.format("%02d:%02d ~ %02d:%02d",
                timeRangeStartHour, timeRangeStartMinute,
                timeRangeEndHour, timeRangeEndMinute)
        } else {
            String.format("%02d:%02d", hour, minute)
        }

}
