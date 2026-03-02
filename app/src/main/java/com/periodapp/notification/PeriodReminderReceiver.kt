package com.periodapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.periodapp.MainActivity
import com.periodapp.R

class PeriodReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE) ?: return
        showTestNotification(context, type)
    }

    companion object {
        const val ACTION_REMIND = "com.periodapp.REMIND"
        const val EXTRA_TYPE = "type"
        const val TYPE_PERIOD = "period"
        const val TYPE_OVULATION = "ovulation"
        const val CHANNEL_PERIOD = "period_reminder"
        const val CHANNEL_OVULATION = "ovulation_reminder"

        /** Call this to show a notification immediately (e.g. from a test button). */
        fun showTestNotification(context: Context, type: String) {
            if (type != TYPE_PERIOD && type != TYPE_OVULATION) return
            showNotification(context, type)
        }

        private fun showNotification(context: Context, type: String) {
            val channelId = when (type) {
                TYPE_PERIOD -> CHANNEL_PERIOD
                TYPE_OVULATION -> CHANNEL_OVULATION
                else -> return
            }
            createChannelIfNeeded(context, channelId, type)
            val title = when (type) {
                TYPE_PERIOD -> context.getString(R.string.notification_period_title)
                TYPE_OVULATION -> context.getString(R.string.notification_ovulation_title)
                else -> return
            }
            val text = when (type) {
                TYPE_PERIOD -> context.getString(R.string.notification_period_text)
                TYPE_OVULATION -> context.getString(R.string.notification_ovulation_text)
                else -> return
            }
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingOpen = PendingIntent.getActivity(
                context,
                type.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingOpen)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(context).notify(type.hashCode(), notification)
        }

        private fun createChannelIfNeeded(context: Context, channelId: String, type: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val name = when (type) {
            TYPE_PERIOD -> context.getString(R.string.notification_channel_period)
            TYPE_OVULATION -> context.getString(R.string.notification_channel_ovulation)
            else -> "Reminders"
        }
        val channel = NotificationChannel(
            channelId,
            name,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
    }
}
