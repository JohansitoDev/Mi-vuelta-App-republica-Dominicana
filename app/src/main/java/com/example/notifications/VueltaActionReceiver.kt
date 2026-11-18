package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.firebase.MiVueltaRepository
import com.example.data.local.LocalSessionStore
import com.example.data.local.LocalVueltaRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VueltaActionReceiver : BroadcastReceiver() {

  companion object {
    const val ACTION_MARK_PAID = "com.example.mivuelta.ACTION_MARK_PAID"
    const val ACTION_SNOOZE_TOMORROW = "com.example.mivuelta.ACTION_SNOOZE_TOMORROW"
  }

  override fun onReceive(context: Context, intent: Intent) {
    val vueltaId = intent.getStringExtra(VueltaReminderScheduler.EXTRA_VUELTA_ID) ?: return
    val title = intent.getStringExtra(VueltaReminderScheduler.EXTRA_VUELTA_TITLE) ?: "Tu vuelta"
    val notificationId = intent.getIntExtra(VueltaReminderScheduler.EXTRA_NOTIFICATION_ID, 0)

    if (notificationId != 0) {
      NotificationManagerCompat.from(context).cancel(notificationId)
    }

    val scheduler = VueltaReminderScheduler(context)

    when (intent.action) {
      ACTION_MARK_PAID -> {
        scheduler.cancelForVuelta(vueltaId)
        val cloudUser = Firebase.auth.currentUser
        val localSession = LocalSessionStore(context).activeLocalSession.value
        if (cloudUser == null && localSession == null) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
          try {
            if (cloudUser != null) {
              MiVueltaRepository(context.applicationContext).toggleCompleted(vueltaId, true)
            } else if (localSession != null) {
              LocalVueltaRepository(
                context = context.applicationContext,
                uid = localSession.uid,
                displayName = localSession.displayName,
                email = localSession.email,
              ).toggleCompleted(vueltaId, true)
            }
          } finally {
            pendingResult.finish()
          }
        }
      }
      ACTION_SNOOZE_TOMORROW -> {
        scheduler.scheduleSnoozeTomorrow(vueltaId, title)
      }
    }
  }
}
