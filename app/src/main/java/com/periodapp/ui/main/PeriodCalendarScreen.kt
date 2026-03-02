package com.periodapp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.periodapp.data.OnboardingDataStore
import com.periodapp.data.PeriodRecord
import com.periodapp.ui.theme.Rose200
import java.util.Calendar
import java.util.Locale

private const val MONTHS_BACK = 12
private const val MONTHS_FORWARD = 1
private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun PeriodCalendarScreen(dataStore: OnboardingDataStore?) {
    if (dataStore == null) return
    val history by dataStore.getPeriodHistory().collectAsState(initial = emptyList())
    var monthOffset by remember { mutableIntStateOf(0) }

    val cal = remember(monthOffset) {
        Calendar.getInstance(Locale.getDefault()).apply {
            add(Calendar.MONTH, monthOffset)
        }
    }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    val periodDays = periodDaysInMonth(year, month, history)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            text = "Period calendar",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Months when you had your period",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { if (monthOffset > -MONTHS_BACK) monthOffset-- },
                enabled = monthOffset > -MONTHS_BACK
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous month",
                    tint = if (monthOffset > -MONTHS_BACK)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            Text(
                text = "$monthName $year",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(
                onClick = { if (monthOffset < MONTHS_FORWARD) monthOffset++ },
                enabled = monthOffset < MONTHS_FORWARD
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next month",
                    tint = if (monthOffset < MONTHS_FORWARD)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }

        MonthCalendarCard(
            year = year,
            month = month,
            monthLabel = "$monthName $year",
            periodDays = periodDays,
            showTitle = false
        )
    }
}

@Composable
private fun MonthCalendarCard(
    year: Int,
    month: Int,
    monthLabel: String,
    periodDays: Set<Int>,
    showTitle: Boolean = true
) {
    val calendar = Calendar.getInstance(Locale.getDefault()).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val offset = (firstDayOfWeek - calendar.firstDayOfWeek + 7) % 7
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val totalCells = offset + daysInMonth
    val rows = (totalCells + 6) / 7

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (showTitle) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WEEKDAYS.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - offset + 1
                        val isPeriodDay = dayNum in 1..daysInMonth && dayNum in periodDays
                        val showDay = dayNum in 1..daysInMonth
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !showDay -> androidx.compose.ui.graphics.Color.Transparent
                                        isPeriodDay -> Rose200
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showDay) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun periodDaysInMonth(year: Int, month: Int, records: List<PeriodRecord>): Set<Int> {
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.set(year, month, 1)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val result = mutableSetOf<Int>()
    for (day in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStartMs = cal.timeInMillis
        val dayEndMs = dayStartMs + 24 * 60 * 60 * 1000 - 1
        if (records.any { it.startMs <= dayEndMs && it.endMs >= dayStartMs }) {
            result.add(day)
        }
    }
    return result
}
