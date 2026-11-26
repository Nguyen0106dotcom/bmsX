package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LotteryProfile
import com.example.data.LotteryProfiles
import com.example.ui.MainUiState
import com.example.ui.components.formatVn
import com.example.ui.theme.LocalAnalyticsColors

@Composable
fun SettingsScreen(
    state: MainUiState,
    onToggleDarkTheme: (Boolean) -> Unit,
    onUpdateSyncUrl: (String, String) -> Unit,
    onSyncProfile: (LotteryProfile) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAnalyticsColors.current
    var showGuide by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.TopCenter, modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
        ) {
            // 1. Theme & Reset Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text("Giao Diện & Hệ Thống", style = MaterialTheme.typography.titleMedium)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DarkMode, contentDescription = null, tint = colors.goldAccent)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Chế độ Tối (Midnight Analytics)", style = MaterialTheme.typography.bodyLarge)
                                    Text("Tối ưu độ tương phản khi soi cầu & bảng số", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                                }
                            }
                            Switch(
                                checked = state.darkTheme,
                                onCheckedChange = onToggleDarkTheme,
                                modifier = Modifier.testTag("dark_theme_switch")
                            )
                        }

                        HorizontalDivider()

                        OutlinedButton(
                            onClick = onResetDefaults,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("reset_defaults_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Khôi Phục Cấu Hình Thuật Toán Mặc Định")
                        }
                    }
                }
            }

            // 2. Data Sync Manager
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = colors.neutralBlue)
                            Spacer(Modifier.width(8.dp))
                            Text("Nguồn Dữ Liệu & Đồng Bộ Từ Xa", style = MaterialTheme.typography.titleMedium)
                        }

                        LotteryProfiles.ALL.forEach { profile ->
                            val count = state.profileRecordCounts[profile.id] ?: 0
                            val latest = state.profileLatestDates[profile.id]?.formatVn() ?: "--"
                            var urlDraft by remember(profile.id, state.syncUrls[profile.id]) {
                                mutableStateOf(state.syncUrls[profile.id] ?: profile.defaultSyncUrl)
                            }
                            var expandedUrl by remember { mutableStateOf(false) }

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(profile.name, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = "$count kỳ · Mới nhất: $latest",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.mutedText
                                            )
                                        }
                                        Button(
                                            onClick = { onSyncProfile(profile) },
                                            enabled = !state.isSyncing,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("sync_${profile.id}_button")
                                        ) {
                                            if (state.isSyncing) {
                                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                            } else {
                                                Text("Đồng bộ", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }

                                    Text(
                                        text = if (expandedUrl) "Ẩn cấu hình URL ▲" else "Chỉnh URL nguồn đồng bộ ▼",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.neutralBlue,
                                        modifier = Modifier.clickable { expandedUrl = !expandedUrl }
                                    )

                                    AnimatedVisibility(visible = expandedUrl) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = urlDraft,
                                                onValueChange = { urlDraft = it },
                                                label = { Text("URL JSON/JSONL") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedButton(
                                                onClick = { onUpdateSyncUrl(profile.id, urlDraft) }
                                            ) {
                                                Text("Lưu")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Built-in Guide
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showGuide = !showGuide }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = colors.goldAccent)
                                Spacer(Modifier.width(8.dp))
                                Text("Hướng Dẫn Sử Dụng Thiên Cơ Số", style = MaterialTheme.typography.titleMedium)
                            }
                            Icon(if (showGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                        }

                        AnimatedVisibility(visible = showGuide) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                HorizontalDivider()
                                GuideSection(
                                    title = "1. Dự Đoán & Điểm Tổng Hợp (Base = 100.0)",
                                    body = "Mỗi con số khởi đầu với 100 điểm gốc. Hệ thống cộng thêm điểm delta đã nhân trọng số từ các thuật toán đang bật. Chạm vào từng ô số để xem đóng góp chi tiết và tần suất 30/100/365 ngày."
                                )
                                GuideSection(
                                    title = "2. Thuật Toán, Tối Ưu & Backtest",
                                    body = "Bao gồm 5 thuật toán cốt lõi (Điểm Lịch Sử V13, Bản Tối Ưu, Điểm Lô Gan, Phạt Vị Trí Giải, Phạt Tần Suất 30 Ngày). Bạn có thể chỉnh tham số thủ công hoặc dùng chức năng Tối Ưu tự động kiểm thử 45 kỳ gần nhất."
                                )
                                GuideSection(
                                    title = "3. Bảng KQXS & Phân Tích Chuyên Sâu",
                                    body = "Xem đủ 27 giải XSMB hoặc kết quả Vietlott, thống kê Lô nháy, Lô rơi, Top Gan Lì, Đầu–Đuôi và tra cứu lịch sử bất kỳ số nào theo năm/tháng."
                                )
                                GuideSection(
                                    title = "4. Giả Lập Chơi Thử (Paper Trading)",
                                    body = "Kiểm tra hiệu quả chiến lược Đề, Lô, Xiên 2/3/4 theo thứ hạng dự đoán trên dữ liệu quá khứ mà không tốn tiền thật."
                                )
                            }
                        }
                    }
                }
            }

            // 4. Legal & Academic Disclaimer
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = colors.specialCrimsonContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, colors.specialCrimson.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = colors.specialCrimson,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Tuyên Bố Miễn Trừ Trách Nhiệm",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.specialCrimson
                            )
                            Text(
                                text = stringResource(R.string.legal_disclaimer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSection(title: String, body: String) {
    val colors = LocalAnalyticsColors.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.goldAccent)
        Text(body, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
    }
}
