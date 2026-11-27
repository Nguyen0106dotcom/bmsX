package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LotteryProfile
import com.example.data.LotteryProfiles
import com.example.data.NumberDetailInfo
import com.example.data.PredictionResult
import com.example.data.SortOrder
import com.example.data.StationDailyPrediction
import com.example.data.XsmbStation
import com.example.ui.MainUiState
import com.example.ui.components.DateNavigatorCard
import com.example.ui.components.HistoricalVerificationCard
import com.example.ui.components.Last7DaysComparisonCard
import com.example.ui.components.MetricSummaryTile
import com.example.ui.components.ProfileSelectorChip
import com.example.ui.components.ThirtyDayAccuracyChartCard
import com.example.ui.components.XsmbPerformanceDashboardCard
import com.example.ui.components.XsmbStationScheduleCard
import com.example.ui.components.dayOfWeekVn
import com.example.ui.components.formatVn
import com.example.ui.theme.LocalAnalyticsColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val vnTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val drawHmFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PredictScreen(
    state: MainUiState,
    onSelectProfile: (LotteryProfile) -> Unit,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    onRunPrediction: () -> Unit,
    onPredictNextRealTime: () -> Unit,
    onJumpToPastDraw: (Int) -> Unit,
    onSortChanged: (SortOrder) -> Unit,
    onInspectNumber: (Int) -> Unit,
    onDismissDetail: () -> Unit,
    onSyncCurrent: () -> Unit,
    onSelectVerifyPresetDraws: (Int) -> Unit,
    onRunCombinedBacktest: () -> Unit,
    onSelectXsmbStation: (XsmbStation) -> Unit,
    onToggleStationOnlyHistory: (Boolean) -> Unit,
    onSelectScheduleDay: (StationDailyPrediction) -> Unit,
    onFetchSevenDays: () -> Unit,
    onSelectDashboardWindow: (Int) -> Unit,
    onUpdateXsmbData: () -> Unit,
    onOpenKqxsForDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAnalyticsColors.current
    var showBacktestPanel by rememberSaveable { mutableStateOf(true) }
    val isXsmb = state.selectedProfile.id == "xsmb"

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
        ) {
            // 1. Hero Analytics Card
            item {
                HeroStatusBanner(
                    state = state,
                    onSelectProfile = onSelectProfile,
                    onSyncCurrent = onSyncCurrent
                )
            }

            // 1.5 Real-Time Clock & Next Scheduled Draw Banner
            item {
                RealTimeNextDrawCard(
                    state = state,
                    onPredictNextRealTime = onPredictNextRealTime
                )
            }

            // 1.75 XSMB Station Classification & 7-Day Station Schedule Predictions
            if (isXsmb) {
                item {
                    val todayStation = XsmbStation.forDate(state.currentVnDateTime.toLocalDate())
                    val stationCount = if (state.selectedXsmbStation == XsmbStation.ALL) {
                        state.draws.size
                    } else {
                        state.draws.count { state.selectedXsmbStation.matchesDate(it.date) }
                    }
                    XsmbStationScheduleCard(
                        selectedStation = state.selectedXsmbStation,
                        todayStation = todayStation,
                        useStationOnlyHistory = state.useStationOnlyHistory,
                        stationRecordCount = stationCount,
                        totalXsmbCount = state.draws.size,
                        selectedPredictDate = state.predictTargetDate,
                        schedulePredictions = state.stationSchedulePredictions,
                        isLoadingSchedule = state.isLoadingSchedulePredictions,
                        onSelectStation = onSelectXsmbStation,
                        onToggleStationOnlyHistory = onToggleStationOnlyHistory,
                        onSelectScheduleDay = onSelectScheduleDay
                    )
                }
            }

            // 2. Date Selector + Quick Past-Draw Verification Chips + Run CTA
            item {
                val targetStationTag = if (isXsmb) {
                    " · ${XsmbStation.forDate(state.predictTargetDate).shortName}"
                } else {
                    ""
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateNavigatorCard(
                        date = state.predictTargetDate,
                        label = if (state.predictTargetDate == state.nextScheduledDraw.targetDate) {
                            "Kỳ tiếp theo (${state.selectedProfile.shortName}$targetStationTag)"
                        } else if (state.predictionResult?.actualDraw != null) {
                            "Kiểm chứng kỳ trước (${state.selectedProfile.shortName}$targetStationTag)"
                        } else {
                            "Ngày dự đoán (${state.selectedProfile.shortName}$targetStationTag)"
                        },
                        onPrevious = onPrevDate,
                        onNext = onNextDate,
                        onDatePicked = onDatePicked,
                        prevTag = "predict_prev_date_button",
                        nextTag = "predict_next_date_button",
                        pickerTag = "predict_date_picker"
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Kỳ trước:",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.mutedText
                        )
                        FilterChip(
                            selected = state.predictTargetDate == state.nextScheduledDraw.targetDate,
                            onClick = onPredictNextRealTime,
                            label = { Text("Kỳ tới", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.testTag("quick_jump_next")
                        )
                        val pastPool = remember(state.draws, state.selectedProfile.id, state.selectedXsmbStation) {
                            if (isXsmb && state.selectedXsmbStation != XsmbStation.ALL) {
                                state.draws.filter { state.selectedXsmbStation.matchesDate(it.date) }
                            } else {
                                state.draws
                            }
                        }
                        listOf(
                            0 to "Gần nhất có KQ",
                            1 to "1 kỳ trước",
                            3 to "3 kỳ trước",
                            7 to "7 kỳ trước",
                            14 to "14 kỳ trước",
                            30 to "30 kỳ trước"
                        ).forEach { (offset, label) ->
                            val offsetDate = if (pastPool.isNotEmpty()) {
                                pastPool[(pastPool.lastIndex - offset).coerceIn(0, pastPool.lastIndex)].date
                            } else null
                            FilterChip(
                                selected = offsetDate != null && state.predictTargetDate == offsetDate,
                                onClick = { onJumpToPastDraw(offset) },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.testTag("quick_jump_past_$offset")
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onRunPrediction,
                        enabled = !state.isPredicting && state.draws.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("run_prediction_button")
                    ) {
                        if (state.isPredicting) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.run_prediction))
                    }

                    OutlinedButton(
                        onClick = {
                            val nextVisible = !showBacktestPanel
                            showBacktestPanel = nextVisible
                            if (nextVisible && state.combinedPerformance == null) {
                                onSelectVerifyPresetDraws(30)
                            }
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("toggle_backtest_button")
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        val verifiedSuffix = state.combinedPerformance?.let { " (${it.totalDays})" }.orEmpty()
                        Text("Kiểm Chứng$verifiedSuffix")
                        Icon(
                            if (showBacktestPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                }
            }

            // 3. Top 3 Golden Picks & Win Evaluation for current targetDate
            val prediction = state.predictionResult
            if (prediction != null) {
                item {
                    TopPicksShowcaseCard(prediction = prediction, profile = state.selectedProfile)
                }

                if (prediction.winEvaluation != null && prediction.actualDraw != null) {
                    item {
                        WinComparisonBanner(
                            prediction = prediction,
                            profile = state.selectedProfile,
                            onOpenKqxsForDate = { onOpenKqxsForDate(prediction.targetDate) }
                        )
                    }
                }

                // 3.5 Dedicated XSMB Accuracy Percentage Performance Dashboard
                item {
                    XsmbPerformanceDashboardCard(
                        dashboard = state.xsmbDashboard,
                        isLoading = state.isLoadingXsmbDashboard,
                        isSyncing = state.isSyncing,
                        activeProfileId = state.selectedProfile.id,
                        activeProfileShortName = state.selectedProfile.shortName,
                        targetDate = state.predictTargetDate,
                        selectedWindowDraws = state.xsmbDashboardWindow,
                        selectedStation = state.selectedXsmbStation,
                        onSwitchToXsmb = { onSelectProfile(LotteryProfiles.XSMB) },
                        onSelectWindowDraws = onSelectDashboardWindow,
                        onSelectStation = { station ->
                            if (!isXsmb) onSelectProfile(LotteryProfiles.XSMB)
                            onSelectXsmbStation(station)
                        },
                        onUpdateXsmbData = onUpdateXsmbData
                    )
                }

                // 4. Predictions & actual outcomes of previous draws prior to current targetDate
                item {
                    AnimatedVisibility(visible = showBacktestPanel) {
                        HistoricalVerificationCard(
                            profile = state.selectedProfile,
                            targetDate = state.predictTargetDate,
                            fromDate = state.combinedFromDate,
                            toDate = state.combinedToDate,
                            activePresetCount = state.verifyPresetDrawCount,
                            isEvaluating = state.isEvaluatingCombined,
                            stats = state.combinedPerformance,
                            selectedPredictDate = state.predictTargetDate,
                            onSelectPresetDraws = onSelectVerifyPresetDraws,
                            onRunVerification = onRunCombinedBacktest,
                            onInspectPastDraw = onDatePicked,
                            onOpenKqxsForDate = onOpenKqxsForDate
                        )
                    }
                }

                // 4.5 Last 7 Days XSMB Public API Results vs Predictions Comparison
                if (isXsmb) {
                    item {
                        Last7DaysComparisonCard(
                            summary = state.sevenDayComparison,
                            isLoading = state.isLoadingSevenDays,
                            selectedStationFilter = state.selectedXsmbStation,
                            onFetchFromApi = onFetchSevenDays,
                            onInspectDrawDate = onOpenKqxsForDate
                        )
                    }
                }

                // 4.75 30-Day Accuracy Percentage Data Visualization (Vico M3)
                item {
                    ThirtyDayAccuracyChartCard(
                        chartData = state.thirtyDayAccuracyChart,
                        isLoading = state.isLoadingAccuracyChart,
                        selectedDate = state.predictTargetDate,
                        onSelectDate = onDatePicked
                    )
                }

                // 5. Sort Bar & Number Matrix Header
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Bảng Điểm Chi Tiết (${state.selectedProfile.minVal}–${state.selectedProfile.maxVal})",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Chạm số để xem chi tiết",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedText
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SortOrder.entries.forEach { order ->
                                FilterChip(
                                    selected = state.sortOrder == order,
                                    onClick = { onSortChanged(order) },
                                    label = { Text(order.label, style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.testTag("sort_${order.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // 6. Number Grid Matrix
                item {
                    ScoreNumberMatrixGrid(
                        prediction = prediction,
                        sortOrder = state.sortOrder,
                        profile = state.selectedProfile,
                        onInspectNumber = onInspectNumber
                    )
                }
            } else if (state.isInitializing) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Đang nạp dữ liệu ${state.selectedProfile.shortName}...")
                    }
                }
            }
        }
    }

    val detail = state.selectedNumberDetail
    if (detail != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissDetail,
            sheetState = sheetState
        ) {
            NumberBreakdownSheet(
                detail = detail,
                profile = state.selectedProfile
            )
        }
    }
}

@Composable
private fun RealTimeNextDrawCard(
    state: MainUiState,
    onPredictNextRealTime: () -> Unit
) {
    val colors = LocalAnalyticsColors.current
    val nowVn = state.currentVnDateTime
    val nextDraw = state.nextScheduledDraw
    val isTargetingNext = state.predictTargetDate == nextDraw.targetDate
    val nextDayLabel = if (nextDraw.isTodayDraw) "Hôm nay" else nextDraw.targetDate.dayOfWeekVn()

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (nextDraw.isLiveWindow) colors.specialCrimson.copy(alpha = 0.7f) else colors.neutralBlue.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = colors.neutralBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${nowVn.format(vnTimeFormatter)} · ${nowVn.toLocalDate().dayOfWeekVn()}, ${nowVn.toLocalDate().formatVn()} (VN)",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.neutralBlue
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (nextDraw.isLiveWindow) colors.specialCrimsonContainer else colors.hitGreenContainer
                ) {
                    Text(
                        text = if (nextDraw.isLiveWindow) "Đang quay" else "Còn ${state.countdownText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (nextDraw.isLiveWindow) colors.specialCrimson else colors.hitGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val stationDesc = if (state.selectedProfile.id == "xsmb") {
                        " · ${XsmbStation.forDate(nextDraw.targetDate).displayName}"
                    } else {
                        ""
                    }
                    Text(
                        text = "Kỳ quay tiếp theo (${state.selectedProfile.shortName}$stationDesc)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                    Text(
                        text = "$nextDayLabel, ${nextDraw.targetDate.formatVn()} · ${nextDraw.drawDateTime.format(drawHmFormatter)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.goldAccent
                    )
                }

                OutlinedButton(
                    onClick = onPredictNextRealTime,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("predict_next_realtime_button")
                ) {
                    Icon(Icons.Default.Upcoming, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isTargetingNext) "Cập nhật kỳ tới" else "Về kỳ tới",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (state.dataGapDays > 1 && state.latestDrawDate != null) {
                Text(
                    text = "Dữ liệu lịch sử mới nhất tới ${state.latestDrawDate.formatVn()} (cách kỳ tới ${state.dataGapDays} ngày). Hệ thống tự neo cửa sổ phân tích theo kỳ mới nhất và sẽ tự cập nhật khi đồng bộ được KQ mới.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.warningAmber
                )
            }
        }
    }
}

@Composable
private fun HeroStatusBanner(
    state: MainUiState,
    onSelectProfile: (LotteryProfile) -> Unit,
    onSyncCurrent: () -> Unit
) {
    val colors = LocalAnalyticsColors.current
    Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.hero_analytics_banner_1795703628057),
                contentDescription = stringResource(R.string.hero_banner_desc),
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "THIÊN CƠ SỐ · v6.2",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.goldAccent
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = state.selectedProfile.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    ProfileSelectorChip(
                        selectedProfile = state.selectedProfile,
                        onSelectProfile = onSelectProfile
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = state.selectedProfile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val latestText = state.latestDrawDate?.formatVn() ?: "--/--/----"
                    Text(
                        text = "${state.draws.size} kỳ · Mới nhất: $latestText",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedButton(
                        onClick = onSyncCurrent,
                        enabled = !state.isSyncing,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("hero_sync_button")
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Đồng bộ", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopPicksShowcaseCard(
    prediction: PredictionResult,
    profile: LotteryProfile
) {
    val colors = LocalAnalyticsColors.current
    val actualSet = prediction.actualDraw?.uniqueNumbers.orEmpty()
    val special = prediction.actualDraw?.specialNumber

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = colors.goldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                val stationTitle = if (profile.id == "xsmb") {
                    " · ${XsmbStation.forDate(prediction.targetDate).shortName}"
                } else {
                    ""
                }
                Text(
                    text = "TOP 3 SỐ VÀNG ĐỀ XUẤT (${prediction.targetDate.formatVn()}$stationTitle)",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.goldAccent
                )
            }
            if (prediction.actualDraw != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Kiểm chứng kỳ trước · Chỉ sử dụng dữ liệu lịch sử trước ${prediction.targetDate.formatVn()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.neutralBlue
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                prediction.top3.forEachIndexed { idx, (num, score) ->
                    val isSpecial = special != null && num == special
                    val isHit = num in actualSet
                    val bgColor = when {
                        isSpecial -> colors.specialCrimsonContainer
                        isHit -> colors.hitGreenContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val borderColor = when {
                        isSpecial -> colors.specialCrimson
                        isHit -> colors.hitGreen
                        idx == 0 -> colors.goldAccent
                        else -> colors.cardBorder
                    }
                    val numColor = when {
                        isSpecial -> colors.specialCrimson
                        isHit -> colors.hitGreen
                        else -> colors.goldAccent
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = bgColor,
                        border = BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Hạng #${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.mutedText
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = profile.formatNumber(num),
                                style = MaterialTheme.typography.headlineLarge,
                                color = numColor
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "%.2f đ".format(score),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isSpecial) {
                                Text("★ ĐẶC BIỆT", style = MaterialTheme.typography.labelSmall, color = colors.specialCrimson)
                            } else if (isHit) {
                                Text("✓ VỀ LÔ", style = MaterialTheme.typography.labelSmall, color = colors.hitGreen)
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
private fun WinComparisonBanner(
    prediction: PredictionResult,
    profile: LotteryProfile,
    onOpenKqxsForDate: () -> Unit
) {
    val eval = prediction.winEvaluation ?: return
    val actual = prediction.actualDraw ?: return
    val colors = LocalAnalyticsColors.current
    val bg = if (eval.isWin) colors.hitGreenContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
    val border = if (eval.isWin) colors.hitGreen else colors.specialCrimson.copy(alpha = 0.55f)
    val fg = if (eval.isWin) colors.hitGreen else colors.specialCrimson

    val top10 = remember(prediction) { prediction.rankedNumbers.take(10) }
    val hitCounts = remember(actual) {
        val counts = HashMap<Int, Int>()
        actual.allNumbers.forEach { n -> counts[n] = (counts[n] ?: 0) + 1 }
        counts
    }
    val top3Hits = remember(top10, actual) { top10.take(3).count { it.first in actual.uniqueNumbers } }
    val top5Hits = remember(top10, actual) { top10.take(5).count { it.first in actual.uniqueNumbers } }
    val top10Hits = remember(top10, actual) { top10.count { it.first in actual.uniqueNumbers } }
    val top10Set = remember(top10) { top10.map { it.first }.toSet() }

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
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(1.2.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("past_draw_comparison_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
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
                        imageVector = if (eval.isWin) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = if (eval.isWin) "Dự đoán đúng" else "Dự đoán sai",
                        tint = fg,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        val stationTag = if (profile.id == "xsmb") {
                            " · ${XsmbStation.forDate(prediction.targetDate).displayName}"
                        } else ""
                        Text(
                            text = "Đối chiếu KQ thực tế ${prediction.targetDate.formatVn()}$stationTag",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText
                        )
                        Text(
                            text = eval.prizeDetail,
                            style = MaterialTheme.typography.titleSmall,
                            color = fg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (eval.isWin) colors.hitGreenContainer else colors.specialCrimsonContainer
                ) {
                    Text(
                        text = if (eval.isWin) "✓ ĐÚNG" else "✗ SAI",
                        style = MaterialTheme.typography.labelLarge,
                        color = fg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Accuracy summary strip across Top 3 / 5 / 10
            Text(
                text = "Độ khớp kỳ này: Top 3 trúng $top3Hits/3 · Top 5 trúng $top5Hits/5 · Top 10 trúng $top10Hits/10 số · ĐB thực tế: ${actual.specialNumber?.let { profile.formatNumber(it) } ?: "--"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Predicted Top 10 picks with right/wrong markers
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Top 10 dự đoán của kỳ ${prediction.targetDate.formatVn()}:",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    top10.forEachIndexed { idx, (num, _) ->
                        val isSp = actual.specialNumber != null && num == actual.specialNumber
                        val nhay = hitCounts[num] ?: 0
                        val isHit = nhay > 0
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when {
                                isSp -> colors.specialCrimsonContainer
                                isHit -> colors.hitGreenContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
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
                                else -> " ✗"
                            }
                            Text(
                                text = "#${idx + 1} ${profile.formatNumber(num)}$suffix",
                                style = MaterialTheme.typography.labelMedium,
                                color = when {
                                    isSp -> colors.specialCrimson
                                    isHit -> colors.hitGreen
                                    else -> colors.mutedText
                                },
                                fontWeight = if (isHit) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.5f))

            // Actual Draw Outcomes (Full Prize Table)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Kết quả thực tế đã quay ngày ${prediction.targetDate.formatVn()} (Xanh = trúng trong Top 10):",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.neutralBlue,
                    fontWeight = FontWeight.SemiBold
                )

                if (actual.prizeMap.isNotEmpty()) {
                    val orderedEntries = if (profile.id == "xsmb") {
                        xsmbGroupLabels.mapNotNull { (k, title) ->
                            actual.prizeMap[k]?.takeIf { it.isNotEmpty() }?.let { title to ((k == "special") to it) }
                        }
                    } else {
                        actual.prizeMap.entries.map { (k, vals) ->
                            k to (k.contains("đặc biệt", ignoreCase = true) to vals)
                        }
                    }
                    orderedEntries.forEach { (title, pair) ->
                        val (isSpecialGroup, values) = pair
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSpecialGroup) colors.specialCrimson else colors.mutedText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                values.forEach { str ->
                                    val nVal = str.toIntOrNull()
                                    val matched = nVal != null && nVal in top10Set
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = when {
                                            isSpecialGroup && matched -> colors.specialCrimsonContainer
                                            matched -> colors.hitGreenContainer
                                            isSpecialGroup -> colors.specialCrimsonContainer.copy(alpha = 0.55f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        },
                                        border = if (matched || isSpecialGroup) {
                                            BorderStroke(1.dp, if (isSpecialGroup) colors.specialCrimson else colors.hitGreen)
                                        } else null
                                    ) {
                                        Text(
                                            text = str,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = when {
                                                isSpecialGroup -> colors.specialCrimson
                                                matched -> colors.hitGreen
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = if (matched || isSpecialGroup) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        actual.uniqueNumbers.sorted().forEach { num ->
                            val isSp = actual.specialNumber == num
                            val matched = num in top10Set
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = when {
                                    isSp -> colors.specialCrimsonContainer
                                    matched -> colors.hitGreenContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = profile.formatNumber(num),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        isSp -> colors.specialCrimson
                                        matched -> colors.hitGreen
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onOpenKqxsForDate,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("past_draw_open_kqxs_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Mở bảng KQXS & Thống kê", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ScoreNumberMatrixGrid(
    prediction: PredictionResult,
    sortOrder: SortOrder,
    profile: LotteryProfile,
    onInspectNumber: (Int) -> Unit
) {
    val colors = LocalAnalyticsColors.current
    val orderedItems = remember(prediction, sortOrder) {
        when (sortOrder) {
            SortOrder.HIGH_TO_LOW -> prediction.rankedNumbers
            SortOrder.LOW_TO_HIGH -> prediction.rankedNumbers.reversed()
            SortOrder.CUSTOM -> prediction.scores.entries.sortedBy { it.key }.map { it.key to it.value }
        }
    }
    val top3Set = remember(prediction) { prediction.top3.map { it.first }.toSet() }
    val actualSet = prediction.actualDraw?.uniqueNumbers.orEmpty()
    val specialNum = prediction.actualDraw?.specialNumber

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cols = when {
            maxWidth >= 680.dp -> 10
            maxWidth >= 420.dp -> 5
            else -> 4
        }
        val rows = remember(orderedItems, cols) { orderedItems.chunked(cols) }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowItems.forEach { (num, score) ->
                        val isSpecial = specialNum != null && num == specialNum
                        val isHit = num in actualSet
                        val isTop3 = num in top3Set

                        val bg = when {
                            isSpecial -> colors.specialCrimsonContainer
                            isHit -> colors.hitGreenContainer
                            isTop3 -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val borderColor = when {
                            isSpecial -> colors.specialCrimson
                            isHit -> colors.hitGreen
                            isTop3 -> colors.goldAccent
                            else -> colors.cardBorder
                        }
                        val numColor = when {
                            isSpecial -> colors.specialCrimson
                            isHit -> colors.hitGreen
                            isTop3 -> colors.goldAccent
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = bg,
                            border = BorderStroke(if (isTop3 || isHit || isSpecial) 1.5.dp else 1.dp, borderColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("score_cell_${profile.formatNumber(num)}")
                                .clickable { onInspectNumber(num) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                            ) {
                                Text(
                                    text = profile.formatNumber(num),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = numColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "%.1f".format(score),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.mutedText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    repeat(cols - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberBreakdownSheet(
    detail: NumberDetailInfo,
    profile: LotteryProfile
) {
    val colors = LocalAnalyticsColors.current
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Phân Tích Số ${profile.formatNumber(detail.number)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.goldAccent
                )
                Text(
                    text = "Xếp hạng #${detail.rank} · Tổng điểm %.2f".format(detail.totalScore),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedText
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricSummaryTile(
                title = "30 ngày",
                value = "${detail.freq30Days} lần",
                accentColor = colors.neutralBlue,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryTile(
                title = "100 ngày",
                value = "${detail.freq100Days} lần",
                accentColor = colors.goldAccent,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryTile(
                title = "365 ngày",
                value = "${detail.freq365Days} lần",
                accentColor = colors.hitGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricSummaryTile(
                title = "Lần cuối về",
                value = detail.daysSinceLast?.let { "$it ngày trước" } ?: "Chưa có",
                accentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryTile(
                title = "Chu kỳ TB",
                value = detail.avgIntervalDays?.let { "%.1f ngày".format(it) } ?: "--",
                accentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider()
        Text(
            text = "Đóng góp từng thuật toán (Gốc = 100.00)",
            style = MaterialTheme.typography.titleSmall
        )

        detail.contributions.forEach { contrib ->
            val signColor = if (contrib.weightedDelta >= 0) colors.hitGreen else colors.specialCrimson
            val prefix = if (contrib.weightedDelta >= 0) "+" else ""
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contrib.algorithmName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Điểm thô: %.3f · Trọng số: %.2fx".format(contrib.rawScore, contrib.weight),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                }
                Text(
                    text = "$prefix%.3f".format(contrib.weightedDelta),
                    style = MaterialTheme.typography.labelLarge,
                    color = signColor
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
