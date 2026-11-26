package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LotteryProfile
import com.example.data.LotteryProfiles
import com.example.ui.theme.LocalAnalyticsColors
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val vnDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val vnDayOfWeekLabels = mapOf(
    1 to "Thứ 2",
    2 to "Thứ 3",
    3 to "Thứ 4",
    4 to "Thứ 5",
    5 to "Thứ 6",
    6 to "Thứ 7",
    7 to "Chủ Nhật"
)

fun LocalDate.formatVn(): String = this.format(vnDateFormatter)
fun LocalDate.dayOfWeekVn(): String = vnDayOfWeekLabels[this.dayOfWeek.value] ?: ""

fun formatVnd(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
    nf.maximumFractionDigits = 0
    return "${nf.format(amount)}đ"
}

@Composable
fun ProfileSelectorChip(
    selectedProfile: LotteryProfile,
    onSelectProfile: (LotteryProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalAnalyticsColors.current

    Box(modifier = modifier) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier
                .testTag("profile_selector_chip")
                .clickable { expanded = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = selectedProfile.shortName,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.goldAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Chọn đài xổ số",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            LotteryProfiles.ALL.forEach { profile ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (profile.id == selectedProfile.id) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = profile.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    trailingIcon = {
                        if (profile.id == selectedProfile.id) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = colors.goldAccent)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectProfile(profile)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateNavigatorCard(
    date: LocalDate,
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    prevTag: String,
    nextTag: String,
    pickerTag: String,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val colors = LocalAnalyticsColors.current

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(prevTag)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Kỳ trước"
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .testTag(pickerTag)
                    .clickable { showDatePicker = true }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = colors.goldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${date.dayOfWeekVn()}, ${date.formatVn()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(nextTag)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Kỳ tiếp theo"
                )
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(date) {
            date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDatePicked(picked)
                    }
                    showDatePicker = false
                }) {
                    Text("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun MetricSummaryTile(
    title: String,
    value: String,
    subtitle: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val colors = LocalAnalyticsColors.current
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
