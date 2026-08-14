package com.leo.autoapplaucher.ui.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leo.autoapplaucher.data.AppDatabase
import com.leo.autoapplaucher.data.TaskEntity
import com.leo.autoapplaucher.scheduler.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 安装的应用信息（用于App选择器）
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val alarmScheduler = AlarmScheduler(application)

    val tasks: Flow<List<TaskEntity>> = database.taskDao().getAllTasks()

    // 安装的应用列表
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    /**
     * 加载设备上安装的可启动应用
     */
    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val mainIntent = android.content.Intent(
                    android.content.Intent.ACTION_MAIN,
                    null
                ).apply {
                    addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                }
                pm.queryIntentActivities(mainIntent, 0)
                    .map { resolveInfo ->
                        val appInfo = resolveInfo.activityInfo.applicationInfo
                        InstalledApp(
                            packageName = appInfo.packageName,
                            appName = pm.getApplicationLabel(appInfo).toString(),
                            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        )
                    }
                    .sortedBy { it.appName.lowercase() }
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    /**
     * 创建新任务
     * 支持固定时间和随机时间段两种模式
     */
    fun createTask(
        targetPackage: String,
        targetAppName: String,
        hour: Int,
        minute: Int,
        repeatMode: Int = 1,
        weekDays: Int = 0b1111111,
        useRandomTime: Boolean = false,
        rangeStartHour: Int = 0,
        rangeStartMinute: Int = 0,
        rangeEndHour: Int = 0,
        rangeEndMinute: Int = 0,
        returnDelaySeconds: Int = 120
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                targetPackage = targetPackage,
                targetAppName = targetAppName,
                hour = hour,
                minute = minute,
                repeatMode = repeatMode,
                weekDays = weekDays,
                useRandomTime = useRandomTime,
                timeRangeStartHour = rangeStartHour,
                timeRangeStartMinute = rangeStartMinute,
                timeRangeEndHour = rangeEndHour,
                timeRangeEndMinute = rangeEndMinute,
                returnDelaySeconds = returnDelaySeconds
            )
            val id = database.taskDao().insert(task)
            val savedTask = task.copy(id = id)
            alarmScheduler.schedule(savedTask)
        }
    }

    /**
     * 更新任务
     * 先取消旧闹钟，再更新数据库，最后重新调度
     */
    fun updateTask(
        taskId: Long,
        targetPackage: String,
        targetAppName: String,
        hour: Int,
        minute: Int,
        repeatMode: Int = 1,
        weekDays: Int = 0b1111111,
        useRandomTime: Boolean = false,
        rangeStartHour: Int = 0,
        rangeStartMinute: Int = 0,
        rangeEndHour: Int = 0,
        rangeEndMinute: Int = 0,
        returnDelaySeconds: Int = 120
    ) {
        viewModelScope.launch {
            val oldTask = database.taskDao().getTaskById(taskId)
            if (oldTask != null) {
                alarmScheduler.cancel(oldTask)
            }
            val updatedTask = TaskEntity(
                id = taskId,
                targetPackage = targetPackage,
                targetAppName = targetAppName,
                hour = hour,
                minute = minute,
                repeatMode = repeatMode,
                weekDays = weekDays,
                enabled = oldTask?.enabled ?: true,
                createdAt = oldTask?.createdAt ?: System.currentTimeMillis(),
                useRandomTime = useRandomTime,
                timeRangeStartHour = rangeStartHour,
                timeRangeStartMinute = rangeStartMinute,
                timeRangeEndHour = rangeEndHour,
                timeRangeEndMinute = rangeEndMinute,
                returnDelaySeconds = returnDelaySeconds
            )
            database.taskDao().update(updatedTask)
            if (updatedTask.enabled) {
                alarmScheduler.schedule(updatedTask)
            }
        }
    }

    /**
     * 删除任务
     */
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            alarmScheduler.cancel(task)
            database.taskDao().delete(task)
        }
    }

    /**
     * 切换任务启用状态
     */
    fun toggleTaskEnabled(task: TaskEntity) {
        viewModelScope.launch {
            val newEnabled = !task.enabled
            database.taskDao().setEnabled(task.id, newEnabled)
            if (newEnabled) {
                alarmScheduler.schedule(task.copy(enabled = true))
            } else {
                alarmScheduler.cancel(task)
            }
        }
    }

    /**
     * 按 ID 获取任务（用于编辑页面预加载）
     */
    suspend fun getTaskById(id: Long): TaskEntity? {
        return withContext(Dispatchers.IO) {
            database.taskDao().getTaskById(id)
        }
    }
}
