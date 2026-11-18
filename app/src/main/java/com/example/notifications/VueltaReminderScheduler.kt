package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.R
import com.example.data.model.Vuelta
import java.util.Calendar

class VueltaReminderScheduler(private val context: Context) {

  companion object {
    const val CHANNEL_ID = "iaestudio_reminders"
    const val EXTRA_VUELTA_ID = "extra_vuelta_id"
    const val EXTRA_VUELTA_TITLE = "extra_vuelta_title"
    const val EXTRA_DAYS_BEFORE = "extra_days_before"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
  }

  fun ensureChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          context.getString(R.string.notif_channel_name),
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = context.getString(R.string.notif_channel_desc)
          enableVibration(true)
        }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun scheduleForVuelta(vuelta: Vuelta) {
    if (vuelta.id.isBlank()) return
    cancelForVuelta(vuelta.id)
    if (vuelta.completed) return
    ensureChannel()

    val offsets = buildList {
      if (vuelta.remind2Days) add(2)
      if (vuelta.remind1Day) add(1)
      if (vuelta.remindSameDay) add(0)
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val now = System.currentTimeMillis()

    offsets.forEach { daysBefore ->
      val triggerCal = Calendar.getInstance().apply {
        timeInMillis = vuelta.dueDateMillis
        add(Calendar.DAY_OF_YEAR, -daysBefore)
        set(Calendar.HOUR_OF_DAY, vuelta.reminderHour.coerceIn(0, 23))
        set(Calendar.MINUTE, vuelta.reminderMinute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }

      if (triggerCal.timeInMillis > now + 10_000L) {
        val requestCode = requestCodeFor(vuelta.id, daysBefore)
        val pendingIntent = buildReceiverPendingIntent(
          vueltaId = vuelta.id,
          title = vuelta.title,
          daysBefore = daysBefore,
          requestCode = requestCode,
        )
        alarmManager.setAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerCal.timeInMillis,
          pendingIntent,
        )
      }
    }
  }

  fun scheduleSnoozeTomorrow(vueltaId: String, title: String) {
    ensureChannel()
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val triggerCal = Calendar.getInstance().apply {
      add(Calendar.DAY_OF_YEAR, 1)
      set(Calendar.HOUR_OF_DAY, 9)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val requestCode = requestCodeFor(vueltaId, 99)
    val pendingIntent = buildReceiverPendingIntent(
      vueltaId = vueltaId,
      title = title,
      daysBefore = 0,
      requestCode = requestCode,
    )
    alarmManager.setAndAllowWhileIdle(
      AlarmManager.RTC_WAKEUP,
      triggerCal.timeInMillis,
      pendingIntent,
    )
  }

  fun cancelForVuelta(vueltaId: String) {
    if (vueltaId.isBlank()) return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    listOf(2, 1, 0, 99).forEach { offset ->
      val requestCode = requestCodeFor(vueltaId, offset)
      val intent = Intent(context, VueltaReminderReceiver::class.java)
      val existing = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
      )
      if (existing != null) {
        alarmManager.cancel(existing)
        existing.cancel()
      }
    }
  }

  private fun buildReceiverPendingIntent(
    vueltaId: String,
    title: String,
    daysBefore: Int,
    requestCode: Int,
  ): PendingIntent {
    val intent = Intent(context, VueltaReminderReceiver::class.java).apply {
      putExtra(EXTRA_VUELTA_ID, vueltaId)
      putExtra(EXTRA_VUELTA_TITLE, title)
      putExtra(EXTRA_DAYS_BEFORE, daysBefore)
      putExtra(EXTRA_NOTIFICATION_ID, requestCode)
    }
    return PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun requestCodeFor(vueltaId: String, offset: Int): Int {
    return (vueltaId.hashCode() * 31 + offset) and 0x7FFFFFFF
  }
}
