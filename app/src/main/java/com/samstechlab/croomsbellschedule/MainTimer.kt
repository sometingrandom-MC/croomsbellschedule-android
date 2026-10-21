package com.samstechlab.croomsbellschedule

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Provides standard Crooms AOIT bell schedule blocks as an offline fallback when
 * the device has no cached schedule and cannot reach the API.
 */
object FallbackScheduleProvider {

    fun getTodayFallbackSchedule(date: LocalDate = LocalDate.now()): List<List<ScheduleBlock>> {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> listOf(
                listOf(ScheduleBlock(0, 0, 0, 23, 59)),
                listOf(ScheduleBlock(0, 0, 0, 23, 59))
            )
            DayOfWeek.WEDNESDAY -> listOf(
                // Even Block Day - Lunch A & Lunch B
                evenBlockSchedule(isLunchA = true),
                evenBlockSchedule(isLunchA = false)
            )
            DayOfWeek.THURSDAY -> listOf(
                // Odd Block Day - Lunch A & Lunch B
                oddBlockSchedule(isLunchA = true),
                oddBlockSchedule(isLunchA = false)
            )
            else -> listOf(
                // Standard 7-Period Day - Lunch A & Lunch B
                normalDaySchedule(isLunchA = true),
                normalDaySchedule(isLunchA = false)
            )
        }
    }

    private fun normalDaySchedule(isLunchA: Boolean): List<ScheduleBlock> {
        val middleBlocks = if (isLunchA) {
            listOf(
                ScheduleBlock(11, 2, 102, 11, 32),  // Lunch A
                ScheduleBlock(11, 37, 5, 12, 27)    // Period 5
            )
        } else {
            listOf(
                ScheduleBlock(11, 7, 5, 11, 57),    // Period 5
                ScheduleBlock(11, 57, 102, 12, 27)  // Lunch B
            )
        }
        return listOf(
            ScheduleBlock(7, 20, 1, 8, 8),
            ScheduleBlock(8, 13, 2, 9, 5),
            ScheduleBlock(9, 10, 3, 10, 0),
            ScheduleBlock(10, 5, 4, 10, 55)
        ) + middleBlocks + listOf(
            ScheduleBlock(12, 32, 6, 13, 22),
            ScheduleBlock(13, 27, 7, 14, 20),
            ScheduleBlock(14, 20, 105, 23, 59)
        )
    }

    private fun evenBlockSchedule(isLunchA: Boolean): List<ScheduleBlock> {
        val middleBlocks = if (isLunchA) {
            listOf(
                ScheduleBlock(10, 21, 102, 10, 51), // Lunch A
                ScheduleBlock(10, 56, 103, 11, 42)  // Homeroom
            )
        } else {
            listOf(
                ScheduleBlock(10, 26, 103, 11, 12), // Homeroom
                ScheduleBlock(11, 17, 102, 11, 47)  // Lunch B
            )
        }
        return listOf(
            ScheduleBlock(7, 20, 2, 8, 44),
            ScheduleBlock(8, 49, 4, 10, 21)
        ) + middleBlocks + listOf(
            ScheduleBlock(11, 47, 6, 13, 20),
            ScheduleBlock(13, 20, 105, 23, 59)
        )
    }

    private fun oddBlockSchedule(isLunchA: Boolean): List<ScheduleBlock> {
        val middleBlocks = if (isLunchA) {
            listOf(
                ScheduleBlock(10, 33, 102, 11, 3),  // Lunch A
                ScheduleBlock(11, 8, 5, 12, 36)     // Period 5
            )
        } else {
            listOf(
                ScheduleBlock(10, 38, 5, 12, 6),    // Period 5
                ScheduleBlock(12, 11, 102, 12, 41)  // Lunch B
            )
        }
        return listOf(
            ScheduleBlock(7, 20, 1, 8, 50),
            ScheduleBlock(8, 55, 3, 10, 33)
        ) + middleBlocks + listOf(
            ScheduleBlock(12, 41, 7, 14, 20),
            ScheduleBlock(14, 20, 105, 23, 59)
        )
    }
}
