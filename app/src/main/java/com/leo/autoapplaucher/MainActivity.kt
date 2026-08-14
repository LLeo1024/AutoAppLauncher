package com.leo.autoapplaucher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.leo.autoapplaucher.data.TaskEntity
import com.leo.autoapplaucher.ui.screen.AppPickerScreen
import com.leo.autoapplaucher.ui.screen.CreateTaskScreen
import com.leo.autoapplaucher.ui.screen.PermissionSettingsScreen
import com.leo.autoapplaucher.ui.screen.TaskListScreen
import com.leo.autoapplaucher.ui.theme.AutoAppLauncherTheme
import com.leo.autoapplaucher.ui.viewmodel.InstalledApp
import com.leo.autoapplaucher.ui.viewmodel.TaskViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoAppLauncherTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: TaskViewModel = viewModel()) {
    val navController = rememberNavController()
    val selectedApp = remember { mutableStateOf<InstalledApp?>(null) }
    val editingTask = remember { mutableStateOf<TaskEntity?>(null) }
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = "task_list"
    ) {
        composable("task_list") {
            TaskListScreen(
                onAddTask = {
                    selectedApp.value = null
                    editingTask.value = null
                    navController.navigate("create_task")
                },
                onOpenSettings = {
                    navController.navigate("permission_settings")
                },
                onEditTask = { taskId ->
                    selectedApp.value = null
                    scope.launch {
                        editingTask.value = viewModel.getTaskById(taskId)
                        navController.navigate("edit_task")
                    }
                },
                viewModel = viewModel
            )
        }

        composable("create_task") {
            CreateTaskScreen(
                selectedApp = selectedApp.value,
                onPickApp = {
                    navController.navigate("app_picker")
                },
                onCreate = { pkg, name, hour, minute, repeatMode, weekDays,
                             useRandomTime, rangeStartH, rangeStartM, rangeEndH, rangeEndM, returnDelay ->
                    viewModel.createTask(
                        targetPackage = pkg,
                        targetAppName = name,
                        hour = hour,
                        minute = minute,
                        repeatMode = repeatMode,
                        weekDays = weekDays,
                        useRandomTime = useRandomTime,
                        rangeStartHour = rangeStartH,
                        rangeStartMinute = rangeStartM,
                        rangeEndHour = rangeEndH,
                        rangeEndMinute = rangeEndM,
                        returnDelaySeconds = returnDelay
                    )
                    navController.popBackStack("task_list", inclusive = false)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("edit_task") {
            CreateTaskScreen(
                selectedApp = selectedApp.value,
                onPickApp = {
                    navController.navigate("app_picker")
                },
                onCreate = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
                onUpdate = { taskId, pkg, name, hour, minute, repeatMode, weekDays,
                             useRandomTime, rangeStartH, rangeStartM, rangeEndH, rangeEndM, returnDelay ->
                    viewModel.updateTask(
                        taskId = taskId,
                        targetPackage = pkg,
                        targetAppName = name,
                        hour = hour,
                        minute = minute,
                        repeatMode = repeatMode,
                        weekDays = weekDays,
                        useRandomTime = useRandomTime,
                        rangeStartHour = rangeStartH,
                        rangeStartMinute = rangeStartM,
                        rangeEndHour = rangeEndH,
                        rangeEndMinute = rangeEndM,
                        returnDelaySeconds = returnDelay
                    )
                    editingTask.value = null
                    navController.popBackStack("task_list", inclusive = false)
                },
                onBack = {
                    editingTask.value = null
                    navController.popBackStack()
                },
                existingTask = editingTask.value
            )
        }

        composable("app_picker") {
            AppPickerScreen(
                onAppSelected = { app ->
                    selectedApp.value = app
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }

        composable("permission_settings") {
            PermissionSettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
