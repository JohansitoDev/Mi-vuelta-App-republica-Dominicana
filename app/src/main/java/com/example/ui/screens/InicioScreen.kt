package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FREE_PLAN_ACTIVE_LIMIT
import com.example.data.model.UrgencyLevel
import com.example.data.model.UserProfile
import com.example.data.model.Vuelta
import com.example.data.model.VueltaDateUtils
import com.example.ui.UiState
import com.example.ui.components.VueltaCard
import com.example.ui.components.urgencyVisuals

enum class VueltaFilter(val label: String) {
  PENDIENTES("Pendientes"),
  URGENTES("Hoy / Mañana"),
  SEMANA("Esta semana"),
  FAMILIA("Familia"),
  HECHAS("Hechas"),
}

@Composable
fun InicioScreen(
  vueltasState: UiState<List<Vuelta>>,
  userProfile: UserProfile?,
  currentUserId: String,
  onOpenCreate: () -> Unit,
  onOpenDetail: (String) -> Unit,
  onToggleDone: (Vuelta) -> Unit,
  onOpenPro: () -> Unit,
) {
  var selectedFilter by remember { mutableStateOf(VueltaFilter.PENDIENTES) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onOpenCreate,
        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        text = { Text("Nueva vuelta") },
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_nueva_vuelta"),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 680.dp)
          .fillMaxSize(),
      ) {
        // Header bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            val firstName = userProfile?.displayName
              ?.substringBefore(" ")
              ?.takeIf { it.isNotBlank() }
              ?: "Mi gente"
            Text(
              text = "Hola, $firstName",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "Mis Vueltas",
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }

          val isPro = userProfile?.isProUser == true
          val activeOwnCount = (vueltasState as? UiState.Success)?.data
            ?.count { !it.completed && it.ownerId == currentUserId } ?: 0

          Surface(
            onClick = onOpenPro,
            color = if (isPro) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50),
            modifier = Modifier.testTag("plan_status_chip"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = if (isPro) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = if (isPro) "Pro" else "$activeOwnCount/$FREE_PLAN_ACTIVE_LIMIT gratis",
                style = MaterialTheme.typography.labelMedium,
                color = if (isPro) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        // Filter Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          VueltaFilter.entries.forEach { filter ->
            FilterChip(
              selected = selectedFilter == filter,
              onClick = { selectedFilter = filter },
              label = { Text(filter.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              ),
              modifier = Modifier.testTag("filter_${filter.name.lowercase()}"),
            )
          }
        }

        when (vueltasState) {
          is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
          is UiState.Error -> {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
              ) {
                Text(
                  text = vueltasState.message,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(16.dp),
                )
              }
            }
          }
          is UiState.Success -> {
            val allVueltas = vueltasState.data
            val urgentCount = allVueltas.count {
              !it.completed && VueltaDateUtils.urgencyOf(it) == UrgencyLevel.ROJO
            }

            val filtered = remember(allVueltas, selectedFilter) {
              when (selectedFilter) {
                VueltaFilter.PENDIENTES -> allVueltas.filter { !it.completed }
                VueltaFilter.URGENTES -> allVueltas.filter {
                  !it.completed && VueltaDateUtils.urgencyOf(it) == UrgencyLevel.ROJO
                }
                VueltaFilter.SEMANA -> allVueltas.filter {
                  !it.completed && (
                    VueltaDateUtils.urgencyOf(it) == UrgencyLevel.ROJO ||
                      VueltaDateUtils.urgencyOf(it) == UrgencyLevel.AMARILLO
                    )
                }
                VueltaFilter.FAMILIA -> allVueltas.filter { it.sharedWithFamily }
                VueltaFilter.HECHAS -> allVueltas.filter { it.completed }
              }
            }

            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 96.dp,
              ),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              if (urgentCount > 0 && selectedFilter == VueltaFilter.PENDIENTES) {
                item(key = "urgent_banner") {
                  val redVisuals = urgencyVisuals(UrgencyLevel.ROJO)
                  Surface(
                    color = redVisuals.container,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = redVisuals.onContainer,
                      )
                      Text(
                        text = if (urgentCount == 1) {
                          "Tienes 1 vuelta que vence hoy o mañana. ¡Cero moras!"
                        } else {
                          "Tienes $urgentCount vueltas urgentes entre hoy y mañana."
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = redVisuals.onContainer,
                      )
                    }
                  }
                }
              }

              if (filtered.isEmpty()) {
                item(key = "empty_state") {
                  EmptyVueltasCard(
                    filter = selectedFilter,
                    onCreate = onOpenCreate,
                  )
                }
              } else {
                items(
                  items = filtered,
                  key = { it.id },
                ) { vuelta ->
                  VueltaCard(
                    vuelta = vuelta,
                    onClick = { onOpenDetail(vuelta.id) },
                    onToggleDone = { onToggleDone(vuelta) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyVueltasCard(
  filter: VueltaFilter,
  onCreate: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 28.dp, horizontal = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_empty_vueltas),
      contentDescription = "Sin vueltas pendientes",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxWidth(0.78f)
        .height(175.dp)
        .clip(MaterialTheme.shapes.large),
    )

    Text(
      text = when (filter) {
        VueltaFilter.PENDIENTES -> "Estás al día, cero vueltas pendientes"
        VueltaFilter.URGENTES -> "Nada urgente para hoy ni mañana"
        VueltaFilter.SEMANA -> "Semana tranquila, sin pagos próximos"
        VueltaFilter.FAMILIA -> "Aún no tienes vueltas compartidas"
        VueltaFilter.HECHAS -> "Todavía no has marcado vueltas hechas"
      },
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )

    Text(
      text = "Crea una vuelta en 15 segundos usando nuestras plantillas dominicanas y tírale foto al recibo.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )

    if (filter == VueltaFilter.PENDIENTES) {
      Spacer(modifier = Modifier.height(4.dp))
      Button(
        onClick = onCreate,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.testTag("empty_create_button"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(modifier = Modifier.size(6.dp))
        Text("Crear mi primera vuelta")
      }
    }
  }
}
