package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ThirtyDayAccuracyChartData
import com.example.ui.theme.LocalAnalyticsColors
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class AccuracyChartMode(val label: String, val subtitle: String) {
    ROLLING_7("Xu hướng 7 kỳ (%)", "Tỷ lệ trúng trượt trên cửa sổ 7 kỳ gần nhất tại mỗi mốc ngày"),
    CUMULATIVE("Tích lũy 30 kỳ (%)", "Tỷ lệ chính xác tích lũy từ đầu chuỗi 30 kỳ đến từng ngày")
}

private val shortDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThirtyDayAccuracyChartCard(
    chartData: ThirtyDayAccuracyChartData?,
    isLoading: Boolean,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAnalyticsColors.current
    var chartMode by rememberSaveable { mutableStateOf(AccuracyChartMode.ROLLING_7) }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(chartData, chartMode) {
        val pts = chartData?.points.orEmpty()
        if (pts.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    if (chartMode == AccuracyChartMode.ROLLING_7) {
                        series(pts.map { it.rolling7Top10Pct })
                        series(pts.map { it.rolling7Top5Pct })
                        series(pts.map { it.rolling7Top3Pct })
                    } else {
                        series(pts.map { it.cumulativeTop10Pct })
                        series(pts.map { it.cumulativeTop5Pct })
                        series(pts.map { it.cumulativeTop3Pct })
                    }
                }
            }
        }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.hitGreen.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("thirty_day_accuracy_chart_card")
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
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = "Biểu đồ độ chính xác 30 ngày",
                        tint = colors.hitGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Biểu Đồ Độ Chính Xác 30 Kỳ",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = chartData?.let {
                                "${it.stationLabel} · ${it.points.size} kỳ gần nhất (Vico M3)"
                            } ?: "Đang tính toán chuỗi độ chính xác 30 ngày...",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (chartData != null && chartData.points.isNotEmpty()) {
                val points = chartData.points

                // Mode switcher
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AccuracyChartMode.entries.forEach { mode ->
                        FilterChip(
                            selected = chartMode == mode,
                            onClick = { chartMode = mode },
                            label = { Text(mode.label, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.testTag("chart_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                Text(
                    text = chartMode.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )

                // Summary KPI Tiles for the 30-day window
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricSummaryTile(
                        title = "Top 3 (30 kỳ)",
                        value = "%.1f%%".format(chartData.overallTop3Pct),
                        subtitle = "${chartData.top3Hits}/${points.size} kỳ trúng",
                        accentColor = colors.hitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryTile(
                        title = "Top 5 (30 kỳ)",
                        value = "%.1f%%".format(chartData.overallTop5Pct),
                        subtitle = "${chartData.top5Hits}/${points.size} kỳ trúng",
                        accentColor = colors.goldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryTile(
                        title = "Top 10 (30 kỳ)",
                        value = "%.1f%%".format(chartData.overallTop10Pct),
                        subtitle = "${chartData.top10Hits}/${points.size} kỳ trúng",
                        accentColor = colors.neutralBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Series Color Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LegendItem(color = colors.hitGreen, label = "Top 3 (%)")
                    LegendItem(color = colors.goldAccent, label = "Top 5 (%)")
                    LegendItem(color = colors.neutralBlue, label = "Top 10 (%)")
                }

                // Native Vico Cartesian Line Chart
                val yFormatter = remember {
                    CartesianValueFormatter { _, value, _ -> "${value.toInt()}%" }
                }
                val xFormatter = remember(points) {
                    CartesianValueFormatter { _, value, _ ->
                        val idx = value.toInt().coerceIn(0, points.lastIndex)
                        points[idx].date.format(shortDateFmt)
                    }
                }
                val rangeProvider = remember {
                    CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = 100.0)
                }
                val marker = rememberDefaultCartesianMarker(
                    label = rememberTextComponent(
                        color = MaterialTheme.colorScheme.onSurface,
                        textSize = 11.sp
                    )
                )

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(
                                    fill = LineCartesianLayer.LineFill.single(fill(colors.neutralBlue)),
                                    areaFill = LineCartesianLayer.AreaFill.single(fill(colors.neutralBlue.copy(alpha = 0.12f))),
                                    pointConnector = LineCartesianLayer.PointConnector.cubic()
                                ),
                                LineCartesianLayer.rememberLine(
                                    fill = LineCartesianLayer.LineFill.single(fill(colors.goldAccent)),
                                    areaFill = LineCartesianLayer.AreaFill.single(fill(colors.goldAccent.copy(alpha = 0.10f))),
                                    pointConnector = LineCartesianLayer.PointConnector.cubic()
                                ),
                                LineCartesianLayer.rememberLine(
                                    fill = LineCartesianLayer.LineFill.single(fill(colors.hitGreen)),
                                    areaFill = LineCartesianLayer.AreaFill.single(fill(colors.hitGreen.copy(alpha = 0.16f))),
                                    pointConnector = LineCartesianLayer.PointConnector.cubic()
                                )
                            ),
                            rangeProvider = rangeProvider
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            valueFormatter = yFormatter
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = xFormatter,
                            itemPlacer = remember {
                                HorizontalAxis.ItemPlacer.aligned(spacing = { 5 })
                            }
                        ),
                        marker = marker
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .testTag("vico_30day_accuracy_chart")
                )

                // Interactive 30-day hit/miss pill timeline
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nhịp trúng 30 kỳ (chạm ô ngày để xem bảng điểm):",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.mutedText
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        points.forEach { pt ->
                            val isSelected = pt.date == selectedDate
                            val tileColor = when {
                                pt.specialHitTop10 -> colors.specialCrimson
                                pt.hitTop3 -> colors.hitGreen
                                pt.hitTop5 -> colors.goldAccent
                                pt.hitTop10 -> colors.neutralBlue
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val textColor = when {
                                pt.specialHitTop10 || pt.hitTop3 -> MaterialTheme.colorScheme.onPrimary
                                pt.hitTop5 || pt.hitTop10 -> MaterialTheme.colorScheme.onPrimary
                                else -> colors.mutedText
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = tileColor.copy(alpha = if (pt.hitTop10 || pt.specialHitTop10) 0.9f else 0.7f),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface) else null,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("chart_day_dot_${pt.date}")
                                    .clickable { onSelectDate(pt.date) }
                            ) {
                                Text(
                                    text = "%02d".format(pt.date.dayOfMonth),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (pt.hitTop3) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = colors.hitGreen, label = "Trúng Top 3")
                        LegendItem(color = colors.goldAccent, label = "Top 5")
                        LegendItem(color = colors.neutralBlue, label = "Top 10")
                        LegendItem(color = colors.specialCrimson, label = "Nổ ĐB")
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    val colors = LocalAnalyticsColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.mutedText
        )
    }
}
