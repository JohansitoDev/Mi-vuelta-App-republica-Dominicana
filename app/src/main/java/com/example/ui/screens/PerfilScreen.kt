package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FREE_PLAN_ACTIVE_LIMIT
import com.example.data.model.FamilyGroup
import com.example.data.model.UserProfile
import com.example.ui.theme.AppThemeMode
import com.example.util.WhatsAppShareHelper

@Composable
fun PerfilScreen(
  userProfile: UserProfile?,
  userEmail: String,
  isCloudSession: Boolean,
  isGuestSession: Boolean,
  themeMode: AppThemeMode,
  onSetThemeMode: (AppThemeMode) -> Unit,
  onConnectGoogle: () -> Unit,
  activeOwnCount: Int,
  familyGroup: FamilyGroup?,
  onActivatePro: () -> Unit,
  onSwitchFree: () -> Unit,
  onCreateFamily: (String) -> Unit,
  onJoinFamily: (String) -> Unit,
  onLeaveFamily: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val isPro = userProfile?.isProUser == true
  val displayName = userProfile?.displayName?.takeIf { it.isNotBlank() }
    ?: if (isGuestSession) "Invitado" else "Usuario IAEstudio"
  val initials = displayName.split(" ")
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .take(2)
    .joinToString("")
    .ifEmpty { "IE" }

  var showCreateFamilyDialog by remember { mutableStateOf(false) }
  var showJoinFamilyDialog by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 680.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(
        start = 20.dp,
        end = 20.dp,
        top = 16.dp,
        bottom = 32.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item(key = "title") {
        Text(
          text = "Perfil / Pro",
          style = MaterialTheme.typography.headlineMedium,
        )
      }

      // User Card
      item(key = "user_card") {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.large,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(52.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = displayName,
                  style = MaterialTheme.typography.titleMedium,
                )
                if (userEmail.isNotBlank()) {
                  Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  Icon(
                    imageVector = if (isCloudSession) Icons.Rounded.CloudQueue else Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = when {
                      isCloudSession -> "Sincronizado en la nube con Google"
                      isGuestSession -> "Modo invitado · Guardado en este teléfono"
                      else -> "Cuenta local · Guardado en este teléfono"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }

            if (!isCloudSession) {
              OutlinedButton(
                onClick = onConnectGoogle,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(46.dp)
                  .testTag("btn_conectar_google"),
              ) {
                Text("Conectar con Google para sincronizar en la nube")
              }
            }
          }
        }
      }

      // Theme Mode Selector: Modo Claro / Modo Negro
      item(key = "theme_card") {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.large,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Apariencia",
              style = MaterialTheme.typography.titleMedium,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              ThemeOptionChip(
                label = "Modo Claro",
                icon = Icons.Rounded.LightMode,
                selected = themeMode == AppThemeMode.LIGHT,
                onClick = { onSetThemeMode(AppThemeMode.LIGHT) },
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_modo_claro"),
              )
              ThemeOptionChip(
                label = "Modo Negro",
                icon = Icons.Rounded.DarkMode,
                selected = themeMode == AppThemeMode.BLACK,
                onClick = { onSetThemeMode(AppThemeMode.BLACK) },
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_modo_negro"),
              )
            }
          }
        }
      }

      // Free / Active Quota Meter
      item(key = "quota_card") {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.large,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (isPro) "Plan IAEstudio Pro" else "Vueltas gratis activas",
                style = MaterialTheme.typography.titleMedium,
              )
              Surface(
                color = if (isPro) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50),
              ) {
                Text(
                  text = if (isPro) "Ilimitadas" else "$activeOwnCount / $FREE_PLAN_ACTIVE_LIMIT",
                  style = MaterialTheme.typography.labelMedium,
                  color = if (isPro) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }

            if (!isPro) {
              val progress = (activeOwnCount.toFloat() / FREE_PLAN_ACTIVE_LIMIT).coerceIn(0f, 1f)
              LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(10.dp)
                  .clip(RoundedCornerShape(50)),
                color = if (activeOwnCount >= FREE_PLAN_ACTIVE_LIMIT) {
                  MaterialTheme.colorScheme.error
                } else {
                  MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
              )
              val remaining = (FREE_PLAN_ACTIVE_LIMIT - activeOwnCount).coerceAtLeast(0)
              Text(
                text = if (remaining == 0) {
                  "Alcanzaste el límite de 5 vueltas activas. Completa una o activa Pro."
                } else {
                  "Te quedan $remaining vueltas gratis activas."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              Text(
                text = "Tienes vueltas ilimitadas y acceso completo para compartir vueltas con tu familia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Pro Upgrade Card
      item(key = "pro_card") {
        Surface(
          color = if (isPro) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
          shape = MaterialTheme.shapes.large,
          border = BorderStroke(
            1.5.dp,
            if (isPro) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
          ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = if (isPro) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
              )
              Column {
                Text(
                  text = "IAEstudio Pro",
                  style = MaterialTheme.typography.titleLarge,
                )
                Text(
                  text = "RD$150/mes · RD$1,200/año (ahorras 33%)",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }

            ProBenefitRow("Vueltas activas ilimitadas cada mes")
            ProBenefitRow("Comparte vueltas con tu pareja o familia")
            ProBenefitRow("Historial completo de recibos en la nube")

            if (!isPro) {
              Button(
                onClick = onActivatePro,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .testTag("btn_activar_pro"),
              ) {
                Text("Activar prueba Pro gratis (7 días)", style = MaterialTheme.typography.titleSmall)
              }
            } else {
              OutlinedButton(
                onClick = onSwitchFree,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("btn_volver_gratis"),
              ) {
                Text("Volver al plan Gratis")
              }
            }
          }
        }
      }

      // Family Sharing Section (Cloud Google users)
      if (isCloudSession) {
        item(key = "family_card") {
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Group,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                  text = "Familia compartida",
                  style = MaterialTheme.typography.titleMedium,
                )
              }

              if (familyGroup != null) {
                Text(
                  text = "Grupo: ${familyGroup.name} (${familyGroup.memberUids.size} miembro(s))",
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.SemiBold,
                )
                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column {
                      Text("Código de invitación", style = MaterialTheme.typography.labelSmall)
                      Text(
                        text = familyGroup.inviteCode,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                    Button(
                      onClick = {
                        WhatsAppShareHelper.shareFamilyInvite(
                          context = context,
                          familyName = familyGroup.name,
                          inviteCode = familyGroup.inviteCode,
                        )
                      },
                      shape = MaterialTheme.shapes.small,
                      modifier = Modifier.testTag("btn_invitar_familia_wa"),
                    ) {
                      Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Enviar")
                    }
                  }
                }

                if (familyGroup.ownerId != userProfile?.userId) {
                  TextButton(onClick = { onLeaveFamily(familyGroup.id) }) {
                    Text("Salir del grupo", color = MaterialTheme.colorScheme.error)
                  }
                }
              } else {
                Text(
                  text = "Crea un grupo con tu pareja o mamá para que las vueltas compartidas lleguen a todos.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Button(
                    onClick = {
                      if (!isPro) onActivatePro()
                      showCreateFamilyDialog = true
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .testTag("btn_crear_familia"),
                  ) {
                    Text("Crear grupo")
                  }
                  OutlinedButton(
                    onClick = { showJoinFamilyDialog = true },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .testTag("btn_unirse_familia"),
                  ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tengo código")
                  }
                }
              }
            }
          }
        }
      }

      // Sign Out
      item(key = "sign_out") {
        OutlinedButton(
          onClick = onSignOut,
          shape = MaterialTheme.shapes.medium,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("btn_cerrar_sesion"),
        ) {
          Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (isGuestSession) "Salir del modo invitado" else "Cerrar sesión")
        }
      }
    }
  }

  if (showCreateFamilyDialog) {
    var groupName by remember { mutableStateOf("Casa / Familia") }
    AlertDialog(
      onDismissRequest = { showCreateFamilyDialog = false },
      title = { Text("Crear grupo familiar") },
      text = {
        OutlinedTextField(
          value = groupName,
          onValueChange = { if (it.length <= 60) groupName = it },
          label = { Text("Nombre del grupo") },
          singleLine = true,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showCreateFamilyDialog = false
            onCreateFamily(groupName)
          }
        ) { Text("Crear") }
      },
      dismissButton = {
        TextButton(onClick = { showCreateFamilyDialog = false }) { Text("Cancelar") }
      },
    )
  }

  if (showJoinFamilyDialog) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showJoinFamilyDialog = false },
      title = { Text("Unirse a familia") },
      text = {
        OutlinedTextField(
          value = code,
          onValueChange = { if (it.length <= 12) code = it.uppercase() },
          label = { Text("Código (ej: MV4829)") },
          singleLine = true,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showJoinFamilyDialog = false
            onJoinFamily(code)
          }
        ) { Text("Unirme") }
      },
      dismissButton = {
        TextButton(onClick = { showJoinFamilyDialog = false }) { Text("Cancelar") }
      },
    )
  }
}

@Composable
private fun ThemeOptionChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(
      width = if (selected) 1.5.dp else 1.dp,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    ),
    modifier = modifier.height(48.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ProBenefitRow(text: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      imageVector = Icons.Rounded.CheckCircle,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.tertiary,
      modifier = Modifier.size(18.dp),
    )
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
  }
}
