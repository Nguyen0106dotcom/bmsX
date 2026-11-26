package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.DailyKqxsStats
import com.example.data.DeepAnalysisResult
import com.example.data.LotteryDraw
import com.example.data.LotteryProfile
import com.example.data.XsmbStation
import com.example.ui.MainUiState
import com.example.ui.components.DateNavigatorCard
import com.example.ui.components.MetricSummaryTile
import com.example.ui.components.dayOfWeekVn
import com.example.ui.components.formatVn
import com.example.ui.theme.LocalAnalyticsColors
import java.time.LocalDate

@Composable
fun KqxsScreen(
    state: MainUiState,
    onPrevKqxsDate: () -> Unit,
    onNextKqxsDate: () -> Unit,
    onSelectKqxsDate: (LocalDate) -> Unit,
    onSelectKqxsStationFilter: (XsmbStation) -> Unit,
    onRunDeepAnalysis: (String, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(contentAlignment = Alignment.TopCenter, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Bảng KQXS & Thống Kê") },
                    modifier = Modifier.testTag("tab_kqxs_board")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Phân Tích Số Sâu") },
                    modifier = Modifier.testTag("tab_deep_analysis")
                )
            }

            if (selectedTab == 0) {
                KqxsBoardContent(
                    state = state,
                    onPrevDate = onPrevKqxsDate,
                    onNextDate = onNextKqxsDate,
                    onDatePicked = onSelectKqxsDate,
                    onSelectStationFilter = onSelectKqxsStationFilter
                )
            } else {
                DeepAnalysisContent(
                    state = state,
                    onRunDeepAnalysis = onRunDeepAnalysis
                )
            }
        }
    }
}

