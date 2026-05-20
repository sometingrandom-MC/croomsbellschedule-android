package com.samstechlab.croomsbellschedule

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class CroomsshedTimerBenchmark {

    @Test
    fun benchmarkCountdown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val schedule = listOf(
            listOf(ScheduleBlock(0, 0, 105, 23, 59))
        )

        // Warmup
        countdown(schedule, "After School", 0, context)

        var totalTime = 0L
        val iterations = 10

        for (i in 1..iterations) {
            val time = measureTimeMillis {
                countdown(schedule, "After School", 0, context)
            }
            totalTime += time
        }

        println("BENCHMARK_RESULT: Average time: ${totalTime / iterations} ms")
    }
}
