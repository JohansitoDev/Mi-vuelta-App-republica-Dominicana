package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Vuelta
import com.example.data.model.VueltaDateUtils
import com.example.ui.components.ReceiptImagePreview
import com.example.ui.components.templateIcon
import com.example.ui.components.urgencyVisuals
import com.example.ui.theme.WhatsAppGreen
import com.example.util.ReceiptImageHelper
import com.example.util.WhatsAppShareHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetalleVueltaScreen(
  vuelta: Vuelta?,
  currentUserId: String,
  onBack: () -> Unit,
  onEdit: (String) -> Unit,
  onToggleDone: (Vuelta) -> Unit,
  onDelete: (String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var showDeleteConfirm by remember { mutableStateOf(false) }
  var showFullPhoto by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("Detalle de la vuelta", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("detalle_back_button")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
          }
        },
        actions = {
          if (vuelta != null && vuelta.ownerId == currentUserId) {
            IconButton(
              onClick = { onEdit(vuelta.id) },
              modifier = Modifier.testTag("detalle_edit_button"),
            ) {
              Icon(Icons.Rounded.Edit, contentDescription = "Editar vuelta")
            }
            IconButton(
              onClick = { showDeleteConfirm = true },
              modifier = Modifier.testTag("detalle_delete_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Eliminar vuelta",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
  ) { innerPadding ->
    if (vuelta == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Esta vuelta ya no está disponible.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      return@Scaffold
    }

    val urgency = VueltaDateUtils.urgencyOf(vuelta)
    val visuals = urgencyVisuals(urgency)

    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 640.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        // Urgency + Title Summary Card
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.large,
          border = BorderStroke(1.dp, visuals.accent.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Surface(
                color = visuals.container,
                shape = RoundedCornerShape(50),
              ) {
                Text(
                  text = VueltaDateUtils.urgencyBadgeText(vuelta),
                  style = MaterialTheme.typography.labelLarge,
                  color = visuals.onContainer,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
              }

              Icon(
                imageVector = templateIcon(vuelta.templateKey),
                contentDescription = null,
                tint = visuals.accent,
                modifier = Modifier.size(28.dp),
              )
            }

            Text(
              text = vuelta.title,
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "Vence: ${VueltaDateUtils.formatFullDate(vuelta.dueDateMillis)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
              )
            }

            if (vuelta.sharedWithFamily) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Group,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "Compartida con familia · Creada por ${vuelta.ownerName}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.tertiary,
                )
              }
            }

            if (vuelta.notes.isNotBlank()) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = vuelta.notes,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }
          }
        }

        // Reminders configured
        val activeReminders = buildList {
          if (vuelta.remind2Days) add("2 días antes")
          if (vuelta.remind1Day) add("1 día antes")
          if (vuelta.remindSameDay) add("El mismo día")
        }
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.medium,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = if (activeReminders.isEmpty()) {
                  "Sin avisos automáticos"
                } else {
                  "Avisos a las ${VueltaDateUtils.formatTime(vuelta.reminderHour, vuelta.reminderMinute)}"
                },
                style = MaterialTheme.typography.titleSmall,
              )
            }
            if (activeReminders.isNotEmpty()) {
              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                activeReminders.forEach { label ->
                  Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50),
                  ) {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                  }
                }
              }
            }
          }
        }

        // Receipt Photo Section ("Foto del papel")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Foto del papel / recibo",
            style = MaterialTheme.typography.titleMedium,
          )
          if (!vuelta.receiptBase64.isNullOrBlank()) {
            ReceiptImagePreview(
              base64 = vuelta.receiptBase64,
              contentDescription = "Foto del recibo de ${vuelta.title}",
              contentScale = ContentScale.Crop,
              onClick = { showFullPhoto = true },
              modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(MaterialTheme.shapes.large)
                .testTag("detalle_receipt_photo"),
            )
            Text(
              text = "Toca la foto para verla completa",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = "Sin foto del recibo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (vuelta.ownerId == currentUserId) {
                  TextButton(onClick = { onEdit(vuelta.id) }) {
                    Text("Agregar")
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Primary Action: "Ya lo hice"
        Button(
          onClick = { onToggleDone(vuelta) },
          shape = MaterialTheme.shapes.medium,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (vuelta.completed) {
              MaterialTheme.colorScheme.surfaceVariant
            } else {
              MaterialTheme.colorScheme.tertiary
            },
            contentColor = if (vuelta.completed) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onTertiary
            },
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("btn_ya_lo_hice"),
        ) {
          Icon(
            imageVector = if (vuelta.completed) Icons.Rounded.Refresh else Icons.Rounded.CheckCircle,
            contentDescription = null,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (vuelta.completed) "Reabrir vuelta" else "Ya lo hice",
            style = MaterialTheme.typography.titleMedium,
          )
        }

        // Secondary Action: "Compartir por WhatsApp"
        OutlinedButton(
          onClick = {
            scope.launch {
              val uri = ReceiptImageHelper.writeShareableReceiptUri(
                context = context,
                base64 = vuelta.receiptBase64,
                vueltaId = vuelta.id,
              )
              WhatsAppShareHelper.shareVuelta(context, vuelta, uri)
            }
          },
          shape = MaterialTheme.shapes.medium,
          border = BorderStroke(1.5.dp, WhatsAppGreen),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("btn_compartir_whatsapp"),
        ) {
          Icon(Icons.Rounded.Share, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Compartir por WhatsApp",
            style = MaterialTheme.typography.titleMedium,
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  if (showDeleteConfirm && vuelta != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("¿Eliminar esta vuelta?") },
      text = { Text("Se borrará \"${vuelta.title}\" y sus recordatorios programados.") },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteConfirm = false
            onDelete(vuelta.id)
          }
        ) {
          Text("Eliminar", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
      },
    )
  }

  if (showFullPhoto && !vuelta?.receiptBase64.isNullOrBlank()) {
    Dialog(onDismissRequest = { showFullPhoto = false }) {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.Black,
        modifier = Modifier
          .fillMaxWidth()
          .height(480.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          ReceiptImagePreview(
            base64 = vuelta?.receiptBase64,
            contentDescription = "Foto completa del recibo",
            contentScale = ContentScale.Fit,
            onClick = { showFullPhoto = false },
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}
