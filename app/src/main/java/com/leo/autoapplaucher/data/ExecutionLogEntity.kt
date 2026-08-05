package com.leo.autoapplaucher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 任务执行日志
 * 记录每次任务触发的结果，方便用户排查问题
 */
@Entity(tableName = "execution_logs")
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 关联的任务ID */
    val taskId: Long,

    /** 目标App包名 */
    val targetPackage: String,

    /** 执行时间戳 */
    val executedAt: Long = System.currentTimeMillis(),

    /** 执行结果：success / failed */
    val result: String,

    /** 失败原因（如果失败） */
    val errorMessage: String? = null
)
