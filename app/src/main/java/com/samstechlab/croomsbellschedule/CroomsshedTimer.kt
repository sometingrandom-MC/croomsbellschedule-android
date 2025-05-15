package com.samstechlab.croomsbellschedule

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.time.Instant
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Calendar

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "my_preferences")
private val CACHED_SCHEDULE_KEY = stringPreferencesKey("cached_schedule")
private val CACHE_TIMESTAMP_KEY = stringPreferencesKey("cache_timestamp")

class MyDataStoreManager(private val context: Context) {

    // You can now access the DataStore instance using 'context.dataStore'
    suspend fun saveData(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getData(key: Preferences.Key<String>): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }
}


data class ScheduleBlock(
    val startHour: Int,
    val startMinute: Int,
    val eventId: Int,
    val endHour: Int,
    val endMinute: Int
)

suspend fun getApiResponse(): String? {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.croomssched.tech/today")
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                // Handle unsuccessful response (e.g., 404, 500)
                println("API request failed with code: ${response.code}")
                null
            }
        } catch (e: IOException) {
            // Handle network or other exceptions
            println("Error during API request: ${e.message}")
            null
        }
    }
}
// Use the ScheduleFetcher to dynamically fetch today's schedule
object ScheduleFetcher {
    private const val CACHE_DURATION_HOURS = 1L

    suspend fun fetchAndParseSchedule(context: Context, refresh: Boolean = false): Triple<Result<List<List<ScheduleBlock>>>, String, Int> =

        withContext(Dispatchers.IO) {
            try {
                val myDataStoreManager = MyDataStoreManager(context)
                val connection: String
                val status: Int
                // Try to get cached data
                val cachedSchedule = myDataStoreManager.getData(CACHED_SCHEDULE_KEY).first()
                val cachedTimestamp = myDataStoreManager.getData(CACHE_TIMESTAMP_KEY).first()
                var minutesSinceCache: Long

                // Check if cache is valid
                if (cachedSchedule != null && cachedTimestamp != null) {
                    val timestamp = Instant.parse(cachedTimestamp)
                    val now = Instant.now()
                    val hoursSinceCache = ChronoUnit.HOURS.between(timestamp, now)
                    minutesSinceCache = ChronoUnit.MINUTES.between(timestamp, now)


                    if (hoursSinceCache < CACHE_DURATION_HOURS && !refresh) {
                        // Use cached data if it's less than 1 hour old
                        status = 1
                        connection = "Fetched $minutesSinceCache minutes ago"
                        return@withContext Triple(parseScheduleFromJson(cachedSchedule), connection, status)
                    }
                }

                // If cache is invalid or missing, fetch new data
                val response = getApiResponse()
                if (response != null) {
                    // Save new data to cache
                    myDataStoreManager.saveData(CACHED_SCHEDULE_KEY, response)
                    myDataStoreManager.saveData(CACHE_TIMESTAMP_KEY, Instant.now().toString())
                    minutesSinceCache = ChronoUnit.MINUTES.between(Instant.parse(Instant.now().toString()), Instant.parse(Instant.now().toString()))
                    status = 1
                    connection = "Fetched $minutesSinceCache minutes ago"
                    return@withContext Triple(parseScheduleFromJson(response), connection, status)
                } else {
                    // If API call fails but we have cached data, use it as fallback
                    if (cachedSchedule != null) {
                        minutesSinceCache = ChronoUnit.MINUTES.between(Instant.parse(cachedTimestamp), Instant.now())
                        connection = "Cannot update schedule, using data from $minutesSinceCache minutes ago"
                        status = 0
                        return@withContext Triple(parseScheduleFromJson(cachedSchedule), connection, status)
                    }
                    throw IOException("Failed to fetch schedule and no cache available")
                }
            } catch (e: Exception) {
                return@withContext Triple(Result.failure(e), "Cannot fetch data", 0)
            }
        }

