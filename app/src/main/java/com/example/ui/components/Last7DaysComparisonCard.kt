package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SevenDayComparisonRow
import com.example.data.SevenDayComparisonSummary
import com.example.data.XsmbStation
import com.example.ui.theme.LocalAnalyticsColors
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Last7DaysComparisonCard(
    summary: SevenDayComparisonSummary?,
    isLoading: Boolean,
    selectedStationFilter: XsmbStation,
    onFetchFromApi: () -> Unit,
    onInspectDrawDate: ((LocalDate) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAnalyticsColors.current
    var compareScope by rememberSaveable { mutableIntStateOf(3) } // 3, 5, or 10
    var filterToSelectedStation by rememberSaveable { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.neutralBlue.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            // Header row
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
                        imageVector = Icons.Default.Public,
                        contentDescription = "Nguồn Public API XSMB",
                        tint = colors.neutralBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Đối Chiếu 7 Ngày XSMB (Public API)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (summary != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (summary.fetchedFromNetwork) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = if (summary.fetchedFromNetwork) "Đã tải từ mạng" else "Dữ liệu cục bộ",
                                    tint = if (summary.fetchedFromNetwork) colors.hitGreen else colors.warningAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${summary.sourceLabel} · ${summary.fetchedAtLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (summary.fetchedFromNetwork) colors.hitGreen else colors.warningAmber,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = "Tải kết quả 7 kỳ gần nhất và đối chiếu dự đoán",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedText
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onFetchFromApi,
                    enabled = !isLoading,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("fetch_7days_api_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Tải API", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (summary?.errorMessage != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = colors.warningAmber.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, colors.warningAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Cảnh báo kết nối API",
                            tint = colors.warningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = summary.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.warningAmber,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onFetchFromApi,
                            enabled = !isLoading,
                            modifier = Modifier.testTag("retry_7days_api_button")
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
            }

            if (summary != null && summary.rows.isNotEmpty()) {
                val displayedRows = remember(summary.rows, filterToSelectedStation, selectedStationFilter) {
                    if (filterToSelectedStation && selectedStationFilter != XsmbStation.ALL) {
                        summary.rows.filter { selectedStationFilter.matchesDate(it.date) }
                    } else {
                        summary.rows
                    }
                }

                // KPI summary strip
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricSummaryTile(
                        title = "Top 3 trúng",
                        value = "${summary.top3WinDays}/${summary.daysCount}",
                        subtitle = "%.0f%% ngày".format(summary.top3WinRate),
                        accentColor = colors.hitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryTile(
                        title = "Top 5 trúng",
                        value = "${summary.top5WinDays}/${summary.daysCount}",
                        subtitle = "Lô Top 5",
                        accentColor = colors.goldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryTile(
                        title = "Top 10 trúng",
                        value = "${summary.top10WinDays}/${summary.daysCount}",
                        subtitle = "%.0f%% ngày".format(summary.top10WinRate),
                        accentColor = colors.neutralBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryTile(
                        title = "Nháy T3 / ĐB",
                        value = "${summary.totalNhayTop3} / ${summary.specialHitsTop10}",
                        subtitle = "Nháy / ĐB",
                        accentColor = colors.specialCrimson,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Top-N Scope + Station filter row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(3 to "Top 3", 5 to "Top 5", 10 to "Top 10").forEach { (scope, label) ->
                        FilterChip(
                            selected = compareScope == scope,
                            onClick = { compareScope = scope },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.testTag("compare_scope_top$scope")
                        )
                    }
                    if (selectedStationFilter != XsmbStation.ALL) {
                        FilterChip(
                            selected = filterToSelectedStation,
                            onClick = { filterToSelectedStation = !filterToSelectedStation },
                            label = {
                                Text(
                                    text = "Chỉ ${selectedStationFilter.shortName}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.testTag("compare_filter_station_chip")
                        )
                    }
                }

                if (displayedRows.isEmpty()) {
                    Text(
                        text = "Không có kỳ nào của ${selectedStationFilter.displayName} trong danh sách lọc hiện tại. Tắt chip lọc để xem đủ 7 ngày.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        displayedRows.forEach { row ->
                            SevenDayComparisonDayCard(
                                row = row,
                                compareScope = compareScope,
                                onInspectDrawDate = onInspectDrawDate
                            )
                        }
                    }
                }
            } else if (!isLoading) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nhấn \"Tải API\" để lấy kết quả XSMB 7 ngày gần nhất từ nguồn công khai và đối chiếu với dự đoán thuật toán.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SevenDayComparisonDayCard(
    row: SevenDayComparisonRow,
    compareScope: Int,
    onInspectDrawDate: ((LocalDate) -> Unit)?
) {
    val colors = LocalAnalyticsColors.current
    var expanded by rememberSaveable(row.date.toString()) { mutableStateOf(false) }

    val picks = remember(row, compareScope) {
        when (compareScope) {
            3 -> row.predictedTop3
            5 -> row.predictedTop5
            else -> row.predictedTop10
        }
    }
    val hitSet = remember(row, compareScope) {
        when (compareScope) {
            3 -> row.hitNumbersInTop3
            5 -> row.hitNumbersInTop5
            else -> row.hitNumbersInTop10
        }
    }
    val specialInScope = row.actualSpecial != null && picks.any { it.first == row.actualSpecial }
    val totalNhayInScope = picks.sumOf { row.actualHitCounts[it.first] ?: 0 }

    val statusText = when {
        specialInScope -> "🎯 NỔ ĐB + $totalNhayInScope NHÁY"
        hitSet.isNotEmpty() -> "✅ TRÚNG $totalNhayInScope NHÁY"
        else -> "Trượt"
    }
    val statusBg = when {
        specialInScope -> colors.specialCrimsonContainer
        hitSet.isNotEmpty() -> colors.hitGreenContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusFg = when {
        specialInScope -> colors.specialCrimson
        hitSet.isNotEmpty() -> colors.hitGreen
        else -> colors.mutedText
    }
    val cardBorder = when {
        specialInScope -> colors.specialCrimson.copy(alpha = 0.7f)
        hitSet.isNotEmpty() -> colors.hitGreen.copy(alpha = 0.55f)
        else -> colors.cardBorder
    }

    val xsmbGroupLabels = listOf(
        "special" to "ĐB",
        "prize1" to "G1",
        "prize2" to "G2",
        "prize3" to "G3",
        "prize4" to "G4",
        "prize5" to "G5",
        "prize6" to "G6",
        "prize7" to "G7"
    )

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("seven_day_row_${row.date}")
            .clickable { expanded = !expanded }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
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
                            text = "${row.date.dayOfWeekVn()}, ${row.date.formatVn()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = colors.neutralBlue.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = row.station.shortName,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.neutralBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "ĐB thực tế: ${row.actualSpecial?.let { "%02d".format(it) } ?: "--"} · ${row.actualUniqueNumbers.size} đầu lô về",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = statusBg
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusFg,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Thu gọn kết quả" else "Xem chi tiết 27 giải",
                        tint = colors.mutedText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Predicted picks chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                picks.forEachIndexed { idx, (num, _) ->
                    val isSp = row.actualSpecial != null && num == row.actualSpecial
                    val nhayCount = row.actualHitCounts[num] ?: 0
                    val isHit = nhayCount > 0

                    val chipBg = when {
                        isSp -> colors.specialCrimsonContainer
                        isHit -> colors.hitGreenContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val chipBorder = when {
                        isSp -> colors.specialCrimson
                        isHit -> colors.hitGreen
                        else -> colors.cardBorder
                    }
                    val chipFg = when {
                        isSp -> colors.specialCrimson
                        isHit -> colors.hitGreen
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = chipBg,
                        border = BorderStroke(if (isSp || isHit) 1.5.dp else 1.dp, chipBorder)
                    ) {
                        val suffix = when {
                            isSp -> " ★ĐB"
                            nhayCount > 1 -> " ×$nhayCount"
                            isHit -> " ✓"
                            else -> ""
                        }
                        Text(
                            text = "#${idx + 1} ${"%02d".format(num)}$suffix",
                            style = MaterialTheme.typography.labelMedium,
                            color = chipFg,
                            fontWeight = if (isHit) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.5f))
                    val predictedSet = remember(picks) { picks.map { it.first }.toSet() }
                    xsmbGroupLabels.forEach { (key, title) ->
                        val values = row.prizeMap[key].orEmpty()
                        if (values.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (key == "special") colors.specialCrimson else colors.mutedText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(34.dp)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    values.forEach { str ->
                                        val nVal = str.toIntOrNull()
                                        val matched = nVal != null && nVal in predictedSet
                                        val isSp = key == "special"
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = when {
                                                isSp -> colors.specialCrimsonContainer
                                                matched -> colors.hitGreenContainer
                                                else -> MaterialTheme.colorScheme.surface
                                            },
                                            border = if (matched || isSp) {
                                                BorderStroke(1.dp, if (isSp) colors.specialCrimson else colors.hitGreen)
                                            } else {
                                                null
                                            }
                                        ) {
                                            Text(
                                                text = str,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = when {
                                                    isSp -> colors.specialCrimson
                                                    matched -> colors.hitGreen
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                fontWeight = if (matched || isSp) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (onInspectDrawDate != null) {
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { onInspectDrawDate(row.date) },
                                modifier = Modifier.testTag("open_kqxs_${row.date}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Mở bảng KQXS & Thống kê")
                            }
                        }
                    }
                }
            }
        }
    }
}
