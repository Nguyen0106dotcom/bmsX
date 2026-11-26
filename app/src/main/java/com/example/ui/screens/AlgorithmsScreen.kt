package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AlgorithmPreference
import com.example.engine.LotteryAlgorithm
import com.example.ui.MainUiState
import com.example.ui.components.MetricSummaryTile
import com.example.ui.theme.LocalAnalyticsColors
import kotlin.math.round

@Composable
fun AlgorithmsScreen(
    state: MainUiState,
    onToggleEnabled: (String, Boolean) -> Unit,
    onToggleWeight: (String, Boolean) -> Unit,
    onWeightChange: (String, Double) -> Unit,
    onParamChange: (String, String, Double) -> Unit,
    onResetAlgo: (String) -> Unit,
    onOptimizeAlgo: (LotteryAlgorithm) -> Unit,
    onApplyOptimization: () -> Unit,
    onDismissOptimization: () -> Unit,
    onRunPeriodBacktest: (LotteryAlgorithm) -> Unit,
    onGenerateAiBundle: (String, String, String) -> Unit,
    onSaveAiBundle: () -> Unit,
    onDeleteCustomBundle: (String) -> Unit,
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
                    text = { Text("Thuật Toán & Tối Ưu") },
                    modifier = Modifier.testTag("tab_algo_list")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("AI Studio (Gemini)") },
                    modifier = Modifier.testTag("tab_ai_studio")
                )
            }

            if (selectedTab == 0) {
                AlgorithmListContent(
                    state = state,
                    onToggleEnabled = onToggleEnabled,
                    onToggleWeight = onToggleWeight,
                    onWeightChange = onWeightChange,
                    onParamChange = onParamChange,
                    onResetAlgo = onResetAlgo,
                    onOptimizeAlgo = onOptimizeAlgo,
                    onRunPeriodBacktest = onRunPeriodBacktest,
                    onDeleteCustomBundle = onDeleteCustomBundle
                )
            } else {
                AiStudioContent(
                    state = state,
                    onGenerate = onGenerateAiBundle,
                    onSaveBundle = onSaveAiBundle
                )
            }
        }
    }

    val optReport = state.optimizationReport
    if (optReport != null) {
        AlertDialog(
            onDismissRequest = onDismissOptimization,
            title = { Text("Kết Quả Tối Ưu Tham Số") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(optReport.algorithmName, style = MaterialTheme.typography.titleSmall)
                    Text("Đánh giá trên ${optReport.validationDays} kỳ quay gần nhất:")
                    Text("• Trước tối ưu (Top 3): %.1f%%".format(optReport.baselineTop3Rate))
                    Text(
                        "• Sau tối ưu (Top 3): %.1f%%".format(optReport.optimizedTop3Rate),
                        color = LocalAnalyticsColors.current.hitGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(onClick = onApplyOptimization) {
                    Text("Áp Dụng")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissOptimization) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
private fun AlgorithmListContent(
    state: MainUiState,
    onToggleEnabled: (String, Boolean) -> Unit,
    onToggleWeight: (String, Boolean) -> Unit,
    onWeightChange: (String, Double) -> Unit,
    onParamChange: (String, String, Double) -> Unit,
    onResetAlgo: (String) -> Unit,
    onOptimizeAlgo: (LotteryAlgorithm) -> Unit,
    onRunPeriodBacktest: (LotteryAlgorithm) -> Unit,
    onDeleteCustomBundle: (String) -> Unit
) {
    val colors = LocalAnalyticsColors.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.isOptimizing) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Đang tối ưu tham số trên lịch sử...", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.optimizeProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (state.isRunningPeriodBacktest || state.periodBacktestRows.isNotEmpty()) {
            item {
                PeriodBacktestCard(state = state)
            }
        }

        items(state.algorithms, key = { it.id }) { algo ->
            val pref = state.algorithmPrefs[algo.id] ?: AlgorithmPreference(algo.id)
            val isCustom = algo.id.startsWith("bundle_")
            AlgorithmCard(
                algo = algo,
                pref = pref,
                isCustom = isCustom,
                isBusy = state.isOptimizing || state.isRunningPeriodBacktest,
                onToggleEnabled = { onToggleEnabled(algo.id, it) },
                onToggleWeight = { onToggleWeight(algo.id, it) },
                onWeightChange = { onWeightChange(algo.id, it) },
                onParamChange = { k, v -> onParamChange(algo.id, k, v) },
                onReset = { onResetAlgo(algo.id) },
                onOptimize = { onOptimizeAlgo(algo) },
                onRunBacktest = { onRunPeriodBacktest(algo) },
                onDelete = if (isCustom) {
                    { onDeleteCustomBundle(algo.id.removePrefix("bundle_")) }
                } else null
            )
        }
    }
}

@Composable
private fun AlgorithmCard(
    algo: LotteryAlgorithm,
    pref: AlgorithmPreference,
    isCustom: Boolean,
    isBusy: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleWeight: (Boolean) -> Unit,
    onWeightChange: (Double) -> Unit,
    onParamChange: (String, Double) -> Unit,
    onReset: () -> Unit,
    onOptimize: () -> Unit,
    onRunBacktest: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = LocalAnalyticsColors.current
    var expandedParams by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (pref.enabled) colors.goldAccent.copy(alpha = 0.45f) else colors.cardBorder
        ),
        modifier = Modifier.fillMaxWidth()
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = algo.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (pref.enabled) colors.goldAccent else colors.mutedText
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = algo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa thuật toán", tint = colors.specialCrimson)
                    }
                }
                Switch(
                    checked = pref.enabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("switch_algo_${algo.id}")
                )
            }

            // Weight configuration row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = pref.weightEnabled,
                    onCheckedChange = onToggleWeight
                )
                Text("Trọng số: %.2fx".format(pref.weightValue), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = pref.weightValue.toFloat(),
                    onValueChange = { onWeightChange((round(it * 20.0) / 20.0).coerceIn(0.1, 3.0)) },
                    valueRange = 0.1f..3.0f,
                    enabled = pref.weightEnabled,
                    modifier = Modifier.weight(1f)
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onRunBacktest,
                    enabled = !isBusy,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("backtest_algo_${algo.id}")
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Backtest", style = MaterialTheme.typography.bodySmall)
                }

                if (algo.paramSpecs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onOptimize,
                        enabled = !isBusy,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("optimize_algo_${algo.id}")
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tối Ưu", style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = { expandedParams = !expandedParams },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tham số", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            AnimatedVisibility(visible = expandedParams && algo.paramSpecs.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tinh chỉnh tham số", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = onReset) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mặc định")
                        }
                    }

                    algo.paramSpecs.forEach { spec ->
                        val cur = pref.params[spec.key] ?: spec.defaultValue
                        val display = if (spec.isInt) cur.toInt().toString() else "%.3f".format(cur)
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(spec.label, style = MaterialTheme.typography.bodySmall)
                                Text(display, style = MaterialTheme.typography.labelMedium, color = colors.goldAccent)
                            }
                            Slider(
                                value = cur.toFloat().coerceIn(spec.minValue.toFloat(), spec.maxValue.toFloat()),
                                onValueChange = { raw ->
                                    val snapped = if (spec.isInt) round(raw.toDouble()) else round(raw * 1000.0) / 1000.0
                                    onParamChange(spec.key, snapped)
                                },
                                valueRange = spec.minValue.toFloat()..spec.maxValue.toFloat()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodBacktestCard(state: MainUiState) {
    val colors = LocalAnalyticsColors.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.neutralBlue.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "Đánh Giá Theo Chu Kỳ 30 Ngày · ${state.periodBacktestAlgoName}",
                style = MaterialTheme.typography.titleSmall,
                color = colors.neutralBlue
            )
            if (state.isRunningPeriodBacktest) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Đang chạy kiểm chứng lịch sử...", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                state.periodBacktestRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${row.periodLabel} (${row.totalDays}k)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = "T1 %.0f%% · T3 %.0f%% · T5 %.0f%%".format(row.top1Rate, row.top3Rate, row.top5Rate),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.hitGreen
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiStudioContent(
    state: MainUiState,
    onGenerate: (String, String, String) -> Unit,
    onSaveBundle: () -> Unit
) {
    val colors = LocalAnalyticsColors.current
    var prompt by remember { mutableStateOf("Ưu tiên số rơi ngắn hạn kết hợp láng giềng giải Đặc biệt và độ lệch Poisson 90 ngày") }
    var bundleName by remember { mutableStateOf("") }
    var bundleDesc by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = colors.warningAmberContainer,
                border = BorderStroke(1.dp, colors.warningAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.warningAmber,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.firebase_ai_required_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gợi ý chiến lược nhanh", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Bắt lô gan 15+ ngày & chu kỳ tuần",
                        "Ưu tiên lô rơi nóng & láng giềng ĐB",
                        "Cân bằng tần suất 45 ngày + Poisson"
                    ).forEach { samplePrompt ->
                        AssistChip(
                            onClick = { prompt = samplePrompt },
                            label = { Text(samplePrompt, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Mô tả ý tưởng thuật toán") },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_prompt_input")
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bundleName,
                    onValueChange = { bundleName = it },
                    label = { Text("Tên thuật toán (tùy chọn)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = bundleDesc,
                    onValueChange = { bundleDesc = it },
                    label = { Text("Ghi chú ngắn") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Button(
                onClick = { onGenerate(prompt, bundleName, bundleDesc) },
                enabled = !state.isGeneratingAi,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("ai_generate_button")
            ) {
                if (state.isGeneratingAi) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.Psychology, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text("Sinh Công Thức Mẫu (JSON Bundle)")
            }
        }

        val draft = state.aiDraftResult
        if (draft != null) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, colors.goldAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(draft.bundle.name, style = MaterialTheme.typography.titleMedium, color = colors.goldAccent)
                        Text(draft.summaryNote, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = draft.bundle.rawJson,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Button(
                            onClick = onSaveBundle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("ai_save_bundle_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Lưu & Kích Hoạt Thuật Toán")
                        }
                    }
                }
            }
        }
    }
}
