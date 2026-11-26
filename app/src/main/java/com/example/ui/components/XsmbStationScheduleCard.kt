package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.StationDailyPrediction
import com.example.data.XsmbStation
import com.example.ui.theme.LocalAnalyticsColors
import java.time.LocalDate

@Composable
fun XsmbStationScheduleCard(
    selectedStation: XsmbStation,
    todayStation: XsmbStation,
    useStationOnlyHistory: Boolean,
    stationRecordCount: Int,
    totalXsmbCount: Int,
    selectedPredictDate: LocalDate,
    schedulePredictions: List<StationDailyPrediction>,
    isLoadingSchedule: Boolean,
    onSelectStation: (XsmbStation) -> Unit,
    onToggleStationOnlyHistory: (Boolean) -> Unit,
    onSelectScheduleDay: (StationDailyPrediction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAnalyticsColors.current

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            // Title Row
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
                        imageVector = Icons.Default.LocationCity,
                        contentDescription = "Phân loại đài XSMB",
                        tint = colors.goldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Phân Loại Theo Đài & Lịch Sổ XSMB",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.goldAccent
                        )
                        Text(
                            text = "Hôm nay mở thưởng: ${todayStation.displayName} (${todayStation.code})",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText
                        )
                    }
                }
            }

            // Station selector chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                XsmbStation.entries.forEach { station ->
                    val isSelected = selectedStation == station
                    val isTodayStation = station == todayStation
                    val chipText = when {
                        station == XsmbStation.ALL -> "Tất cả (7 ngày)"
                        isTodayStation -> "${station.shortName} · Hôm nay"
                        else -> station.shortName
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectStation(station) },
                        label = {
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected || isTodayStation) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.goldAccent.copy(alpha = 0.2f),
                            selectedLabelColor = colors.goldAccent
                        ),
                        modifier = Modifier.testTag("station_chip_${station.id}")
                    )
                }
            }

            // Active station schedule detail + station-history switch
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedStation.displayName} · ${selectedStation.scheduleLabel}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val historyCaption = if (selectedStation == XsmbStation.ALL) {
                            "Tổng hợp toàn bộ $totalXsmbCount kỳ quay XSMB từ 2005 đến nay"
                        } else if (useStationOnlyHistory) {
                            "Đang phân tích riêng $stationRecordCount kỳ của ${selectedStation.shortName}"
                        } else {
                            "Đang phân tích trên toàn bộ $totalXsmbCount kỳ XSMB (lịch quay ${selectedStation.shortName})"
                        }
                        Text(
                            text = historyCaption,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText
                        )
                    }
                    if (selectedStation != XsmbStation.ALL) {
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Riêng đài",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.mutedText
                            )
                            Switch(
                                checked = useStationOnlyHistory,
                                onCheckedChange = onToggleStationOnlyHistory,
                                modifier = Modifier.testTag("station_only_history_switch")
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.45f))

            // 7-day weekly schedule predictions by station
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = colors.neutralBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Dự Đoán 7 Ngày Theo Lịch Sổ Từng Đài",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = "Chạm để xem chi tiết",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText
                )
            }

            if (isLoadingSchedule) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (schedulePredictions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    schedulePredictions.forEach { item ->
                        val isTargetDay = item.date == selectedPredictDate
                        val isStationHighlighted = selectedStation == XsmbStation.ALL || selectedStation == item.station
                        val border = when {
                            isTargetDay -> colors.goldAccent
                            item.isTodayDraw -> colors.neutralBlue.copy(alpha = 0.65f)
                            else -> colors.cardBorder
                        }
                        val bg = when {
                            isTargetDay -> colors.goldAccent.copy(alpha = 0.1f)
                            isStationHighlighted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = bg,
                            border = BorderStroke(if (isTargetDay) 1.5.dp else 1.dp, border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("schedule_day_${item.station.id}_${item.date}")
                                .clickable { onSelectScheduleDay(item) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${item.date.dayOfWeekVn()} · ${item.date.formatVn()}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isTargetDay) colors.goldAccent else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (item.isTodayDraw) {
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = if (item.isLiveWindow) colors.specialCrimsonContainer else colors.hitGreenContainer
                                            ) {
                                                Text(
                                                    text = if (item.isLiveWindow) "Đang quay" else "Hôm nay",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (item.isLiveWindow) colors.specialCrimson else colors.hitGreen,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${item.station.displayName} (${item.station.code}) · ${item.stationHistoryCount} kỳ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    item.top3.forEachIndexed { idx, (num, _) ->
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = if (idx == 0) colors.goldAccent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                1.dp,
                                                if (idx == 0) colors.goldAccent else colors.cardBorder
                                            )
                                        ) {
                                            Text(
                                                text = "%02d".format(num),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (idx == 0) colors.goldAccent else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
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
    }
}
