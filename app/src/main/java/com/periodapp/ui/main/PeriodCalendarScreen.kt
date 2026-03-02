package com.periodapp.ui.main

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import com.periodapp.data.OnboardingDataStore
import com.periodapp.data.PeriodRecord
import com.periodapp.ui.theme.Rose200
import com.periodapp.ui.theme.Sage200
import com.periodapp.ui.theme.WarmGray
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val MONTHS_BACK = 12

@Composable
private fun LegendChip(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val MONTHS_FORWARD = 1
private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun PeriodCalendarScreen(dataStore: OnboardingDataStore?) {
    if (dataStore == null) return
    val history by dataStore.getPeriodHistory().collectAsState(initial = emptyList())
    val cycleLength by dataStore.getCycleLengthDays().collectAsState(initial = 27)
    var monthOffset by remember { mutableStateOf(0) }
    var showAddPeriodDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    val cal = remember(monthOffset) {
        Calendar.getInstance(Locale.getDefault()).apply {
            add(Calendar.MONTH, monthOffset)
        }
    }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    val cycleDays = cycleLength ?: 27
    val dayPhases = dayPhasesInMonth(year, month, history, cycleDays)
    val firstOfMonthMs = remember(cal) {
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.timeInMillis
    }

    if (showAddPeriodDialog) {
        AddPeriodDialog(
            dataStore = dataStore,
            cycleLengthDays = cycleDays,
            initialStartMs = firstOfMonthMs,
            onDismiss = { showAddPeriodDialog = false }
        )
    }

    val scope = rememberCoroutineScope()
    var editingPeriod by remember { mutableStateOf<PeriodRecord?>(null) }
    selectedDay?.let { day ->
        DayDetailBottomSheet(
            year = year,
            month = month,
            dayOfMonth = day,
            phase = dayPhases[day] ?: DayPhase.WAIT,
                periodRecord = periodRecordForDay(year, month, day, history),
                dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault()),
                onEditPeriod = { record ->
                    selectedDay = null
                    editingPeriod = record
                },
                onDeletePeriod = { record ->
                    selectedDay = null
                    scope.launch {
                        dataStore.removePeriodFromHistory(record.startMs)
                    }
                },
                onAddPeriod = {
                    selectedDay = null
                    showAddPeriodDialog = true
                },
                onDismiss = { selectedDay = null }
            )
    }

    editingPeriod?.let { record ->
        AddPeriodDialog(
            dataStore = dataStore,
            cycleLengthDays = cycleDays,
            initialStartMs = record.startMs,
            initialEndMs = record.endMs,
            existingStartMsForEdit = record.startMs,
            onDismiss = { editingPeriod = null }
        )
    }

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
            text = "Tap a day to see details or edit. Colors: period, ovulation window, other days.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendChip(color = Rose200, label = "Period")
            Spacer(modifier = Modifier.size(16.dp))
            LegendChip(color = Sage200, label = "Ovulation")
            Spacer(modifier = Modifier.size(16.dp))
            LegendChip(color = WarmGray, label = "Other")
        }

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
            dayPhases = dayPhases,
            showTitle = false,
            onDayClick = { dayNum -> selectedDay = dayNum }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { showAddPeriodDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Add period")
        }
    }
}

@Composable
private fun AddPeriodDialog(
    dataStore: OnboardingDataStore,
    cycleLengthDays: Int,
    initialStartMs: Long,
    initialEndMs: Long? = null,
    existingStartMsForEdit: Long? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    var startMs by remember { mutableStateOf(initialStartMs) }
    var endMs by remember(initialStartMs, initialEndMs) {
        mutableStateOf(initialEndMs ?: run {
            val c = Calendar.getInstance(Locale.getDefault())
            c.timeInMillis = initialStartMs
            c.add(Calendar.DAY_OF_MONTH, 5)
            c.timeInMillis
        })
    }
    val calendar = remember { Calendar.getInstance(Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingStartMsForEdit != null) "Edit period" else "Add period") },
        text = {
            Column {
                Text(
                    "Period start",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                calendar.set(y, m, d)
                                startMs = calendar.timeInMillis
                                if (endMs < startMs) endMs = startMs
                            },
                            Calendar.getInstance().apply { timeInMillis = startMs }.get(Calendar.YEAR),
                            Calendar.getInstance().apply { timeInMillis = startMs }.get(Calendar.MONTH),
                            Calendar.getInstance().apply { timeInMillis = startMs }.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                ) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(dateFormat.format(java.util.Date(startMs)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Period end",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        val picker = DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                calendar.set(y, m, d)
                                endMs = calendar.timeInMillis
                            },
                            Calendar.getInstance().apply { timeInMillis = endMs }.get(Calendar.YEAR),
                            Calendar.getInstance().apply { timeInMillis = endMs }.get(Calendar.MONTH),
                            Calendar.getInstance().apply { timeInMillis = endMs }.get(Calendar.DAY_OF_MONTH)
                        )
                        picker.datePicker.minDate = startMs
                        picker.show()
                    }
                ) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(dateFormat.format(java.util.Date(endMs)))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val start = startMs
                    val end = endMs
                    val periodDays = ((end - start) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
                    scope.launch {
                        if (existingStartMsForEdit != null) {
                            dataStore.updatePeriodInHistory(existingStartMsForEdit, start, end)
                        } else {
                            dataStore.addPeriodAndUpdateLast(start, end, periodDays, cycleLengthDays)
                        }
                        onDismiss()
                    }
                }
            ) {
                Text(if (existingStartMsForEdit != null) "Save changes" else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MonthCalendarCard(
    year: Int,
    month: Int,
    monthLabel: String,
    dayPhases: Map<Int, DayPhase>,
    showTitle: Boolean = true,
    onDayClick: (Int) -> Unit = {}
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
                        val phase = dayPhases[dayNum]
                        val showDay = dayNum in 1..daysInMonth
                        val backgroundColor = when {
                            !showDay -> androidx.compose.ui.graphics.Color.Transparent
                            phase == DayPhase.PERIOD -> Rose200
                            phase == DayPhase.OVULATION -> Sage200
                            else -> WarmGray
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(backgroundColor)
                                .then(
                                    if (showDay) Modifier.clickable { onDayClick(dayNum) }
                                    else Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailBottomSheet(
    year: Int,
    month: Int,
    dayOfMonth: Int,
    phase: DayPhase,
    periodRecord: PeriodRecord?,
    dateFormat: SimpleDateFormat,
    onEditPeriod: (PeriodRecord) -> Unit,
    onDeletePeriod: (PeriodRecord) -> Unit,
    onAddPeriod: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cal = remember(year, month, dayOfMonth) {
        Calendar.getInstance(Locale.getDefault()).apply {
            set(year, month, dayOfMonth)
        }
    }
    val dateStr = dateFormat.format(cal.time)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            val phaseLabel = when (phase) {
                DayPhase.PERIOD -> "Period day"
                DayPhase.OVULATION -> "Ovulation window"
                DayPhase.WAIT -> "Regular day"
            }
            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            periodRecord?.let { record ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Period: ${dateFormat.format(java.util.Date(record.startMs))} – ${dateFormat.format(java.util.Date(record.endMs))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onEditPeriod(record) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit period")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onDeletePeriod(record) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete period")
                }
            } ?: run {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddPeriod,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add period")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
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
