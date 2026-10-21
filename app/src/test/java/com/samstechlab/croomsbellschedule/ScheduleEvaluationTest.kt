package com.samstechlab.croomsbellschedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleEvaluationTest {

    @Test
    fun evaluateSchedule_activePeriodCalculatesProgressAndCustomLabels() {
        val schedule = FallbackScheduleProvider.getTodayFallbackSchedule(LocalDate.of(2026, 10, 20)) // Tuesday
        val prefs = UserPreferencesSnapshot(
            lunchPreference = "Lunch A",
            periodNames = mapOf(1 to "AP Computer Science", 2 to "Digital Design")
        )

        val evaluation = ScheduleFetcher.evaluateSchedule(
            schedule = schedule,
            prefs = prefs,
            now = LocalTime.of(7, 44, 0), // Halfway through Period 1 (7:20 - 8:08)
            today = LocalDate.of(2026, 10, 20)
        )

        assertTrue(evaluation.isSchoolDayActive)
        assertFalse(evaluation.isPassingPeriod)
        assertEquals("AP Computer Science", evaluation.headlineTitle)
        assertEquals("24m 00s", evaluation.countdownClock)
        assertEquals(0.5f, evaluation.currentProgress, 0.01f)
        assertEquals(PeriodState.CURRENT, evaluation.periods.first().state)
    }

    @Test
    fun parseScheduleFromJson_parsesUpdatedCroomsBellScheduleApiFormat() {
        val apiJson = """
            {
              "status": "OK",
              "responseTime": "2026-10-21T20:37:53.613Z",
              "data": {
                "id": "evenBlock",
                "msg": "Today is an EVEN Block Day.",
                "schedule": [
                  [[0,0,100,7,15],[7,15,101,7,20],[7,20,2,8,49],[8,55,4,10,21],[10,21,102,10,51],[10,57,8,11,47],[11,53,6,13,20],[13,20,104,13,25],[13,25,105,23,59],[23,59,106,23,59]],
                  [[0,0,100,7,15],[7,15,101,7,20],[7,20,2,8,49],[8,55,4,10,21],[10,27,8,11,17],[11,17,102,11,47],[11,53,6,13,20],[13,20,104,13,25],[13,25,105,23,59],[23,59,106,23,59]]
                ]
              }
            }
        """.trimIndent()

        val result = ScheduleFetcher.parseScheduleFromJson(apiJson)
        assertTrue(result.isSuccess)
        assertEquals("Today is an EVEN Block Day.", ScheduleFetcher.getScheduleDayTitle())

        val eval = ScheduleFetcher.evaluateSchedule(
            schedule = result.getOrThrow(),
            prefs = UserPreferencesSnapshot(altLunchPreference = "Lunch A"),
            now = LocalTime.of(11, 10, 0), // During Homeroom (eventId = 8)
            today = LocalDate.of(2026, 10, 21) // Wednesday
        )
        assertEquals("Homeroom", eval.headlineTitle)
        assertEquals(5, eval.periods.size) // Period 2, Period 4, Lunch, Homeroom, Period 6
    }

    @Test
    fun parseFeedItemsFromJson_parsesNewAnnouncementsWebsiteFormat() {
        val sampleJson = """
            {
              "status": "OK",
              "data": [
                {
                  "id": "415973fa-24b4-403c-8fb0-030f86e86d3e",
                  "data": {
                    "title": "Website Update v5.1.0",
                    "message": "<div>The Crooms Bell Schedule website has been updated.</div><ul><li>Add schedule table</li></ul>"
                  },
                  "priority": true,
                  "targets": ["website", "app"],
                  "created": "2026-09-08T01:56:20.999Z",
                  "expires": null
                }
              ]
            }
        """.trimIndent()

        val result = parseFeedItemsFromJson(sampleJson)
        assertTrue(result.isSuccess)
        val items = result.getOrThrow()
        assertEquals(1, items.size)
        assertEquals("Website Update v5.1.0", items[0].title)
        assertTrue(items[0].data.contains("• Add schedule table"))
        assertTrue(items[0].priority)
    }
}
