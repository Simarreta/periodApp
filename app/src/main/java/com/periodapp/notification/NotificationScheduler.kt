package com.periodapp.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.periodapp.data.OnboardingDataStore
import kotlinx.coroutines.flow.first

private const val MS_PER_DAY = 24 * 60 * 60 * 1000L
private const val NOTIFICATION_HOUR = 8
private const val REQUEST_PERIOD = 1001
private const val REQUEST_OVULATION = 1002

object NotificationScheduler {

    suspend fun schedule(context: Context, dataStore: OnboardingDataStore) {
        val lastStartMs = dataStore.getLastPeriodStartMs().first() ?: return
        val cycleDays = dataStore.getCycleLengthDays().first() ?: 27
        val cycleMs = cycleDays * MS_PER_DAY
        val now = System.currentTimeMillis()

        var nextPeriodStartMs = lastStartMs
        while (nextPeriodStartMs <= now) {
            nextPeriodStartMs += cycleMs
        }
        val nextOvulationMs = nextPeriodStartMs - 14 * MS_PER_DAY

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        scheduleAlarmAt8Am(context, alarmManager, nextPeriodStartMs, REQUEST_PERIOD, PeriodReminderReceiver.TYPE_PERIOD)
        scheduleAlarmAt8Am(context, alarmManager, nextOvulationMs, REQUEST_OVULATION, PeriodReminderReceiver.TYPE_OVULATION)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAlarm(context, alarmManager, REQUEST_PERIOD)
        cancelAlarm(context, alarmManager, REQUEST_OVULATION)
    }

    private fun scheduleAlarmAt8Am(
        context: Context,
        alarmManager: AlarmManager,
        dayStartMs: Long,
        requestCode: Int,
        type: String
    ) {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = dayStartMs
            set(java.util.Calendar.HOUR_OF_DAY, NOTIFICATION_HOUR)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        var triggerAt = cal.timeInMillis
        if (triggerAt <= System.currentTimeMillis()) return

        val intent = Intent(context, PeriodReminderReceiver::class.java).apply {
            action = PeriodReminderReceiver.ACTION_REMIND
            putExtra(PeriodReminderReceiver.EXTRA_TYPE, type)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                return
            }
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, PeriodReminderReceiver::class.java).apply {
            action = PeriodReminderReceiver.ACTION_REMIND
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}
