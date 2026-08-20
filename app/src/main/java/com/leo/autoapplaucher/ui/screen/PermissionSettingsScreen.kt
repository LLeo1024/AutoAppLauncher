package com.leo.autoapplaucher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import com.leo.autoapplaucher.R
import com.leo.autoapplaucher.ui.theme.Error
import com.leo.autoapplaucher.ui.theme.Success
import com.leo.autoapplaucher.ui.theme.TextSecondary
import com.leo.autoapplaucher.ui.theme.Warning
import com.leo.autoapplaucher.util.MiuiUtils

/**
 * 权限状态三态
 */
enum class PermState {
    GRANTED,   // 已开启（绿色）
    DENIED,    // 未开启（红色）
    UNKNOWN    // 无法检测，需手动确认（黄色）
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }

    val status = remember(refreshKey) {
        MiuiUtils.getPermissionStatus(context)
    }

    // 统计可检测权限的状态
    val checkedPerms = mutableListOf<Pair<String, PermState>>()
    if (status.isMiui) {
        checkedPerms.add("省电策略" to if (status.batteryOptimization) PermState.GRANTED else PermState.DENIED)
        checkedPerms.add("后台弹出界面" to if (status.overlayPermission) PermState.GRANTED else PermState.DENIED)
    }
    checkedPerms.add("电池优化" to if (status.batteryOptimization) PermState.GRANTED else PermState.DENIED)
    checkedPerms.add("通知权限" to if (status.notification) PermState.GRANTED else PermState.DENIED)
    checkedPerms.add("精确闹钟" to if (status.exactAlarm) PermState.GRANTED else PermState.DENIED)

    val grantedCount = checkedPerms.count { it.second == PermState.GRANTED }
    val deniedCount = checkedPerms.count { it.second == PermState.DENIED }
    val unknownCount = if (status.isMiui) 2 else 0 // 自启动 + 锁屏不清理

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("权限设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshKey++ }) {
                        Icon(painter = painterResource(R.drawable.ic_check_circle), contentDescription = "刷新状态")
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
            // ===== 顶部汇总卡片 =====
            PermissionSummaryCard(
                grantedCount = grantedCount,
                deniedCount = deniedCount,
                unknownCount = unknownCount,
                isMiui = status.isMiui,
                miuiVersion = status.miuiVersion
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (status.isMiui) {
                // MIUI 专属权限
                Text(
                    text = "MIUI 专属权限 (${status.miuiVersion})",
                    style = MaterialTheme.typography.labelLarge,
                    color = Warning,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PermissionCard(
                    iconRes = R.drawable.ic_power_settings_new,
                    title = "自启动",
                    description = "重启手机后自动恢复所有定时任务，关闭后重启手机闹钟会丢失",
                    state = PermState.UNKNOWN,
                    configPath = "安全中心 → 应用管理 → 定时启动 → 自启动",
                    onClick = {
                        MiuiUtils.openAutoStartSettings(context)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PermissionCard(
                    iconRes = R.drawable.ic_battery_full,
                    title = "省电策略：无限制",
                    description = "防止待机时App被MIUI冻结导致闹钟不触发",
                    state = if (status.batteryOptimization) PermState.GRANTED else PermState.DENIED,
                    configPath = "设置 → 省电与电池 → 应用智能省电 → 定时启动 → 无限制",
                    onClick = {
                        if (!status.batteryOptimization) {
                            MiuiUtils.requestIgnoreBatteryOptimization(
                                context as android.app.Activity
                            )
                        } else {
                            MiuiUtils.openBatterySaverSettings(context)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PermissionCard(
                    iconRes = R.drawable.ic_notifications,
                    title = "后台弹出界面",
                    description = "允许App在后台拉起目标App，未开启时拉起会被MIUI静默拦截",
                    state = if (status.overlayPermission) PermState.GRANTED else PermState.DENIED,
                    configPath = "设置 → 应用设置 → 应用管理 → 定时启动 → 后台弹出界面",
                    onClick = {
                        if (!status.overlayPermission) {
                            MiuiUtils.openOverlaySettings(context)
                        } else {
                            MiuiUtils.openBackgroundPopupSettings(context)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PermissionCard(
                    iconRes = R.drawable.ic_screen_lock_portrait,
                    title = "锁屏不清理",
                    description = "防止锁屏后App被清理导致任务失效",
                    state = PermState.UNKNOWN,
                    configPath = "设置 → 应用设置 → 应用管理 → 定时启动 → 锁屏不清理",
                    onClick = {
                        MiuiUtils.openAppDetailSettings(context)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Android 标准权限
            Text(
                text = "Android 标准权限",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            PermissionCard(
                iconRes = R.drawable.ic_battery_full,
                title = "电池优化：已忽略",
                description = "允许App在后台持续运行，不被系统电池优化策略限制",
                state = if (status.batteryOptimization) PermState.GRANTED else PermState.DENIED,
                configPath = "设置 → 应用 → 特殊应用权限 → 忽略电池优化 → 定时启动",
                onClick = {
                    if (!status.batteryOptimization) {
                        MiuiUtils.requestIgnoreBatteryOptimization(
                            context as android.app.Activity
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionCard(
                iconRes = R.drawable.ic_notifications,
                title = "通知权限",
                description = "前台服务需要通知权限才能显示常驻通知",
                state = if (status.notification) PermState.GRANTED else PermState.DENIED,
                configPath = "设置 → 应用 → 定时启动 → 通知",
                onClick = {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    ).apply {
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionCard(
                iconRes = R.drawable.ic_security,
                title = "精确闹钟权限",
                description = if (status.exactAlarm) "已授权精确闹钟" else "需要授权精确闹钟",
                state = if (status.exactAlarm) PermState.GRANTED else PermState.DENIED,
                configPath = "设置 → 应用 → 特殊应用权限 → 闹钟和提醒 → 定时启动",
                onClick = {
                    MiuiUtils.openExactAlarmSettings(context)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Warning.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "MIUI 权限说明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Warning
                    )
                    Text(
                        text = "小米手机的MIUI系统对后台运行有额外限制，以上权限需要手动开启。\n\n" +
                                "点击对应项目可直接跳转到设置页面。\n\n" +
                                "如果跳转失败，请手动前往：\n" +
                                "设置 → 应用设置 → 应用管理 → 定时启动 → 权限管理",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 顶部汇总卡片
 */
@Composable
private fun PermissionSummaryCard(
    grantedCount: Int,
    deniedCount: Int,
    unknownCount: Int,
    isMiui: Boolean,
    miuiVersion: String
) {
    val allGood = deniedCount == 0
    val bgColor = when {
        deniedCount > 0 -> Error.copy(alpha = 0.08f)
        unknownCount > 0 -> Warning.copy(alpha = 0.08f)
        else -> Success.copy(alpha = 0.08f)
    }
    val headerColor = when {
        deniedCount > 0 -> Error
        unknownCount > 0 -> Warning
        else -> Success
    }
    val headerText = when {
        deniedCount > 0 -> "$deniedCount 项权限未开启，请点击下方对应项设置"
        unknownCount > 0 -> "可检测权限已通过，$unknownCount 项MIUI权限需手动确认"
        else -> "所有权限已就绪"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(if (allGood) R.drawable.ic_check_circle else R.drawable.ic_error),
                contentDescription = null,
                tint = headerColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = headerColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append("已开启 $grantedCount")
                        if (deniedCount > 0) append("  ·  未开启 $deniedCount")
                        if (unknownCount > 0) append("  ·  待确认 $unknownCount")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    state: PermState,
    configPath: String,
    onClick: () -> Unit
) {
    val stateColor = when (state) {
        PermState.GRANTED -> Success
        PermState.DENIED -> Error
        PermState.UNKNOWN -> Warning
    }
    val stateText = when (state) {
        PermState.GRANTED -> "已开启"
        PermState.DENIED -> "未开启"
        PermState.UNKNOWN -> "待确认"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(stateColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // 状态标签
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(stateColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stateText,
                                style = MaterialTheme.typography.labelSmall,
                                color = stateColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Icon(
                    painter = painterResource(
                        if (state == PermState.GRANTED) R.drawable.ic_check_circle
                        else R.drawable.ic_error
                    ),
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 配置路径提示（未开启或待确认时显示）
            if (state != PermState.GRANTED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(stateColor.copy(alpha = 0.06f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_error),
                        contentDescription = null,
                        tint = stateColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "设置路径：$configPath",
                        style = MaterialTheme.typography.labelSmall,
                        color = stateColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
