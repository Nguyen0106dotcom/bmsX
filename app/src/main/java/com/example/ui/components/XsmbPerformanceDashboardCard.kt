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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.XsmbDashboardSummary
import com.example.data.XsmbStation
import com.example.ui.theme.LocalAnalyticsColors
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XsmbPerformanceDashboardCard(
    dashboard: XsmbDashboardSummary?,
    isLoading: Boolean,
    isSyncing: Boolean,
    activeProfileId: String,
    activeProfileShortName: String,
    targetDate: LocalDate,
    selectedWindowDraws: Int,
    selectedStation: XsmbStation,
    onSwitchToXsmb: () -> Unit,
    onSelectWindowDraws: (Int) -> Unit,
    onSelectStation: (XsmbStation) -> Unit,
    onUpdateXsmbData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAnalyticsColors.current

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, colors.goldAccent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("xsmb_performance_dashboard_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            // Header + Sync/Update button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "Bảng hiệu suất dự đoán XSMB",
                        tint = colors.goldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Dashboard Hiệu Suất XSMB (% Chính Xác)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val rangeLabel = dashboard?.stats?.let { st ->
                            val fromStr = st.fromDate?.formatVn() ?: "--"
                            val toStr = st.toDate?.formatVn() ?: "--"
                            "${st.stationLabel} · ${st.totalDays} kỳ ($fromStr → $toStr)"
                        } ?: "Đang tính hiệu suất các kỳ quay XSMB trước kỳ ${targetDate.formatVn()}..."
                        Text(
                            text = rangeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onUpdateXsmbData,
                    enabled = !isSyncing && !isLoading,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dashboard_update_xsmb_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(5.dp))
                    Text("Cập nhật KQ", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (activeProfileId != "xsmb") {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = colors.warningAmber.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, colors.warningAmber.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Bạn đang ở chế độ $activeProfileShortName. Bảng này thống kê riêng XSMB.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.warningAmber,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onSwitchToXsmb,
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.goldAccent),
                            modifier = Modifier.testTag("dashboard_switch_xsmb_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Về XSMB", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Window size chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(7, 14, 30, 60, 90).forEach { count ->
                    FilterChip(
                        selected = selectedWindowDraws == count,
                        onClick = { onSelectWindowDraws(count) },
                        enabled = !isLoading,
                        label = { Text("$count kỳ XSMB", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.testTag("dashboard_window_$count")
                    )
                }
            }

            if (dashboard != null && dashboard.stats.totalDays > 0) {
                val stats = dashboard.stats

                // Accuracy Percentage Progress Gauges
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccuracyGaugeRow(
                        label = "Top 3 Số Vàng (Đề xuất chính)",
                        pct = dashboard.top3AccuracyPct,
                        hits = stats.top3Hits,
                        total = stats.totalDays,
                        extraInfo = "TB %.2f nháy/kỳ".format(dashboard.avgNhayTop3),
                        accentColor = colors.hitGreen
                    )
                    AccuracyGaugeRow(
                        label = "Top 5 Đề Xuất",
                        pct = dashboard.top5AccuracyPct,
                        hits = stats.top5Hits,
                        total = stats.totalDays,
                        extraInfo = null,
                        accentColor = colors.goldAccent
                    )
                    AccuracyGaugeRow(
                        label = "Top 10 Đề Xuất",
                        pct = dashboard.top10AccuracyPct,
                        hits = stats.top10Hits,
                        total = stats.totalDays,
                        extraInfo = "Trúng ${stats.specialTop10Hits} lần ĐB",
                        accentColor = colors.neutralBlue
                    )
                    AccuracyGaugeRow(
                        label = "Top 1 Bạch Thủ (Hạng #1)",
                        pct = dashboard.top1AccuracyPct,
                        hits = stats.top1Hits,
                        total = stats.totalDays,
                        extraInfo = null,
                        accentColor = MaterialTheme.colorScheme.secondary
                    )
                    AccuracyGaugeRow(
                        label = "Bắt Giải Đặc Biệt (trong Top 10)",
                        pct = dashboard.specialTop10AccuracyPct,
                        hits = stats.specialTop10Hits,
                        total = stats.totalDays,
                        extraInfo = "Top 3 bắt ĐB: ${stats.specialTop3Hits} kỳ",
                        accentColor = colors.specialCrimson
                    )
                }

                // Momentum & Streak Strip
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricSummaryTile(
                        title = "7 kỳ gần nhất (T3)",
                        value = "%.1f%%".format(dashboard.recent7Top3AccuracyPct),
                        subtitle = "Top 5: %.0f%% · Top 10: %.0f%%".format(
                            dashboard.recent7Top5AccuracyPct,
                            dashboard.recent7Top10AccuracyPct
                        ),
                        accentColor = colors.hitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryTile(
                        title = "Chuỗi đúng Top 3",
                        value = "${dashboard.currentTop3WinStreak} kỳ",
                        subtitle = "Kỷ lục: ${dashboard.longestTop3WinStreak} kỳ liên tiếp",
                        accentColor = colors.goldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Per-Station XSMB Accuracy Breakdown
                if (dashboard.stationBreakdowns.size > 1) {
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tỷ lệ chính xác Top 3 theo từng đài XSMB:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (selectedStation != XsmbStation.ALL) {
                            TextButton(
                                onClick = { onSelectStation(XsmbStation.ALL) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("dashboard_reset_station_button")
                            ) {
                                Text("Xem tất cả đài", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        dashboard.stationBreakdowns.forEach { row ->
                            val isSelectedStation = selectedStation == row.station
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (isSelectedStation) {
                                    colors.goldAccent.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                                border = BorderStroke(
                                    width = if (isSelectedStation) 1.5.dp else 1.dp,
                                    color = if (isSelectedStation) colors.goldAccent else colors.cardBorder
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dashboard_station_${row.station.id}")
                                    .clickable { onSelectStation(row.station) }
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = row.station.shortName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelectedStation) colors.goldAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "T3: %.1f%% (%d/%d)".format(
                                            row.top3AccuracyPct,
                                            row.top3Hits,
                                            row.totalDraws
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.hitGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "T10: %.0f%% · ĐB: %d".format(
                                            row.top10AccuracyPct,
                                            row.specialTop10Hits
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.mutedText
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

@Composable
private fun AccuracyGaugeRow(
    label: String,
    pct: Double,
    hits: Int,
    total: Int,
    extraInfo: String?,
    accentColor: Color
) {
    val colors = LocalAnalyticsColors.current
    val misses = (total - hits).coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "%.1f%%".format(pct),
                style = MaterialTheme.typography.titleSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
            color = accentColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "✓ $hits đúng · ✗ $misses sai / $total kỳ",
                style = MaterialTheme.typography.labelSmall,
                color = colors.mutedText
            )
            if (extraInfo != null) {
                Text(
                    text = extraInfo,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
            }
        }
    }
}
