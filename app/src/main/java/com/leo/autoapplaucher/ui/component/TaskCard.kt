package com.leo.autoapplaucher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.res.painterResource
import com.leo.autoapplaucher.R
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leo.autoapplaucher.data.TaskEntity
import com.leo.autoapplaucher.ui.theme.Success
import com.leo.autoapplaucher.ui.theme.TextSecondary
import com.leo.autoapplaucher.ui.theme.Warning

@Composable
fun TaskCard(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
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
            // 时间图标（随机模式用 Shuffle 图标）
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (task.enabled)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                val timeIconRes = if (task.useRandomTime) R.drawable.ic_shuffle
                    else if (task.repeatMode == 3 || task.repeatMode == 4 || task.repeatMode == 5) R.drawable.ic_calendar_month
                    else R.drawable.ic_access_time
                Icon(
                    painter = painterResource(timeIconRes),
                    contentDescription = null,
                    tint = if (task.enabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 任务信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.timeString,
                    style = if (task.useRandomTime)
                        MaterialTheme.typography.titleLarge
                    else
                        MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (task.enabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = task.targetAppName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1
                )
                // 重复模式标签 + 随机标签
                val repeatText = when (task.repeatMode) {
                    0 -> "仅一次"
                    1 -> "每天"
                    2 -> "每周"
                    3 -> "法定节假日"
                    4 -> "非法定节假日"
                    5 -> "法定工作日"
                    else -> "每天"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (task.enabled) Success.copy(alpha = 0.12f)
                                else Warning.copy(alpha = 0.12f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (task.enabled) repeatText else "已暂停",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (task.enabled) Success else Warning,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (task.useRandomTime) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "随机",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 开关和删除按钮
            Switch(
                checked = task.enabled,
                onCheckedChange = { onToggle() }
            )

            IconButton(onClick = { onDelete() }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
