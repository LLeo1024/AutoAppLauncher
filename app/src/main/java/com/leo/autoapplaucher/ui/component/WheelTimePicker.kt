package com.leo.autoapplaucher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 滚动式时间选择器
 * 两个可滚动列分别选择小时和分钟，替代 Material3 TimePicker
 */
@Composable
fun WheelTimePicker(
    initialHour: Int = 8,
    initialMinute: Int = 0,
    onTimeChanged: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelColumn(
            items = (0..23).toList(),
            initialIndex = initialHour.coerceIn(0, 23),
            onItemSelected = {
                selectedHour = it
                onTimeChanged(selectedHour, selectedMinute)
            },
            modifier = Modifier.width(80.dp)
        )

        Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        WheelColumn(
            items = (0..59).toList(),
            initialIndex = initialMinute.coerceIn(0, 59),
            onItemSelected = {
                selectedMinute = it
                onTimeChanged(selectedHour, selectedMinute)
            },
            modifier = Modifier.width(80.dp)
        )
    }
}

/**
 * 单个滚动列
 */
@Composable
private fun WheelColumn(
    items: List<Int>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 40.dp
    val visibleItemCount = 5
    val halfVisible = visibleItemCount / 2

    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val safeIndex = index.coerceIn(0, items.lastIndex)
                onItemSelected(items[safeIndex])
            }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItemCount),
        contentAlignment = Alignment.Center
    ) {
        // 中心高亮背景层（在 LazyColumn 下方）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        )

        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            contentPadding = PaddingValues(vertical = itemHeight * halfVisible),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { value ->
                val isSelected = value == items[state.firstVisibleItemIndex.coerceIn(0, items.lastIndex)]
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", value),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}
