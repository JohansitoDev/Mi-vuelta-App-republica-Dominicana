package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.UrgencyLevel
import com.example.data.model.Vuelta
import com.example.data.model.VueltaDateUtils
import com.example.ui.UiState
import com.example.ui.components.VueltaCard
import com.example.ui.components.urgencyVisuals
import java.util.Calendar

@Composable
fun CalendarioScreen(
  vueltasState: UiState<List<Vuelta>>,
  onOpenCreate: () -> Unit,
  onOpenDetail: (String) -> Unit,
  onToggleDone: (Vuelta) -> Unit,
) {
  val nowCal = remember { Calendar.getInstance() }
  var currentYear by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
  var currentMonth by remember { mutableIntStateOf(nowCal.get(Calendar.MONTH)) } // 0-based
  var selectedDayMillis by remember { mutableLongStateOf(VueltaDateUtils.todayStartMillis()) }

  val allVueltas = (vueltasState as? UiState.Success)?.data.orEmpty()

  val vueltasByDay = remember(allVueltas) {
    allVueltas.groupBy { VueltaDateUtils.startOfDayMillis(it.dueDateMillis) }
  }

  val monthCount = remember(allVueltas, currentYear, currentMonth) {
    allVueltas.count { vuelta ->
      val cal = Calendar.getInstance().apply { timeInMillis = vuelta.dueDateMillis }
      cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
    }
  }

  val selectedDayVueltas = remember(vueltasByDay, selectedDayMillis) {
    vueltasByDay[selectedDayMillis].orEmpty()
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = onOpenCreate,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = Color.White,
        modifier = Modifier.testTag("calendar_fab_nueva"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = "Nueva vuelta")
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
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
          bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item(key = "header") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Calendario",
                style = MaterialTheme.typography.headlineMedium,
              )
              Text(
                text = if (monthCount == 1) {
                  "Este mes tienes 1 vuelta"
                } else {
                  "Este mes tienes $monthCount vueltas"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }

        item(key = "calendar_card") {
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              // Month Navigation
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                IconButton(
                  onClick = {
                    if (currentMonth == 0) {
                      currentMonth = 11
                      currentYear -= 1
                    } else {
                      currentMonth -= 1
                    }
                  },
                  modifier = Modifier.testTag("cal_prev_month"),
                ) {
                  Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Mes anterior")
                }

                Text(
                  text = VueltaDateUtils.formatMonthYear(currentYear, currentMonth),
                  style = MaterialTheme.typography.titleMedium,
                )

                IconButton(
                  onClick = {
                    if (currentMonth == 11) {
                      currentMonth = 0
                      currentYear += 1
                    } else {
                      currentMonth += 1
                    }
                  },
                  modifier = Modifier.testTag("cal_next_month"),
                ) {
                  Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Mes siguiente")
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Weekday headers (Monday-first)
              Row(modifier = Modifier.fillMaxWidth()) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { dayLabel ->
                  Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Days Grid
              val cells = remember(currentYear, currentMonth) {
                buildMonthCells(currentYear, currentMonth)
              }
              val todayMillis = remember { VueltaDateUtils.todayStartMillis() }

              cells.chunked(7).forEach { week ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  week.forEach { dayMillis ->
                    if (dayMillis == null) {
                      Spacer(
                        modifier = Modifier
                          .weight(1f)
                          .aspectRatio(1f)
                      )
                    } else {
                      val dayCal = Calendar.getInstance().apply { timeInMillis = dayMillis }
                      val dayNumber = dayCal.get(Calendar.DAY_OF_MONTH)
                      val dayVueltas = vueltasByDay[dayMillis].orEmpty()
                      val isSelected = dayMillis == selectedDayMillis
                      val isToday = dayMillis == todayMillis

                      Box(
                        modifier = Modifier
                          .weight(1f)
                          .aspectRatio(1f)
                          .clip(RoundedCornerShape(12.dp))
                          .background(
                            when {
                              isSelected -> MaterialTheme.colorScheme.primary
                              isToday -> MaterialTheme.colorScheme.primaryContainer
                              else -> Color.Transparent
                            }
                          )
                          .clickable { selectedDayMillis = dayMillis }
                          .testTag("cal_day_$dayNumber"),
                        contentAlignment = Alignment.Center,
                      ) {
                        Column(
                          horizontalAlignment = Alignment.CenterHorizontally,
                          verticalArrangement = Arrangement.Center,
                        ) {
                          Text(
                            text = dayNumber.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                              isSelected -> MaterialTheme.colorScheme.onPrimary
                              isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                              else -> MaterialTheme.colorScheme.onSurface
                            },
                          )
                          if (dayVueltas.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                              dayVueltas.take(3).forEach { v ->
                                val dotLevel = VueltaDateUtils.urgencyOf(v)
                                val dotColor = if (isSelected) {
                                  Color.White
                                } else {
                                  urgencyVisuals(dotLevel).accent
                                }
                                Box(
                                  modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
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
            }
          }
        }

        item(key = "selected_day_header") {
          Text(
            text = VueltaDateUtils.formatFullDate(selectedDayMillis),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }

        if (selectedDayVueltas.isEmpty()) {
          item(key = "selected_day_empty") {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "No tienes vueltas para este día.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        } else {
          items(
            items = selectedDayVueltas,
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

private fun buildMonthCells(year: Int, monthZeroBased: Int): List<Long?> {
  val cal = Calendar.getInstance().apply {
    clear()
    set(year, monthZeroBased, 1)
  }
  val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  // Calendar.DAY_OF_WEEK: Sun=1, Mon=2 ... Sat=7 -> convert to Monday-first 0..6
  val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
  val leadingEmpty = (firstDayOfWeek + 5) % 7

  val cells = mutableListOf<Long?>()
  repeat(leadingEmpty) { cells.add(null) }
  for (day in 1..daysInMonth) {
    cal.set(year, monthZeroBased, day, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cells.add(cal.timeInMillis)
  }
  while (cells.size % 7 != 0) {
    cells.add(null)
  }
  return cells
}
