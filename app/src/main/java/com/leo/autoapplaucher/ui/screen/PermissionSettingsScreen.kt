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
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import com.leo.autoapplaucher.R
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leo.autoapplaucher.ui.theme.Error
import com.leo.autoapplaucher.ui.theme.Success
import com.leo.autoapplaucher.ui.theme.TextSecondary
import com.leo.autoapplaucher.ui.theme.Warning
import com.leo.autoapplaucher.util.MiuiUtils

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
            if (status.isMiui) {
                // MIUI 专属权限
                Text(
                    text = "MIUI 专属权限 (${status.miuiVersion})",
                    style = MaterialTheme.typography.labelLarge,
                    color = Warning,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                PermissionCard(
                    iconRes = R.drawable.ic_power_settings_new,
                    title = "自启动",
                    description = "重启手机后自动恢复所有定时任务",
                    isGranted = true, // 无法通过API检测，默认显示需手动确认
                    showManualHint = true,
                    onClick = {
                        MiuiUtils.openAutoStartSettings(context)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PermissionCard(
                    iconRes = R.drawable.ic_battery_full,
                    title = "省电策略：无限制",
                    description = "防止待机时App被MIUI冻结导致闹钟不触发",
                    isGranted = status.batteryOptimization,
                    showManualHint = status.isMiui,
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
                    description = "允许App从后台拉起目标应用，这是核心功能必需权限",
                    isGranted = true, // 无法通过API检测
                    showManualHint = true,
                    onClick = {
                        MiuiUtils.openBackgroundPopupSettings(context)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PermissionCard(
                    iconRes = R.drawable.ic_screen_lock_portrait,
                    title = "锁屏不清理",
                    description = "防止锁屏后App被清理导致任务失效",
                    isGranted = true,
                    showManualHint = true,
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
                isGranted = status.batteryOptimization,
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
                isGranted = status.notification,
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
                isGranted = status.exactAlarm,
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
                        text = "小米手机的MIUI系统对后台运行有额外限制，以上权限需要手动开启。点击对应项目可直接跳转到设置页面。\n\n如果跳转失败，请手动前往：设置 → 应用设置 → 应用管理 → 定时启动 → 权限管理。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    isGranted: Boolean,
    showManualHint: Boolean = false,
    onClick: () -> Unit
) {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isGranted) Success.copy(alpha = 0.1f)
                        else Error.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = if (isGranted) Success else Error,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (showManualHint) {
                        Text(
                            text = "(需手动确认)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Warning
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            val statusIconRes = if (isGranted) R.drawable.ic_check_circle else R.drawable.ic_error
            Icon(
                painter = painterResource(statusIconRes),
                contentDescription = null,
                tint = if (isGranted) Success else Error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
