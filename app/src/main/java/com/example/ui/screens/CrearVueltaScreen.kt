package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.DominicanTemplate
import com.example.data.model.DominicanTemplates
import com.example.data.model.FamilyGroup
import com.example.data.model.UserProfile
import com.example.data.model.Vuelta
import com.example.data.model.VueltaDateUtils
import com.example.ui.components.ReceiptImagePreview
import com.example.ui.components.templateIcon
import com.example.util.ReceiptImageHelper
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrearVueltaScreen(
  existingVuelta: Vuelta?,
  userProfile: UserProfile?,
  familyGroup: FamilyGroup?,
  isSaving: Boolean,
  onBack: () -> Unit,
  onSave: (
    title: String,
    templateKey: String,
    notes: String,
    dueDateMillis: Long,
    remind2Days: Boolean,
    remind1Day: Boolean,
    remindSameDay: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    sharedWithFamily: Boolean,
    receiptBase64: String?,
  ) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedTemplateKey by remember(existingVuelta) {
    mutableStateOf(existingVuelta?.templateKey ?: "internet")
  }
  var title by remember(existingVuelta) {
    mutableStateOf(existingVuelta?.title ?: "")
  }
  var notes by remember(existingVuelta) {
    mutableStateOf(existingVuelta?.notes ?: "")
  }
  var dueDateMillis by remember(existingVuelta) {
    mutableLongStateOf(existingVuelta?.dueDateMillis ?: VueltaDateUtils.addDaysFromTodayMillis(1))
  }
  var remind2Days by remember(existingVuelta) {
    mutableStateOf(existingVuelta?.remind2Days ?: true)
  }
  var remind1Day by remember(existingVuelta) {
    mutableStateOf(existingVuelta?.remind1Day ?: true)
  }
  var remindSameDay by remember(existingVuelta) {
    mutableStateOf(existingVuelta?.remindSameDay ?: true)
  }
  var reminderHour by remember(existingVuelta) {
    mutableIntStateOf(existingVuelta?.reminderHour ?: 9)
  }
  var reminderMinute by remember(existingVuelta) {
    mutableIntStateOf(existingVuelta?.reminderMinute ?: 0)
  }
  var sharedWithFamily by remember(existingVuelta) {
    mutableStateOf(existingVuelta?.sharedWithFamily ?: false)
  }
  var receiptBase64 by remember(existingVuelta) {
    mutableStateOf(existingVuelta?.receiptBase64)
  }

  var isProcessingPhoto by remember { mutableStateOf(false) }
  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { /* Continue regardless; notifications work when granted */ }

  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        isProcessingPhoto = true
        receiptBase64 = ReceiptImageHelper.uriToCompressedBase64(context, uri)
        isProcessingPhoto = false
      }
    }
  }

  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
  ) { success ->
    val captured = pendingCameraUri
    if (success && captured != null) {
      scope.launch {
        isProcessingPhoto = true
        receiptBase64 = ReceiptImageHelper.uriToCompressedBase64(context, captured)
        isProcessingPhoto = false
      }
    }
  }

  fun requestNotificationsIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (existingVuelta == null) "Nueva Vuelta" else "Editar Vuelta",
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("crear_back_button")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
  ) { innerPadding ->
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
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        // 1. Dominican Quick Templates
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Plantillas rápidas",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Toca una opción para llenarla en un segundo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DominicanTemplates.all.forEach { template ->
              TemplateQuickCard(
                template = template,
                selected = selectedTemplateKey == template.key && title == template.title,
                onClick = {
                  selectedTemplateKey = template.key
                  title = if (template.key == "otra") "" else template.title
                  validationError = null
                },
              )
            }
          }
        }

        // 2. Title & Notes
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "1. ¿Qué tienes que hacer?",
            style = MaterialTheme.typography.titleMedium,
          )
          OutlinedTextField(
            value = title,
            onValueChange = {
              if (it.length <= 100) title = it
              validationError = null
            },
            placeholder = { Text("Ej: Pagar internet, renovar marbete...") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_vuelta_title"),
          )
          OutlinedTextField(
            value = notes,
            onValueChange = { if (it.length <= 300) notes = it },
            placeholder = { Text("Nota opcional (ej: contrato, monto RD$1,650...)") },
            maxLines = 3,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_vuelta_notes"),
          )
        }

        // 3. Due Date
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "2. ¿Cuándo vence?",
            style = MaterialTheme.typography.titleMedium,
          )

          val todayMillis = remember { VueltaDateUtils.todayStartMillis() }
          val tomorrowMillis = remember { VueltaDateUtils.addDaysFromTodayMillis(1) }
          val in3DaysMillis = remember { VueltaDateUtils.addDaysFromTodayMillis(3) }
          val nextWeekMillis = remember { VueltaDateUtils.addDaysFromTodayMillis(7) }
          val endOfMonthMillis = remember { VueltaDateUtils.endOfCurrentMonthMillis() }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            DateQuickChip("Hoy", dueDateMillis == todayMillis) { dueDateMillis = todayMillis }
            DateQuickChip("Mañana", dueDateMillis == tomorrowMillis) { dueDateMillis = tomorrowMillis }
            DateQuickChip("En 3 días", dueDateMillis == in3DaysMillis) { dueDateMillis = in3DaysMillis }
            DateQuickChip("En 1 semana", dueDateMillis == nextWeekMillis) { dueDateMillis = nextWeekMillis }
            DateQuickChip("Fin de mes", dueDateMillis == endOfMonthMillis) { dueDateMillis = endOfMonthMillis }
          }

          Surface(
            onClick = { showDatePicker = true },
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("open_calendar_picker"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.CalendarMonth,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = VueltaDateUtils.formatFullDate(dueDateMillis),
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.Medium,
                )
              }
              Text(
                text = "Cambiar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        // 4. Reminders
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "3. ¿Quieres que te avise?",
              style = MaterialTheme.typography.titleMedium,
            )
            Surface(
              onClick = { showTimePicker = true },
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(50),
              modifier = Modifier.testTag("time_picker_chip"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.AccessTime,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(15.dp),
                )
                Text(
                  text = VueltaDateUtils.formatTime(reminderHour, reminderMinute),
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = remind2Days,
              onClick = {
                remind2Days = !remind2Days
                if (remind2Days) requestNotificationsIfNeeded()
              },
              label = { Text("2 días antes") },
              leadingIcon = if (remind2Days) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
              } else null,
              modifier = Modifier.testTag("chip_remind_2d"),
            )
            FilterChip(
              selected = remind1Day,
              onClick = {
                remind1Day = !remind1Day
                if (remind1Day) requestNotificationsIfNeeded()
              },
              label = { Text("1 día antes") },
              leadingIcon = if (remind1Day) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
              } else null,
              modifier = Modifier.testTag("chip_remind_1d"),
            )
            FilterChip(
              selected = remindSameDay,
              onClick = {
                remindSameDay = !remindSameDay
                if (remindSameDay) requestNotificationsIfNeeded()
              },
              label = { Text("El mismo día") },
              leadingIcon = if (remindSameDay) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
              } else null,
              modifier = Modifier.testTag("chip_remind_same_day"),
            )
          }
        }

        // 5. Photo of Receipt ("Foto del papel")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "4. ¿Tienes foto del papel o recibo?",
            style = MaterialTheme.typography.titleMedium,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = {
                val uri = ReceiptImageHelper.createTempCameraUri(context)
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
              },
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("btn_tirar_foto"),
            ) {
              Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Tirar foto")
            }

            OutlinedButton(
              onClick = {
                galleryLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("btn_elegir_foto"),
            ) {
              Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Galería")
            }
          }

          if (isProcessingPhoto) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              Text("Guardando foto del recibo...", style = MaterialTheme.typography.bodySmall)
            }
          } else if (!receiptBase64.isNullOrBlank()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(MaterialTheme.shapes.medium),
            ) {
              ReceiptImagePreview(
                base64 = receiptBase64,
                contentDescription = "Vista previa del recibo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
              Surface(
                onClick = { receiptBase64 = null },
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(10.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Quitar foto",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = "Quitar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                  )
                }
              }
            }
          }
        }

        // 6. Family Sharing (available when Pro + inside a Family Group)
        if (userProfile?.isProUser == true && familyGroup != null && familyGroup.memberUids.size > 1) {
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Group,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Column {
                  Text(
                    text = "Compartir con ${familyGroup.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                  )
                  Text(
                    text = "Le llegará a todos los miembros del grupo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                  )
                }
              }
              Switch(
                checked = sharedWithFamily,
                onCheckedChange = { sharedWithFamily = it },
                modifier = Modifier.testTag("switch_share_family"),
              )
            }
          }
        }

        if (validationError != null) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = validationError!!,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(12.dp),
            )
          }
        }

        Button(
          onClick = {
            val clean = title.trim()
            if (clean.isEmpty()) {
              validationError = "Escribe qué vuelta tienes que hacer o elige una plantilla."
              return@Button
            }
            if (remind2Days || remind1Day || remindSameDay) {
              requestNotificationsIfNeeded()
            }
            onSave(
              clean,
              selectedTemplateKey,
              notes.trim(),
              dueDateMillis,
              remind2Days,
              remind1Day,
              remindSameDay,
              reminderHour,
              reminderMinute,
              sharedWithFamily,
              receiptBase64,
            )
          },
          enabled = !isSaving && !isProcessingPhoto,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("btn_guardar_vuelta"),
        ) {
          if (isSaving) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.5.dp,
            )
          } else {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (existingVuelta == null) "Guardar vuelta" else "Actualizar vuelta",
              style = MaterialTheme.typography.titleMedium,
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  if (showDatePicker) {
    // Convert local midnight -> UTC midnight for M3 DatePicker
    val utcSelected = remember(dueDateMillis) {
      val localCal = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
      Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
          localCal.get(Calendar.YEAR),
          localCal.get(Calendar.MONTH),
          localCal.get(Calendar.DAY_OF_MONTH),
        )
      }.timeInMillis
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = utcSelected)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { pickedUtc ->
              val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = pickedUtc
              }
              val localCal = Calendar.getInstance().apply {
                clear()
                set(
                  utcCal.get(Calendar.YEAR),
                  utcCal.get(Calendar.MONTH),
                  utcCal.get(Calendar.DAY_OF_MONTH),
                )
              }
              dueDateMillis = localCal.timeInMillis
            }
            showDatePicker = false
          }
        ) {
          Text("Listo")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showTimePicker) {
    val timePickerState = rememberTimePickerState(
      initialHour = reminderHour,
      initialMinute = reminderMinute,
      is24Hour = false,
    )
    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      title = { Text("Hora del aviso") },
      text = { TimePicker(state = timePickerState) },
      confirmButton = {
        TextButton(
          onClick = {
            reminderHour = timePickerState.hour
            reminderMinute = timePickerState.minute
            showTimePicker = false
          }
        ) { Text("Aceptar") }
      },
      dismissButton = {
        TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
      },
    )
  }
}

@Composable
private fun TemplateQuickCard(
  template: DominicanTemplate,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(
      width = if (selected) 1.5.dp else 1.dp,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    ),
    modifier = Modifier
      .width(134.dp)
      .testTag("template_${template.key}"),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = templateIcon(template.key),
        contentDescription = null,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(24.dp),
      )
      Text(
        text = template.title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
      )
      Text(
        text = template.subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun DateQuickChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = { Text(label) },
  )
}
