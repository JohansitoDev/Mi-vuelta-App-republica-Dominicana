package com.example.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class VueltaReminderReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val vueltaId = intent.getStringExtra(VueltaReminderScheduler.EXTRA_VUELTA_ID) ?: return
    val title = intent.getStringExtra(VueltaReminderScheduler.EXTRA_VUELTA_TITLE) ?: "Tu vuelta"
    val daysBefore = intent.getIntExtra(VueltaReminderScheduler.EXTRA_DAYS_BEFORE, 1)
    val notificationId = intent.getIntExtra(
      VueltaReminderScheduler.EXTRA_NOTIFICATION_ID,
      vueltaId.hashCode() and 0x7FFFFFFF,
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }

    VueltaReminderScheduler(context).ensureChannel()

    val (notifTitle, notifBody) = when (daysBefore) {
      2 -> "En 2 días vence: $title" to "Faltan 2 días para \"$title\". ¡Prepárate y evita moras!"
      1 -> "Mañana se vence: $title" to "Mañana se vence tu $title, ¿ya lo pagaste?"
      else -> "¡Hoy se vence: $title!" to "Hoy toca resolver \"$title\". ¡Que no se te pase!"
    }

    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(VueltaReminderScheduler.EXTRA_VUELTA_ID, vueltaId)
    }
    val openPendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val paidIntent = Intent(context, VueltaActionReceiver::class.java).apply {
      action = VueltaActionReceiver.ACTION_MARK_PAID
      putExtra(VueltaReminderScheduler.EXTRA_VUELTA_ID, vueltaId)
      putExtra(VueltaReminderScheduler.EXTRA_VUELTA_TITLE, title)
      putExtra(VueltaReminderScheduler.EXTRA_NOTIFICATION_ID, notificationId)
    }
    val paidPendingIntent = PendingIntent.getBroadcast(
      context,
      (notificationId + 1) and 0x7FFFFFFF,
      paidIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val snoozeIntent = Intent(context, VueltaActionReceiver::class.java).apply {
      action = VueltaActionReceiver.ACTION_SNOOZE_TOMORROW
      putExtra(VueltaReminderScheduler.EXTRA_VUELTA_ID, vueltaId)
      putExtra(VueltaReminderScheduler.EXTRA_VUELTA_TITLE, title)
      putExtra(VueltaReminderScheduler.EXTRA_NOTIFICATION_ID, notificationId)
    }
    val snoozePendingIntent = PendingIntent.getBroadcast(
      context,
      (notificationId + 2) and 0x7FFFFFFF,
      snoozeIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, VueltaReminderScheduler.CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_popup_reminder)
      .setContentTitle(notifTitle)
      .setContentText(notifBody)
      .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(openPendingIntent)
      .addAction(
        android.R.drawable.checkbox_on_background,
        context.getString(R.string.notif_action_paid),
        paidPendingIntent,
      )
      .addAction(
        android.R.drawable.ic_menu_recent_history,
        context.getString(R.string.notif_action_snooze),
        snoozePendingIntent,
      )
      .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
  }
}
