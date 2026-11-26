package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.BetStrategy
import com.example.data.TrialBetType
import com.example.ui.MainUiState
import com.example.ui.components.MetricSummaryTile
import com.example.ui.components.formatVn
import com.example.ui.components.formatVnd
import com.example.ui.theme.LocalAnalyticsColors
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrialPlayScreen(
    state: MainUiState,
    onToggleTrialAlgo: (String) -> Unit,
    onAddStrategy: (BetStrategy) -> Unit,
    onRemoveStrategy: (String) -> Unit,
    onRunSimulation: (Double, Int?, LocalDate?, LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAnalyticsColors.current
    var capitalInput by remember { mutableStateOf("10000000") }
    var drawsCount by remember { mutableStateOf(30) }
    var selectedBetType by remember { mutableStateOf(TrialBetType.LO) }
    var positionsInput by remember { mutableStateOf("1, 2, 3") }
    var pointsInput by remember { mutableStateOf("5") }
    var costPerPointInput by remember { mutableStateOf(TrialBetType.LO.defaultCostPerPoint.toLong().toString()) }
    var winPerPointInput by remember { mutableStateOf(TrialBetType.LO.defaultWinPerPoint.toLong().toString()) }

    Box(contentAlignment = Alignment.TopCenter, modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
        ) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text("Thiết Lập Vốn & Chu Kỳ Chơi Thử", style = MaterialTheme.typography.titleMedium)

                        OutlinedTextField(
                            value = capitalInput,
                            onValueChange = { capitalInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Vốn ban đầu (VNĐ)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("trial_capital_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(
                                5_000_000L to "5 Tr",
                                10_000_000L to "10 Tr",
                                50_000_000L to "50 Tr"
                            ).forEach { (amt, label) ->
                                OutlinedButton(
                                    onClick = { capitalInput = amt.toString() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label)
                                }
                            }
                        }

                        Text("Số kỳ quay giả lập gần nhất:", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 30, 60, 90).forEach { count ->
                                FilterChip(
                                    selected = drawsCount == count,
                                    onClick = { drawsCount = count },
                                    label = { Text("$count kỳ") }
                                )
                            }
                        }

                        Text("Thuật toán tham gia:", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.algorithms.forEach { algo ->
                                FilterChip(
                                    selected = algo.id in state.trialSelectedAlgoIds,
                                    onClick = { onToggleTrialAlgo(algo.id) },
                                    label = { Text(algo.name, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }
                }
            }

            // Bet Strategy Builder
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text("Chiến Lược Đặt Cược (Theo Hạng Dự Đoán)", style = MaterialTheme.typography.titleMedium)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TrialBetType.entries.forEach { betType ->
                                FilterChip(
                                    selected = selectedBetType == betType,
                                    onClick = {
                                        selectedBetType = betType
                                        costPerPointInput = betType.defaultCostPerPoint.toLong().toString()
                                        winPerPointInput = betType.defaultWinPerPoint.toLong().toString()
                                        positionsInput = when (betType.requiredPositions) {
                                            2 -> "1, 2"
                                            3 -> "1, 2, 3"
                                            4 -> "1, 2, 3, 4"
                                            else -> "1, 2, 3"
                                        }
                                    },
                                    label = { Text(betType.label) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = positionsInput,
                                onValueChange = { positionsInput = it },
                                label = { Text("Vị trí hạng (VD: 1, 2)") },
                                singleLine = true,
                                modifier = Modifier.weight(1.3f)
                            )
                            OutlinedTextField(
                                value = pointsInput,
                                onValueChange = { pointsInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Số điểm") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.7f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = costPerPointInput,
                                onValueChange = { costPerPointInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Giá/1 điểm (đ)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = winPerPointInput,
                                onValueChange = { winPerPointInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Thắng/1 điểm (đ)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val parsedPos = positionsInput.split(",", " ", "-")
                                    .mapNotNull { it.trim().toIntOrNull() }
                                    .filter { it in 1..20 }
                                    .distinct()
                                val pts = pointsInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                val cost = costPerPointInput.toDoubleOrNull()?.coerceAtLeast(100.0)
                                    ?: selectedBetType.defaultCostPerPoint
                                val win = winPerPointInput.toDoubleOrNull()?.coerceAtLeast(100.0)
                                    ?: selectedBetType.defaultWinPerPoint

                                val validPos = if (selectedBetType.requiredPositions > 1) {
                                    parsedPos.take(selectedBetType.requiredPositions)
                                } else parsedPos

                                if (validPos.size >= selectedBetType.requiredPositions) {
                                    onAddStrategy(
                                        BetStrategy(
                                            id = UUID.randomUUID().toString(),
                                            type = selectedBetType,
                                            positions = validPos,
                                            points = pts,
                                            costPerPoint = cost,
                                            winPerPoint = win
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("trial_add_strategy_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Thêm Lệnh Cược Vào Danh Sách")
                        }

                        if (state.trialStrategies.isNotEmpty()) {
                            HorizontalDivider()
                            state.trialStrategies.forEach { strat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${strat.type.label} · Top ${strat.positions.joinToString(", ")}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${strat.points} điểm · Chi ${formatVnd(strat.costPerPoint)}/đ · Ăn ${formatVnd(strat.winPerPoint)}/đ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.mutedText
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveStrategy(strat.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Xóa lệnh cược", tint = colors.specialCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val cap = capitalInput.toDoubleOrNull() ?: 10_000_000.0
                        onRunSimulation(cap, drawsCount, null, null)
                    },
                    enabled = !state.isSimulatingTrial,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("run_trial_simulation_button")
                ) {
                    if (state.isSimulatingTrial) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Chạy Giả Lập Chơi Thử")
                }

                if (state.isSimulatingTrial) {
                    Spacer(Modifier.height(8.dp))
                    val (done, total) = state.trialProgress
                    val frac = if (total > 0) done.toFloat() / total else 0f
                    LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth())
                }
            }

            val summary = state.simulationSummary
            if (summary != null) {
                item {
                    val profitColor = if (summary.netProfit >= 0) colors.hitGreen else colors.specialCrimson
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tổng Kết Giả Lập (${summary.totalDays} kỳ)", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricSummaryTile(
                                title = "Số dư cuối",
                                value = formatVnd(summary.finalBalance),
                                subtitle = "Vốn: ${formatVnd(summary.initialCapital)}",
                                accentColor = profitColor,
                                modifier = Modifier.weight(1f)
                            )
                            MetricSummaryTile(
                                title = "Lãi / Lỗ (ROI)",
                                value = "%+.1f%%".format(summary.roiPercent),
                                subtitle = formatVnd(summary.netProfit),
                                accentColor = profitColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricSummaryTile(
                                title = "Kỳ có lãi / lỗ",
                                value = "${summary.winDays} / ${summary.lossDays}",
                                accentColor = colors.neutralBlue,
                                modifier = Modifier.weight(1f)
                            )
                            MetricSummaryTile(
                                title = "Tổng thu / chi",
                                value = formatVnd(summary.totalWin),
                                subtitle = "Chi: ${formatVnd(summary.totalCost)}",
                                accentColor = colors.goldAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                items(summary.logs) { log ->
                    val net = log.win - log.cost
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Kỳ ${log.actualDate.formatVn()}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = (if (net >= 0) "+" else "") + formatVnd(net),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (net >= 0) colors.hitGreen else colors.specialCrimson,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            log.details.forEach { line ->
                                Text(line, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            }
                            Text(
                                text = "Số dư sau kỳ: ${formatVnd(log.balanceAfter)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.goldAccent
                            )
                        }
                    }
                }
            }
        }
    }
}