@Composable
private fun KqxsBoardContent(
    state: MainUiState,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    onSelectStationFilter: (XsmbStation) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.selectedProfile.id == "xsmb") {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    XsmbStation.entries.forEach { station ->
                        FilterChip(
                            selected = state.kqxsStationFilter == station,
                            onClick = { onSelectStationFilter(station) },
                            label = { Text(station.shortName, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.testTag("kqxs_station_chip_${station.id}")
                        )
                    }
                }
            }
        }

        item {
            val stationLabel = if (state.selectedProfile.id == "xsmb") {
                " · ${XsmbStation.forDate(state.kqxsSelectedDate).shortName}"
            } else {
                ""
            }
            DateNavigatorCard(
                date = state.kqxsSelectedDate,
                label = "Kết quả ${state.selectedProfile.shortName}$stationLabel",
                onPrevious = onPrevDate,
                onNext = onNextDate,
                onDatePicked = onDatePicked,
                prevTag = "kqxs_prev_button",
                nextTag = "kqxs_next_button",
                pickerTag = "kqxs_date_picker"
            )
        }

        val draw = state.currentKqxsDraw
        if (draw != null) {
            item {
                PrizeResultBoardCard(draw = draw, profile = state.selectedProfile)
            }
            val stats = state.dailyKqxsStats
            if (stats != null) {
                item {
                    DailyStatsCard(stats = stats, profile = state.selectedProfile)
                }
                if (state.selectedProfile.id == "xsmb") {
                    item {
                        HeadTailDistributionCard(stats = stats)
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Chưa có dữ liệu cho ngày này.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrizeResultBoardCard(
    draw: LotteryDraw,
    profile: LotteryProfile
) {
    val colors = LocalAnalyticsColors.current
    val xsmbRows = listOf(
        "special" to "Đặc Biệt",
        "prize1" to "Giải Nhất",
        "prize2" to "Giải Nhì",
        "prize3" to "Giải Ba",
        "prize4" to "Giải Tư",
        "prize5" to "Giải Năm",
        "prize6" to "Giải Sáu",
        "prize7" to "Giải Bảy"
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bảng Kết Quả ${draw.date.dayOfWeekVn()}, ${draw.date.formatVn()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.goldAccent
                    )
                    if (profile.id == "xsmb") {
                        Text(
                            text = "${draw.xsmbStation.displayName} (${draw.xsmbStation.code})",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.neutralBlue
                        )
                    }
                }
                if (draw.drawId.isNotBlank()) {
                    Text(
                        text = "Kỳ #${draw.drawId}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.mutedText
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            if (profile.id == "xsmb") {
                xsmbRows.forEachIndexed { idx, (key, title) ->
                    val values = draw.prizeMap[key].orEmpty()
                    if (values.isNotEmpty()) {
                        val isSpecial = key == "special"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSpecial) colors.specialCrimson else colors.mutedText,
                                fontWeight = if (isSpecial) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.width(84.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                values.forEach { numStr ->
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = if (isSpecial) colors.specialCrimsonContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSpecial) BorderStroke(1.dp, colors.specialCrimson) else null
                                    ) {
                                        Text(
                                            text = numStr,
                                            style = if (isSpecial) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                                            color = if (isSpecial) colors.specialCrimson else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (idx < xsmbRows.lastIndex) {
                            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.4f))
                        }
                    }
                }
            } else {
                draw.prizeMap.entries.forEach { (title, values) ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(title, style = MaterialTheme.typography.labelMedium, color = colors.mutedText)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isSp = title.contains("đặc biệt", ignoreCase = true)
                            values.forEach { v ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isSp) colors.specialCrimsonContainer else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = v,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSp) colors.specialCrimson else colors.goldAccent,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
private fun DailyStatsCard(stats: DailyKqxsStats, profile: LotteryProfile) {
    val colors = LocalAnalyticsColors.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Text("Thống Kê Nhanh Trong Ngày", style = MaterialTheme.typography.titleMedium)

            val multiText = if (stats.multiHits.isEmpty()) "Không có"
            else stats.multiHits.joinToString(" · ") { "${profile.formatNumber(it.first)} (${it.second} nháy)" }
            StatLine("Lô về nhiều nháy", multiText, colors.hitGreen)

            val roiText = if (stats.loRoi.isEmpty()) "Không có"
            else stats.loRoi.joinToString(", ") { profile.formatNumber(it) }
            StatLine("Lô rơi từ kỳ trước", roiText, colors.neutralBlue)

            val ganVeText = if (stats.ganDaVe.isEmpty()) "Không có số gan ≥10 ngày về hôm nay"
            else stats.ganDaVe.joinToString(" · ") { "${profile.formatNumber(it.first)} (${it.second}N)" }
            StatLine("Số gan đã nổ", ganVeText, colors.goldAccent)

            val topGanText = stats.topGanLi.joinToString(" · ") { "${profile.formatNumber(it.first)} (${it.second}N)" }
            StatLine("Top Gan Lì hiện tại", topGanText, colors.specialCrimson)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    val colors = LocalAnalyticsColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HeadTailDistributionCard(stats: DailyKqxsStats) {
    val colors = LocalAnalyticsColors.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Text("Bảng Đầu – Đuôi Lô Tô (0–9)", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Đầu", style = MaterialTheme.typography.labelMedium, color = colors.goldAccent, modifier = Modifier.width(36.dp))
                Text("Đuôi xuất hiện", style = MaterialTheme.typography.labelMedium, color = colors.mutedText)
            }
            HorizontalDivider()
            for (d in 0..9) {
                val tails = stats.headCounts[d].orEmpty()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Text(
                        text = "$d",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.goldAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                    Text(
                        text = if (tails.isEmpty()) "Câm" else tails.joinToString(", ") { "$d$it" },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (tails.isEmpty()) colors.specialCrimson else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeepAnalysisContent(
    state: MainUiState,
    onRunDeepAnalysis: (String, Int?) -> Unit
) {
    val colors = LocalAnalyticsColors.current
    var query by remember { mutableStateOf("68, 86") }
    var lookbackDays by remember { mutableStateOf<Int?>(365) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Nhập 1 hoặc nhiều số (VD: 68 hoặc 68, 86)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deep_analysis_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(null to "Toàn bộ", 365 to "1 Năm", 90 to "90 Ngày", 30 to "30 Ngày").forEach { (days, label) ->
                        FilterChip(
                            selected = lookbackDays == days,
                            onClick = { lookbackDays = days },
                            label = { Text(label) }
                        )
                    }
                }

                Button(
                    onClick = { onRunDeepAnalysis(query, lookbackDays) },
                    enabled = !state.isAnalyzingDeep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("run_deep_analysis_button")
                ) {
                    if (state.isAnalyzingDeep) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Phân Tích Chuyên Sâu")
                }
            }
        }

        val res = state.deepAnalysisResult
        if (res != null) {
            item {
                DeepAnalysisSummaryCard(result = res, profile = state.selectedProfile)
            }
            items(res.yearBreakdown, key = { it.year }) { yearItem ->
                var expanded by remember { mutableStateOf(false) }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Năm ${yearItem.year} · ${yearItem.totalHits} lần về",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                        AnimatedVisibility(visible = expanded) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                yearItem.months.forEach { m ->
                                    Text(
                                        text = "Tháng %02d: %d nháy (%s)".format(
                                            m.month,
                                            m.totalHits,
                                            m.drawDatesWithHit.take(6).joinToString(", ") { "${it.first.dayOfMonth}/${it.first.monthValue}" }
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
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
private fun DeepAnalysisSummaryCard(
    result: DeepAnalysisResult,
    profile: LotteryProfile
) {
    val colors = LocalAnalyticsColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Kết Quả Phân Tích: ${result.queriedNumbers.joinToString(", ") { profile.formatNumber(it) }}",
            style = MaterialTheme.typography.titleMedium,
            color = colors.goldAccent
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricSummaryTile(
                title = "Tổng số lần về",
                value = "${result.totalHitCount} nháy",
                subtitle = "${result.drawsWithHits}/${result.totalDrawsAnalyzed} kỳ",
                accentColor = colors.hitGreen,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryTile(
                title = "Tỷ lệ kỳ có mặt",
                value = "%.1f%%".format(result.hitPercentage),
                subtitle = "TB %.1f kỳ/lần".format(result.avgDrawsPerHit),
                accentColor = colors.neutralBlue,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricSummaryTile(
                title = "Gan cực đại",
                value = "${result.maxGap} kỳ",
                subtitle = result.maxGapRange,
                accentColor = colors.specialCrimson,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryTile(
                title = "Gan hiện tại",
                value = "${result.currentGap} kỳ",
                subtitle = "Về ĐB: ${result.specialHits} lần",
                accentColor = colors.goldAccent,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricSummaryTile(
                title = "Chuỗi liên tiếp Max",
                value = "${result.maxStreak} kỳ",
                subtitle = result.maxStreakRange,
                accentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryTile(
                title = "Max nháy / 1 kỳ",
                value = "${result.maxHitsInOneDraw} nháy",
                subtitle = result.maxHitsDates.firstOrNull()?.formatVn() ?: "--",
                accentColor = colors.hitGreen,
                modifier = Modifier.weight(1f)
            )
        }
        if (result.topCoOccurring.isNotEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Top 5 số hay về cùng", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = result.topCoOccurring.joinToString("  ·  ") {
                            "${profile.formatNumber(it.first)} (${it.second} lần)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.goldAccent
                    )
                }
            }
        }
    }
}
