package com.example

import com.example.data.model.UrgencyLevel
import com.example.data.model.Vuelta
import com.example.data.model.VueltaDateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class VueltaUrgencyUnitTest {
  @Test
  fun urgency_thresholds_match_traffic_light_rules() {
    val ref = VueltaDateUtils.todayStartMillis()
    val oneDay = TimeUnit.DAYS.toMillis(1)

    val todayVuelta = Vuelta(id = "1", title = "Luz", dueDateMillis = ref)
    val tomorrowVuelta = Vuelta(id = "2", title = "Internet", dueDateMillis = ref + oneDay)
    val weekVuelta = Vuelta(id = "3", title = "Colegio", dueDateMillis = ref + 5 * oneDay)
    val laterVuelta = Vuelta(id = "4", title = "Marbete", dueDateMillis = ref + 14 * oneDay)
    val doneVuelta = Vuelta(id = "5", title = "Agua", dueDateMillis = ref, completed = true)

    assertEquals(UrgencyLevel.ROJO, VueltaDateUtils.urgencyOf(todayVuelta, ref))
    assertEquals(UrgencyLevel.ROJO, VueltaDateUtils.urgencyOf(tomorrowVuelta, ref))
    assertEquals(UrgencyLevel.AMARILLO, VueltaDateUtils.urgencyOf(weekVuelta, ref))
    assertEquals(UrgencyLevel.VERDE, VueltaDateUtils.urgencyOf(laterVuelta, ref))
    assertEquals(UrgencyLevel.COMPLETADA, VueltaDateUtils.urgencyOf(doneVuelta, ref))
  }
}