    fun parseScheduleFromJson(jsonString: String): Result<List<List<ScheduleBlock>>> {
        return try {
            val json = org.json.JSONObject(jsonString)
            val data = json.getJSONObject("data")
            val scheduleJson = data.getJSONArray("schedule")
            val schedule = mutableListOf<List<ScheduleBlock>>()

            for (i in 0 until scheduleJson.length()) {
                val daySchedule = scheduleJson.getJSONArray(i)
                val blocks = mutableListOf<ScheduleBlock>()

                for (j in 0 until daySchedule.length()) {
                    val block = daySchedule.getJSONArray(j)
                    blocks.add(
                        ScheduleBlock(
                            startHour = block.getInt(0),
                            startMinute = block.getInt(1),
                            eventId = block.getInt(2),
                            endHour = block.getInt(3),
                            endMinute = block.getInt(4)
                        )
                    )
                }
                schedule.add(blocks)
            }

            Result.success(schedule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPeriodLabels(
        context: Context,
        schedule: List<List<ScheduleBlock>>
    ): List<Any> {
        val calendar = Calendar.getInstance()
        val myDataStoreManager = MyDataStoreManager(context)
        var lunch: String
        if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.WEDNESDAY) {
            runBlocking {
                val lunchPreference =
                    myDataStoreManager.getData(stringPreferencesKey("lunch_preference")).first()
                        ?: "Lunch A"
                lunch = lunchPreference
            }
        } else {
            runBlocking {
                val lunchPreference =
                    myDataStoreManager.getData(stringPreferencesKey("alt_lunch_preference")).first()
                        ?: "Lunch A"
                lunch = lunchPreference
            }
        }


        val lunchInt: Int
        lunchInt = if (lunch == "Lunch A")
            0
        else
            1

        val todaySchedule = schedule[lunchInt]
        for (block in todaySchedule) {
            val now = LocalTime.now()
            val eventEnd = LocalTime.of(block.endHour, block.endMinute)
            val eventId = block.eventId
            var processedEventId: String

            if (eventId == 1 || eventId == 2 || eventId == 3 || eventId == 4 || eventId == 5
                || eventId == 6 || eventId == 7
            ) {
                processedEventId =
                    myDataStoreManager.getData(stringPreferencesKey("p$eventId")).first()
                        ?: "Period $eventId";
            } else if (eventId == 0) {
                processedEventId = "Nothing";
            } else if (eventId == 100) {
                processedEventId = "Morning";
            } else if (eventId == 101) {
                processedEventId = "Welcome";
            } else if (eventId == 102) {
                processedEventId = "Lunch";
            } else if (eventId == 103) {
                processedEventId = "Homeroom";
            } else if (eventId == 104) {
                processedEventId = "Dismissal";
            } else if (eventId == 105) {
                processedEventId = "After School";
            } else if (eventId == 106) {
                processedEventId = "End";
            } else if (eventId == 107) {
                processedEventId = "Break";
            } else if (eventId == 110) {
                processedEventId = "PSAT/SAT";
            } else {
                processedEventId = "Unknown Event";
            }

            if (now.isAfter(eventEnd)) {
                continue
            }
            return listOf(processedEventId, lunchInt)

        }
        return listOf(1, 2, 3)
    }


}
suspend fun afterSchoolQuips(context: Context): String {
    val cacheDurationHours = 1L
    val myDataStoreManager = MyDataStoreManager(context)
    val cachedQuip = myDataStoreManager.getData(stringPreferencesKey("CACHED_QUIP_KEY")).first().toString()
    val cachedTimestamp = myDataStoreManager.getData(stringPreferencesKey("CACHE_QUIP_TIMESTAMP_KEY")).first()

    // Check if cache is valid
    if (cachedQuip != "" && cachedTimestamp != null) {
        val timestamp = Instant.parse(cachedTimestamp)
        val now = Instant.now()
        val hoursSinceCache = ChronoUnit.HOURS.between(timestamp, now)

        if (hoursSinceCache < cacheDurationHours) {
            // Use cached data if it's less than 1 hour old
            return cachedQuip
        }
    }

    // If cache is invalid or missing, fetch new data

    val afterSchoolQuips = listOf(
        "School's out. Brain going into standby mode. Please leave a message after the beep...",
        "Freedom bell! Time to activate 'do absolutely nothing' mode.", "Brain: Accepting applications for enjoyable distractions only.",
        "Time to skillfully transition from 'student' to 'professional doom-scroller'.",
        "Homework?  Yeah, that's happening right after... just five more minutes of doomscrolling.",
        "Entering personal time.  Productivity?  Postponed due to urgent doomscrolling research.",
        "School day over. Doomscrolling systems online and fully operational.  Prepare for targeted ads for things you just thought about.",
        "Achievement unlocked: Survived another school day.  Reward: Unlocking next level of algorithm.",
        "Entering personal time.  Productivity?  Optional. Highly optional.",
        "Recharging brain. Process may involve copious amounts of… not homework.",
        "Why are you here? School is over. You have plenty of time to do... nothing."
    )
        val afterSchoolQuip = afterSchoolQuips[(afterSchoolQuips.indices).random()]

        // Save new data to cache
        myDataStoreManager.saveData(stringPreferencesKey("CACHED_QUIP_KEY"), afterSchoolQuip)
        myDataStoreManager.saveData(stringPreferencesKey("CACHE_QUIP_TIMESTAMP_KEY"), Instant.now().toString())

    return afterSchoolQuip
}



// Countdown timer logic
fun countdown(schedule: List<List<ScheduleBlock>>, processedEventId: String, lunchInt: Int, context: Context): String {
    val myDataStoreManager = MyDataStoreManager(context)
    val afterSchoolQuip = runBlocking {
        if (processedEventId == "After School" || processedEventId == "Nothing")
            return@runBlocking afterSchoolQuips(context)
        else
            return@runBlocking null
    }
    if (afterSchoolQuip != null){
        runBlocking {
            myDataStoreManager.saveData(stringPreferencesKey("AFTER_SCHOOL"), "true")
        }
        return afterSchoolQuip
    }


    val todaySchedule = schedule[lunchInt]

    for (block in todaySchedule) {
        val now = LocalTime.now()
        val eventStart = LocalTime.of(block.startHour, block.startMinute)
        val eventEnd = LocalTime.of(block.endHour, block.endMinute)


        // Skip events that have ended
        if (now.isAfter(eventEnd)) {
            continue
        }

        // Countdown until the event start
        if (now.isBefore(eventStart)) {

            while (LocalTime.now().isBefore(eventStart)) {
                val currentTime = LocalTime.now()
                val timeUntilStart = ChronoUnit.SECONDS.between(currentTime, eventStart)
                val minutes = timeUntilStart / 60
                val seconds = timeUntilStart % 60

                return("Time until start: ${"%02d".format(minutes)}:${"%02d".format(seconds)}")
            }
        }

        // Countdown during the event

        while (LocalTime.now().isBefore(eventEnd)) {
            val currentTime = LocalTime.now()
            val timeUntilEnd = ChronoUnit.SECONDS.between(currentTime, eventEnd)
            if (timeUntilEnd >= 3600) {
                val hours = timeUntilEnd / 3600
                val minutes = (timeUntilEnd / 60) - (hours * 60)
                val seconds = timeUntilEnd % 60

                return(
                        "${"%1d".format(hours)}h ${"%02d".format(minutes)}m ${
                            "%02d".format(
                                seconds
                            )
                        }s \n Remaining in $processedEventId"
                        )
            } else {
                val minutes = timeUntilEnd / 60
                val seconds = timeUntilEnd % 60

                return("${"%02d".format(minutes)}m ${
                    "%02d".format(
                        seconds
                    )
                }s \n Remaining in $processedEventId")
            }
        }

    }
    return ("Calculation Timed Out")
}

// Main Function: Fetch schedule and start the countdown

fun formatedTimer(context: Context): Triple<String, String, String> {

    val formatedTimer =
        runBlocking {
        val result = ScheduleFetcher.fetchAndParseSchedule(context, false)
        val scheduleResult = result.first
        val connection = result.second
            val status = result.third.toString()



        ((if (scheduleResult.isSuccess) {
            val schedule = scheduleResult.getOrNull()
            val returnedList: List<Any> = if (schedule != null) {
                ScheduleFetcher.fetchPeriodLabels(context, schedule.toList())
            } else listOf(1,2,3)
            if (schedule.isNullOrEmpty()) {
                arrayOf("No schedule available for today.", connection, status)
            } else {
                arrayOf(countdown(schedule, returnedList[0].toString(), returnedList[1] as Int, context), connection, status)
                //processedEventId, lunchInt
            }
        } else {
            arrayOf("Failed to fetch the schedule: ${scheduleResult.exceptionOrNull()?.message}", connection, status)
        }))
    }

    return Triple(formatedTimer[0], formatedTimer[1], formatedTimer[2])
}
