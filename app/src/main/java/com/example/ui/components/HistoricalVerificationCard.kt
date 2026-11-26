package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CombinedPerformanceStats
import com.example.data.HistoricalVerificationDay
import com.example.data.LotteryProfile
import com.example.ui.theme.LocalAnalyticsColors
import java.time.LocalDate

private enum class VerificationFilter(val label: String) {
    ALL("Tất cả"),
    HITS_ONLY("Chỉ kỳ trúng"),
    SPECIAL_ONLY("Chỉ nổ ĐB")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoricalVerificationCard(
    profile: LotteryProfile,
    fromDate: LocalDate,
    toDate: LocalDate,
    isEvaluating: Boolean,
    stats: CombinedPerformanceStats?,
    selectedPredictDate: LocalDate,
    onSelectPresetDraws: (Int) -> Unit,
    onRunVerification: () -> Unit,
    onInspectPastDraw: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAnalyticsColors.current
    var selectedScope by rememberSaveable { mutableIntStateOf(3) }
    var outcomeFilter by rememberSaveable { mutableStateOf(VerificationFilter.ALL) }
    var showAllRows by rememberSaveable { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.neutralBlue.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Kiểm chứng lịch sử thuật toán",
                    tint = colors.neutralBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kiểm Chứng Lịch Sử Thuật Toán",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Chạy dự đoán từng kỳ quá khứ (chỉ dùng dữ liệu trước ngày quay) để đo độ chính xác thực tế.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                }
            }

            Text(
                text = "Khoảng kiểm chứng: ${fromDate.formatVn()} → ${toDate.formatVn()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            // Preset draw counts
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(10, 30, 60, 90).forEach { count ->
                    OutlinedButton(
                        onClick = { onSelectPresetDraws(count) },
                        enabled = !isEvaluating,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("verify_preset_${count}_draws")
                    ) {
                        Text("$count kỳ", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Button(
                onClick = onRunVerification,
                enabled = !isEvaluating,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("run_combined_backtest_button")
            ) {
                if (isEvaluating) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("Chạy Kiểm Chứng Khoảng Thời Gian Này")
            }

            if (stats != null) {
                HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.5f))

                if (stats.stationLabel.isNotBlank()) {
                    Text(
                        text = "Phạm vi: ${stats.stationLabel} · ${stats.totalDays} kỳ đã kiểm chứng",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.goldAccent
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricSummaryTile(
                        title = "Top 1",
                        value = "%.1f%%".format(stats.top1Rate),
                        subtitle = "${stats.top1Hits}/${stats.totalDays} kỳ",
                        accentColor = colors.neutralBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryTile(
                        title = "Top 3",
                        value = "%.1f%%".format(stats.top3Rate),
                        subtitle = "${stats.top3Hits}/${stats.totalDays} kỳ",
                        accentColor = colors.hitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryTile(
                        title = "Top 5",
                        value = "%.1f%%".format(stats.top5Rate),
                        subtitle = "${stats.top5Hits}/${stats.totalDays} kỳ",
                        accentColor = colors.goldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricSummaryTile(
                        title = "Top 10",
                        value = "%.1f%%".format(stats.top10Rate),
                        subtitle = "${stats.top10Hits}/${stats.totalDays} kỳ",
                        accentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryTile(
                        title = "Tổng nháy T3",
                        value = "${stats.totalNhayTop3} nháy",
                        subtitle = "TB %.2f/kỳ".format(
                            if (stats.totalDays > 0) stats.totalNhayTop3.toDouble() / stats.totalDays else 0.0
                        ),
                        accentColor = colors.hitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryTile(
                        title = "Nổ ĐB (T3/T10)",
                        value = "${stats.specialTop3Hits} / ${stats.specialTop10Hits}",
                        subtitle = "Theo ${stats.totalDays} kỳ",
                        accentColor = colors.specialCrimson,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (stats.dayLogs.isNotEmpty()) {
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))

                    Text(
                        text = "Chi tiết từng kỳ đã kiểm chứng (chạm kỳ để xem bảng điểm 00–99):",
                        style = MaterialTheme.typography.labelLarge
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3 to "Top 3", 5 to "Top 5", 10 to "Top 10").forEach { (scope, label) ->
                            FilterChip(
                                selected = selectedScope == scope,
                                onClick = { selectedScope = scope },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.testTag("verify_scope_top$scope")
                            )
                        }
                        VerificationFilter.entries.forEach { f ->
                            FilterChip(
                                selected = outcomeFilter == f,
                                onClick = { outcomeFilter = f },
                                label = { Text(f.label, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.testTag("verify_filter_${f.name.lowercase()}")
                            )
                        }
                    }

                    val filteredLogs = remember(stats.dayLogs, selectedScope, outcomeFilter) {
                        stats.dayLogs.filter { day ->
                            when (outcomeFilter) {
                                VerificationFilter.ALL -> true
                                VerificationFilter.HITS_ONLY -> when (selectedScope) {
                                    3 -> day.isWinTop3
                                    5 -> day.isWinTop5
                                    else -> day.isWinTop10
                                }
                                VerificationFilter.SPECIAL_ONLY -> when (selectedScope) {
                                    3 -> day.specialHitTop3
                                    5 -> day.actualSpecial != null && day.predictedTop5.any { it.first == day.actualSpecial }
                                    else -> day.specialHitTop10
                                }
                            }
                        }
                    }

                    val visibleLogs = if (showAllRows) filteredLogs else filteredLogs.take(12)

                    if (visibleLogs.isEmpty()) {
                        Text(
                            text = "Không có kỳ nào phù hợp bộ lọc trong khoảng thời gian này.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            visibleLogs.forEach { day ->
                                VerificationDayRow(
                                    day = day,
                                    profile = profile,
                                    scope = selectedScope,
                                    isSelected = day.date == selectedPredictDate,
                                    onInspect = { onInspectPastDraw(day.date) }
                                )
                            }
                        }

                        if (filteredLogs.size > 12) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = { showAllRows = !showAllRows },
                                    modifier = Modifier.testTag("verify_toggle_all_rows")
                                ) {
                                    Text(
                                        if (showAllRows) "Thu gọn danh sách"
                                        else "Xem toàn bộ ${filteredLogs.size} kỳ"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerificationDayRow(
    day: HistoricalVerificationDay,
    profile: LotteryProfile,
    scope: Int,
    isSelected: Boolean,
    onInspect: () -> Unit
) {
    val colors = LocalAnalyticsColors.current
    val picks = remember(day, scope) {
        when (scope) {
            3 -> day.predictedTop3
            5 -> day.predictedTop5
            else -> day.predictedTop10
        }
    }
    val matchedSet = remember(day, scope) {
        when (scope) {
            3 -> day.matchedTop3
            5 -> day.matchedTop5
            else -> day.matchedTop10
        }
    }
    val specialInScope = day.actualSpecial != null && picks.any { it.first == day.actualSpecial }
    val totalNhayInScope = picks.sumOf { day.actualHitCounts[it.first] ?: 0 }

    val statusText = when {
        specialInScope -> "🎯 NỔ ĐB + $totalNhayInScope NHÁY"
        matchedSet.isNotEmpty() -> "✅ TRÚNG $totalNhayInScope NHÁY"
        else -> "Trượt"
    }
    val border = when {
        isSelected -> colors.goldAccent
        specialInScope -> colors.specialCrimson.copy(alpha = 0.7f)
        matchedSet.isNotEmpty() -> colors.hitGreen.copy(alpha = 0.55f)
        else -> colors.cardBorder
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) {
            colors.goldAccent.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("verify_day_row_${day.date}")
            .clickable { onInspect() }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${day.date.dayOfWeekVn()}, ${day.date.formatVn()}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = colors.neutralBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = day.stationName,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.neutralBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    val spText = day.actualSpecial?.let { profile.formatNumber(it) } ?: "--"
                    Text(
                        text = "ĐB thực tế: $spText · Về ${day.actualUniqueNumbers.size} số",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when {
                            specialInScope -> colors.specialCrimsonContainer
                            matchedSet.isNotEmpty() -> colors.hitGreenContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                specialInScope -> colors.specialCrimson
                                matchedSet.isNotEmpty() -> colors.hitGreen
                                else -> colors.mutedText
                            },
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Xem chi tiết kỳ này",
                        tint = colors.mutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                picks.forEachIndexed { idx, (num, _) ->
                    val isSp = day.actualSpecial != null && num == day.actualSpecial
                    val nhay = day.actualHitCounts[num] ?: 0
                    val isHit = nhay > 0
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when {
                            isSp -> colors.specialCrimsonContainer
                            isHit -> colors.hitGreenContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            if (isSp || isHit) 1.5.dp else 1.dp,
                            when {
                                isSp -> colors.specialCrimson
                                isHit -> colors.hitGreen
                                else -> colors.cardBorder
                            }
                        )
                    ) {
                        val suffix = when {
                            isSp -> " ★ĐB"
                            nhay > 1 -> " ×$nhay"
                            isHit -> " ✓"
                            else -> ""
                        }
                        Text(
                            text = "#${idx + 1} ${profile.formatNumber(num)}$suffix",
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                isSp -> colors.specialCrimson
                                isHit -> colors.hitGreen
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (isHit) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
