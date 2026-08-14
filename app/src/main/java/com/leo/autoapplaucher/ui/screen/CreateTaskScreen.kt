package com.leo.autoapplaucher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.res.painterResource
import com.leo.autoapplaucher.R
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leo.autoapplaucher.ui.component.WheelTimePicker
import com.leo.autoapplaucher.ui.theme.TextSecondary
import com.leo.autoapplaucher.ui.viewmodel.InstalledApp
import com.leo.autoapplaucher.data.TaskEntity

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CreateTaskScreen(
    selectedApp: InstalledApp?,
    onPickApp: () -> Unit,
    onCreate: (
        packageName: String,
        appName: String,
        hour: Int,
        minute: Int,
        repeatMode: Int,
        weekDays: Int,
        useRandomTime: Boolean,
        rangeStartHour: Int,
        rangeStartMinute: Int,
        rangeEndHour: Int,
        rangeEndMinute: Int
    ) -> Unit,
    onBack: () -> Unit,
    existingTask: TaskEntity? = null,
    onUpdate: (
        taskId: Long,
        packageName: String,
        appName: String,
        hour: Int,
        minute: Int,
        repeatMode: Int,
        weekDays: Int,
        useRandomTime: Boolean,
        rangeStartHour: Int,
        rangeStartMinute: Int,
        rangeEndHour: Int,
        rangeEndMinute: Int
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _ -> }
) {
    val isEditMode = existingTask != null

    // 固定时间模式
    var hour by remember { mutableIntStateOf(existingTask?.hour ?: 8) }
    var minute by remember { mutableIntStateOf(existingTask?.minute ?: 0) }

    // 随机时间段模式
    var useRandomTime by remember { mutableStateOf(existingTask?.useRandomTime ?: false) }
    var rangeStartHour by remember { mutableIntStateOf(existingTask?.timeRangeStartHour ?: 9) }
    var rangeStartMinute by remember { mutableIntStateOf(existingTask?.timeRangeStartMinute ?: 0) }
    var rangeEndHour by remember { mutableIntStateOf(existingTask?.timeRangeEndHour ?: 11) }
    var rangeEndMinute by remember { mutableIntStateOf(existingTask?.timeRangeEndMinute ?: 0) }

    var repeatMode by remember { mutableIntStateOf(existingTask?.repeatMode ?: 1) }
    var selectedDays by remember { mutableIntStateOf(existingTask?.weekDays ?: 0b1111111) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑任务" else "新建任务", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ===== 选择目标App =====
            Text(
                text = "目标应用",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickApp() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_apps),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val displayName = selectedApp?.appName
                            ?: existingTask?.targetAppName
                            ?: "点击选择应用"
                        val displayPackage = selectedApp?.packageName
                            ?: existingTask?.targetPackage
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (displayName != "点击选择应用")
                                MaterialTheme.colorScheme.onSurface
                            else
                                TextSecondary
                        )
                        if (displayPackage != null) {
                            Text(
                                text = displayPackage,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 触发模式切换 =====
            Text(
                text = "触发方式",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = !useRandomTime,
                    onClick = { useRandomTime = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(painter = painterResource(R.drawable.ic_access_time), contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                ) {
                    Text("固定时间")
                }
                SegmentedButton(
                    selected = useRandomTime,
                    onClick = { useRandomTime = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(painter = painterResource(R.drawable.ic_shuffle), contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                ) {
                    Text("随机时间段")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 时间选择区域 =====
            if (useRandomTime) {
                // 随机时间段模式
                Text(
                    text = "开始时间",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WheelTimePicker(
                            initialHour = rangeStartHour,
                            initialMinute = rangeStartMinute,
                            onTimeChanged = { h, m ->
                                rangeStartHour = h
                                rangeStartMinute = m
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "结束时间",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WheelTimePicker(
                            initialHour = rangeEndHour,
                            initialMinute = rangeEndMinute,
                            onTimeChanged = { h, m ->
                                rangeEndHour = h
                                rangeEndMinute = m
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_shuffle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "系统将在该时间段内随机选一个时间点触发，每天触发时间不同",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val startTotal = rangeStartHour * 60 + rangeStartMinute
                val endTotal = rangeEndHour * 60 + rangeEndMinute
                if (endTotal <= startTotal) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "结束时间需大于开始时间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // 固定时间模式
                Text(
                    text = "触发时间",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WheelTimePicker(
                            initialHour = hour,
                            initialMinute = minute,
                            onTimeChanged = { h, m ->
                                hour = h
                                minute = m
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 重复模式 =====
            Text(
                text = "重复模式",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = repeatMode == 0,
                    onClick = { repeatMode = 0 },
                    label = { Text("仅一次") }
                )
                FilterChip(
                    selected = repeatMode == 1,
                    onClick = { repeatMode = 1 },
                    label = { Text("每天") }
                )
                FilterChip(
                    selected = repeatMode == 2,
                    onClick = { repeatMode = 2 },
                    label = { Text("每周") }
                )
                FilterChip(
                    selected = repeatMode == 3,
                    onClick = { repeatMode = 3 },
                    label = { Text("法定节假日") }
                )
                FilterChip(
                    selected = repeatMode == 4,
                    onClick = { repeatMode = 4 },
                    label = { Text("非法定节假日") }
                )
                FilterChip(
                    selected = repeatMode == 5,
                    onClick = { repeatMode = 5 },
                    label = { Text("法定工作日") }
                )
            }

            // 每周模式：选择星期
            if (repeatMode == 2) {
                Spacer(modifier = Modifier.height(12.dp))
                val dayLabels = listOf("日", "一", "二", "三", "四", "五", "六")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    dayLabels.forEachIndexed { index, label ->
                        val bit = 1 shl index
                        val isSelected = (selectedDays shr index) and 1 == 1
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    selectedDays = selectedDays xor bit
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 节假日模式提示
            if (repeatMode == 3 || repeatMode == 4 || repeatMode == 5) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar_month),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (repeatMode) {
                                3 -> "仅在法定节假日（如春节、国庆等）触发，需联网获取节假日数据"
                                5 -> "仅在实际工作日触发（含调休上班的周末，排除法定节假日和普通周末），需联网获取节假日数据"
                                else -> "仅在非法定节假日（工作日+周末）触发，需联网获取节假日数据"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 验证时间段有效性
            val startTotal = rangeStartHour * 60 + rangeStartMinute
            val endTotal = rangeEndHour * 60 + rangeEndMinute
            val timeRangeValid = !useRandomTime || endTotal > startTotal

            // 编辑模式下，如果没有新选App则使用原有App
            val effectiveApp = selectedApp
                ?: existingTask?.let { InstalledApp(it.targetPackage, it.targetAppName, false) }
            val canSubmit = effectiveApp != null && timeRangeValid

            // 创建/保存按钮
            Button(
                onClick = {
                    if (canSubmit && effectiveApp != null) {
                        if (isEditMode && existingTask != null) {
                            onUpdate(
                                existingTask.id,
                                effectiveApp.packageName,
                                effectiveApp.appName,
                                hour,
                                minute,
                                repeatMode,
                                selectedDays,
                                useRandomTime,
                                rangeStartHour,
                                rangeStartMinute,
                                rangeEndHour,
                                rangeEndMinute
                            )
                        } else {
                            onCreate(
                                effectiveApp.packageName,
                                effectiveApp.appName,
                                hour,
                                minute,
                                repeatMode,
                                selectedDays,
                                useRandomTime,
                                rangeStartHour,
                                rangeStartMinute,
                                rangeEndHour,
                                rangeEndMinute
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = canSubmit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditMode) "保存修改" else "创建任务", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
