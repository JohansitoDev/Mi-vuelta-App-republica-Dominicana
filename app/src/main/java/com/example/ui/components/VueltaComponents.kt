package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.UrgencyLevel
import com.example.data.model.Vuelta
import com.example.data.model.VueltaDateUtils
import com.example.ui.theme.LocalIsBlackTheme
import com.example.ui.theme.UrgencyAmber
import com.example.ui.theme.UrgencyAmberContainer
import com.example.ui.theme.UrgencyAmberDarkContainer
import com.example.ui.theme.UrgencyGreen
import com.example.ui.theme.UrgencyGreenContainer
import com.example.ui.theme.UrgencyGreenDarkContainer
import com.example.ui.theme.UrgencyRed
import com.example.ui.theme.UrgencyRedContainer
import com.example.ui.theme.UrgencyRedDarkContainer
import com.example.util.ReceiptImageHelper

data class UrgencyVisuals(
  val accent: Color,
  val container: Color,
  val onContainer: Color,
)

@Composable
fun urgencyVisuals(level: UrgencyLevel): UrgencyVisuals {
  val isDark = LocalIsBlackTheme.current
  return when (level) {
    UrgencyLevel.ROJO -> UrgencyVisuals(
      accent = if (isDark) Color(0xFFFF8A80) else UrgencyRed,
      container = if (isDark) UrgencyRedDarkContainer else UrgencyRedContainer,
      onContainer = if (isDark) Color(0xFFFFDAD6) else Color(0xFF8C1D18),
    )
    UrgencyLevel.AMARILLO -> UrgencyVisuals(
      accent = if (isDark) Color(0xFFFCD34D) else UrgencyAmber,
      container = if (isDark) UrgencyAmberDarkContainer else UrgencyAmberContainer,
      onContainer = if (isDark) Color(0xFFFEF3C7) else Color(0xFF78350F),
    )
    UrgencyLevel.VERDE -> UrgencyVisuals(
      accent = if (isDark) Color(0xFF6EE7B7) else UrgencyGreen,
      container = if (isDark) UrgencyGreenDarkContainer else UrgencyGreenContainer,
      onContainer = if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46),
    )
    UrgencyLevel.COMPLETADA -> UrgencyVisuals(
      accent = MaterialTheme.colorScheme.outline,
      container = MaterialTheme.colorScheme.surfaceVariant,
      onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

fun templateIcon(templateKey: String): ImageVector = when (templateKey) {
  "internet" -> Icons.Rounded.Wifi
  "luz" -> Icons.Rounded.Bolt
  "agua" -> Icons.Rounded.WaterDrop
  "cable" -> Icons.Rounded.Tv
  "marbete" -> Icons.Rounded.DirectionsCar
  "pasaporte" -> Icons.Rounded.FlightTakeoff
  "licencia" -> Icons.Rounded.Badge
  "colegio" -> Icons.Rounded.School
  "medico" -> Icons.Rounded.MedicalServices
  else -> Icons.Rounded.EventNote
}

@Composable
fun VueltaCard(
  vuelta: Vuelta,
  onClick: () -> Unit,
  onToggleDone: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val level = VueltaDateUtils.urgencyOf(vuelta)
  val visuals = urgencyVisuals(level)
  val badgeLabel = VueltaDateUtils.urgencyBadgeText(vuelta)

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
      .testTag("vuelta_card_${vuelta.id}"),
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(
      width = 1.dp,
      color = if (level == UrgencyLevel.ROJO) visuals.accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant,
    ),
    tonalElevation = if (vuelta.completed) 0.dp else 2.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Traffic-light left urgency ribbon
      Box(
        modifier = Modifier
          .width(7.dp)
          .fillMaxHeight()
          .background(visuals.accent)
      )

      Row(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Template category icon pill
        Surface(
          color = visuals.container,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.size(46.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = templateIcon(vuelta.templateKey),
              contentDescription = null,
              tint = visuals.onContainer,
              modifier = Modifier.size(24.dp),
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Text(
            text = vuelta.title,
            style = MaterialTheme.typography.titleMedium.copy(
              textDecoration = if (vuelta.completed) TextDecoration.LineThrough else TextDecoration.None,
            ),
            color = if (vuelta.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              color = visuals.container,
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = visuals.onContainer,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = VueltaDateUtils.formatShortDate(vuelta.dueDateMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          // Secondary metadata badges (Receipt photo, Reminders, Family)
          val hasReceipt = !vuelta.receiptBase64.isNullOrBlank()
          val hasAlarms = vuelta.remind2Days || vuelta.remind1Day || vuelta.remindSameDay
          if (hasReceipt || vuelta.sharedWithFamily || hasAlarms) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (hasReceipt) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = "Tiene foto del recibo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = "Con foto",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
              if (hasAlarms && !vuelta.completed) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = "Con aviso activo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                  )
                  Text(
                    text = VueltaDateUtils.formatTime(vuelta.reminderHour, vuelta.reminderMinute),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              if (vuelta.sharedWithFamily) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Group,
                    contentDescription = "Compartida con familia",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = "Familia",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                  )
                }
              }
            }
          }
        }

        // Quick "Ya lo hice" toggle button (48dp touch target)
        IconButton(
          onClick = onToggleDone,
          modifier = Modifier
            .size(48.dp)
            .testTag("toggle_done_${vuelta.id}"),
        ) {
          Surface(
            shape = CircleShape,
            color = if (vuelta.completed) MaterialTheme.colorScheme.tertiary else Color.Transparent,
            border = BorderStroke(
              width = 2.dp,
              color = if (vuelta.completed) MaterialTheme.colorScheme.tertiary else visuals.accent,
            ),
            modifier = Modifier.size(30.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (vuelta.completed) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = "Marcar como pendiente",
                  tint = MaterialTheme.colorScheme.onTertiary,
                  modifier = Modifier.size(18.dp),
                )
              } else {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = "Ya lo hice",
                  tint = visuals.accent.copy(alpha = 0.5f),
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ReceiptImagePreview(
  base64: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop,
  onClick: (() -> Unit)? = null,
) {
  val imageBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = base64) {
    value = ReceiptImageHelper.decodeBase64ToImageBitmap(base64)
  }

  val img = imageBitmap
  if (img != null) {
    Image(
      bitmap = img,
      contentDescription = contentDescription,
      contentScale = contentScale,
      modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    )
  } else {
    Box(
      modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.ReceiptLong,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(36.dp),
      )
    }
  }
}
