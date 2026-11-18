package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

const val FREE_PLAN_ACTIVE_LIMIT = 5

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val planTier: String = "free",
  val familyId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val isProUser: Boolean
    get() = planTier == "pro"
}

@Keep
@IgnoreExtraProperties
data class FamilyGroup(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val name: String = "",
  val inviteCode: String = "",
  val memberUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Vuelta(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val ownerName: String = "",
  val memberUids: List<String> = emptyList(),
  val title: String = "",
  val templateKey: String = "otra",
  val notes: String = "",
  val dueDateMillis: Long = 0L,
  val remind2Days: Boolean = true,
  val remind1Day: Boolean = true,
  val remindSameDay: Boolean = true,
  val reminderHour: Int = 9,
  val reminderMinute: Int = 0,
  val completed: Boolean = false,
  val sharedWithFamily: Boolean = false,
  val receiptBase64: String? = null,
  val completedAtMillis: Long? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class UrgencyLevel {
  ROJO,
  AMARILLO,
  VERDE,
  COMPLETADA,
}

data class DominicanTemplate(
  val key: String,
  val title: String,
  val subtitle: String,
)

object DominicanTemplates {
  val all: List<DominicanTemplate> = listOf(
    DominicanTemplate("internet", "Pagar Internet", "Altice / Claro"),
    DominicanTemplate("luz", "Pagar Luz", "Edesur / Edenorte / Edeeste"),
    DominicanTemplate("agua", "Pagar Agua", "CAASD / Coraasan"),
    DominicanTemplate("cable", "Pagar Cable", "TV / Streaming"),
    DominicanTemplate("marbete", "Renovar Marbete", "DGII / Impuesto placa"),
    DominicanTemplate("pasaporte", "Cita Pasaporte", "Dirección de Pasaportes"),
    DominicanTemplate("licencia", "Licencia de Conducir", "INTRANT / Renovación"),
    DominicanTemplate("colegio", "Pagar Colegio / Uni", "Mensualidad / Matrícula"),
    DominicanTemplate("medico", "Ir al Médico / Receta", "Cita / Farmacia"),
    DominicanTemplate("otra", "Otra vuelta", "Personalizada"),
  )
}

object VueltaDateUtils {
  private val spanishLocale = Locale("es", "DO")

  fun startOfDayMillis(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = millis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  fun todayStartMillis(): Long = startOfDayMillis(System.currentTimeMillis())

  fun addDaysFromTodayMillis(days: Int): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = todayStartMillis()
      add(Calendar.DAY_OF_YEAR, days)
    }
    return cal.timeInMillis
  }

  fun endOfCurrentMonthMillis(): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = todayStartMillis()
      set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
    }
    return cal.timeInMillis
  }

  fun daysUntilDue(dueDateMillis: Long, referenceMillis: Long = System.currentTimeMillis()): Long {
    val today = startOfDayMillis(referenceMillis)
    val due = startOfDayMillis(dueDateMillis)
    return TimeUnit.MILLISECONDS.toDays(due - today)
  }

  fun urgencyOf(vuelta: Vuelta, referenceMillis: Long = System.currentTimeMillis()): UrgencyLevel {
    if (vuelta.completed) return UrgencyLevel.COMPLETADA
    val days = daysUntilDue(vuelta.dueDateMillis, referenceMillis)
    return when {
      days <= 1L -> UrgencyLevel.ROJO
      days <= 7L -> UrgencyLevel.AMARILLO
      else -> UrgencyLevel.VERDE
    }
  }

  fun urgencyBadgeText(vuelta: Vuelta, referenceMillis: Long = System.currentTimeMillis()): String {
    if (vuelta.completed) return "Completada"
    val days = daysUntilDue(vuelta.dueDateMillis, referenceMillis)
    return when {
      days < -1L -> "Vencida hace ${-days} días"
      days == -1L -> "Venció ayer"
      days == 0L -> "Vence hoy"
      days == 1L -> "Vence mañana"
      days in 2L..7L -> "En $days días · Esta semana"
      else -> "En $days días"
    }
  }

  fun formatShortDate(millis: Long): String {
    val formatter = SimpleDateFormat("EEE d MMM", spanishLocale)
    return formatter.format(Date(millis)).replaceFirstChar { it.titlecase(spanishLocale) }
  }

  fun formatFullDate(millis: Long): String {
    val formatter = SimpleDateFormat("EEEE d 'de' MMMM, yyyy", spanishLocale)
    return formatter.format(Date(millis)).replaceFirstChar { it.titlecase(spanishLocale) }
  }

  fun formatMonthYear(year: Int, monthZeroBased: Int): String {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, monthZeroBased)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    val formatter = SimpleDateFormat("MMMM yyyy", spanishLocale)
    return formatter.format(cal.time).replaceFirstChar { it.titlecase(spanishLocale) }
  }

  fun formatTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("h:mm a", spanishLocale).format(cal.time)
  }
}
