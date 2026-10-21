package com.samstechlab.croomsbellschedule

import android.content.Context
import androidx.compose.runtime.Immutable
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
import org.json.JSONObject
import java.io.IOException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "my_preferences")

private val CACHED_SCHEDULE_KEY = stringPreferencesKey("cached_schedule_v2")
private val CACHE_TIMESTAMP_KEY = stringPreferencesKey("cache_timestamp_v2")
private val CACHED_QUIP_KEY = stringPreferencesKey("CACHED_QUIP_KEY")
private val CACHE_QUIP_TIMESTAMP_KEY = stringPreferencesKey("CACHE_QUIP_TIMESTAMP_KEY")
private val AFTER_SCHOOL_KEY = stringPreferencesKey("AFTER_SCHOOL")

/**
 * Shared singleton OkHttpClient to reuse connection pools, thread pools, and TLS sessions.
 */
object NetworkClient {
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}

@Immutable
data class UserPreferencesSnapshot(
    val lunchPreference: String = "Lunch A",
    val altLunchPreference: String = "Lunch A",
    val periodNames: Map<Int, String> = emptyMap()
) {
    fun getPeriodLabel(periodNumber: Int): String {
        val custom = periodNames[periodNumber]?.trim()
        val defaultName = if (periodNumber == 8) "Homeroom" else "Period $periodNumber"
        return if (!custom.isNullOrEmpty()) custom else defaultName
    }
}

/**
 * Optimized DataStore manager with an in-memory cache layer so repeated reads do not hit disk.
 */
class MyDataStoreManager(context: Context) {
    private val appContext = context.applicationContext

    val preferencesFlow: Flow<UserPreferencesSnapshot> = appContext.appDataStore.data.map { prefs ->
        prefs.asMap().forEach { (k, v) ->
            if (v is String) {
                memoryCache[k.name] = v
            }
        }
        UserPreferencesSnapshot(
            lunchPreference = prefs[PreferenceKeys.LUNCH_PREF] ?: "Lunch A",
            altLunchPreference = prefs[PreferenceKeys.ALT_LUNCH_PREF] ?: "Lunch A",
            periodNames = (1..8).associateWith { i ->
                prefs[PreferenceKeys.periodKey(i)] ?: ""
            }
        )
    }

    suspend fun saveData(key: Preferences.Key<String>, value: String) {
        if (memoryCache[key.name] == value) return
        memoryCache[key.name] = value
        appContext.appDataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getData(key: Preferences.Key<String>): Flow<String?> {
        return appContext.appDataStore.data.map { preferences ->
            val value = preferences[key]
            if (value != null) {
                memoryCache[key.name] = value
            }
            value
        }
    }

    suspend fun getCachedOrRead(key: Preferences.Key<String>): String? {
        memoryCache[key.name]?.let { return it }
        val prefs = appContext.appDataStore.data.first()
        prefs.asMap().forEach { (k, v) ->
            if (v is String) memoryCache[k.name] = v
        }
        return prefs[key]
    }

    suspend fun getSnapshot(): UserPreferencesSnapshot {
        val prefs = appContext.appDataStore.data.first()
        prefs.asMap().forEach { (k, v) ->
            if (v is String) memoryCache[k.name] = v
        }
        return UserPreferencesSnapshot(
            lunchPreference = prefs[PreferenceKeys.LUNCH_PREF] ?: "Lunch A",
            altLunchPreference = prefs[PreferenceKeys.ALT_LUNCH_PREF] ?: "Lunch A",
            periodNames = (1..8).associateWith { i ->
                prefs[PreferenceKeys.periodKey(i)] ?: ""
            }
        )
    }

    companion object {
        private val memoryCache = ConcurrentHashMap<String, String>()
    }
}

object PreferenceKeys {
    val LUNCH_PREF = stringPreferencesKey("lunch_preference")
    val ALT_LUNCH_PREF = stringPreferencesKey("alt_lunch_preference")
    private val PERIOD_KEYS = (1..8).associateWith { stringPreferencesKey("p$it") }

    fun periodKey(period: Int): Preferences.Key<String> =
        PERIOD_KEYS[period] ?: stringPreferencesKey("p$period")
}

private val TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.US)

@Immutable
data class ScheduleBlock(
    val startHour: Int,
    val startMinute: Int,
    val eventId: Int,
    val endHour: Int,
    val endMinute: Int
) {
    val startTime: LocalTime
        get() = LocalTime.of(startHour.coerceIn(0, 23), startMinute.coerceIn(0, 59))

    val endTime: LocalTime
        get() = LocalTime.of(endHour.coerceIn(0, 23), endMinute.coerceIn(0, 59))

    val durationMinutes: Long
        get() = ChronoUnit.MINUTES.between(startTime, endTime).coerceAtLeast(0L)

    fun formattedTimeRange(): String =
        "${startTime.format(TIME_FORMATTER)} – ${endTime.format(TIME_FORMATTER)}"
}

enum class PeriodState {
    COMPLETED,
    CURRENT,
    UPCOMING
}

@Immutable
data class ResolvedSchedulePeriod(
    val block: ScheduleBlock,
    val label: String,
    val defaultLabel: String,
    val formattedTimeRange: String,
    val durationMinutes: Long,
    val state: PeriodState,
    val progress: Float
)

@Immutable
data class ScheduleEvaluation(
    val headlineTitle: String,
    val countdownClock: String,
    val subtitleText: String,
    val currentProgress: Float,
    val dayProgress: Float,
    val isSchoolDayActive: Boolean,
    val isPassingPeriod: Boolean,
    val selectedLunchLabel: String,
    val isWednesday: Boolean,
    val scheduleDayTitle: String,
    val periods: List<ResolvedSchedulePeriod>
)

suspend fun getApiResponse(): String? = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url("https://api.croomsbellschedule.com/today")
        .header("Accept", "application/json")
        .build()

    try {
        NetworkClient.httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        }
    } catch (_: IOException) {
        null
    }
}

object ScheduleFetcher {
    private const val CACHE_DURATION_MINUTES = 60L

    @Volatile
    private var inMemorySchedule: List<List<ScheduleBlock>>? = null

    @Volatile
    private var inMemoryDayName: String = "Today's Schedule"

    @Volatile
    private var inMemoryTimestamp: Instant? = null

    fun getScheduleDayTitle(): String = inMemoryDayName

    fun defaultEventLabel(eventId: Int): String = when (eventId) {
        in 1..7 -> "Period $eventId"
        8 -> "Homeroom"
        0 -> "No School"
        100 -> "Morning"
        101 -> "Welcome"
        102 -> "Lunch"
        103 -> "Homeroom"
        104 -> "Dismissal"
        105 -> "After School"
        106 -> "End of Day"
        107 -> "Break"
        110 -> "PSAT / SAT"
        111 -> "Session 1"
        112 -> "Session 2"
        113 -> "Session 3"
        114 -> "Session 5"
        115 -> "Field Day"
        116 -> "Testing"
        else -> "Special Event"
    }

    fun resolveEventLabel(eventId: Int, prefs: UserPreferencesSnapshot): String = when (eventId) {
        in 1..8 -> prefs.getPeriodLabel(eventId)
        0 -> "Nothing"
        100 -> "Morning"
        101 -> "Welcome"
        102 -> "Lunch"
        103 -> "Homeroom"
        104 -> "Dismissal"
        105 -> "After School"
        106 -> "End"
        107 -> "Break"
        110 -> "PSAT/SAT"
        111 -> "Session 1"
        112 -> "Session 2"
        113 -> "Session 3"
        114 -> "Session 5"
        115 -> "Field Day"
        116 -> "Testing"
        else -> "Unknown Event"
    }

    suspend fun fetchAndParseSchedule(
        context: Context,
        refresh: Boolean = false
    ): Triple<Result<List<List<ScheduleBlock>>>, String, Int> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now()
            val memSchedule = inMemorySchedule
            val memTime = inMemoryTimestamp

            if (!refresh && memSchedule != null && memTime != null) {
                val mins = ChronoUnit.MINUTES.between(memTime, now).coerceAtLeast(0L)
                if (mins < CACHE_DURATION_MINUTES) {
                    val msg = if (mins == 0L) "Updated just now" else "Fetched $mins min ago"
                    return@withContext Triple(Result.success(memSchedule), msg, 1)
                }
            }

            val myDataStoreManager = MyDataStoreManager(context)
            val cachedSchedule = myDataStoreManager.getCachedOrRead(CACHED_SCHEDULE_KEY)
            val cachedTimestamp = myDataStoreManager.getCachedOrRead(CACHE_TIMESTAMP_KEY)

            if (!refresh && cachedSchedule != null && cachedTimestamp != null) {
                val timestamp = runCatching { Instant.parse(cachedTimestamp) }.getOrNull()
                if (timestamp != null) {
                    val minutesSinceCache = ChronoUnit.MINUTES.between(timestamp, now).coerceAtLeast(0L)
                    if (minutesSinceCache < CACHE_DURATION_MINUTES) {
                        val parsed = parseScheduleFromJson(cachedSchedule)
                        if (parsed.isSuccess) {
                            inMemorySchedule = parsed.getOrNull()
                            inMemoryTimestamp = timestamp
                        }
                        val connection = if (minutesSinceCache == 0L) {
                            "Updated just now"
                        } else {
                            "Fetched $minutesSinceCache min ago"
                        }
                        return@withContext Triple(parsed, connection, 1)
                    }
                }
            }

            val response = getApiResponse()
            if (response != null) {
                val parsed = parseScheduleFromJson(response)
                if (parsed.isSuccess) {
                    val currentInstant = Instant.now()
                    inMemorySchedule = parsed.getOrNull()
                    inMemoryTimestamp = currentInstant
                    myDataStoreManager.saveData(CACHED_SCHEDULE_KEY, response)
                    myDataStoreManager.saveData(CACHE_TIMESTAMP_KEY, currentInstant.toString())
                    return@withContext Triple(parsed, "Updated just now", 1)
                }
            }

            if (cachedSchedule != null) {
                val timestamp = runCatching { Instant.parse(cachedTimestamp) }.getOrNull() ?: now
                val minutesSinceCache = ChronoUnit.MINUTES.between(timestamp, now).coerceAtLeast(0L)
                val parsed = parseScheduleFromJson(cachedSchedule)
                if (parsed.isSuccess) {
                    inMemorySchedule = parsed.getOrNull()
                    inMemoryTimestamp = timestamp
                }
                val connection = "Offline • Cached ${minutesSinceCache}m ago"
                return@withContext Triple(parsed, connection, 0)
            }

            val fallback = FallbackScheduleProvider.getTodayFallbackSchedule()
            inMemorySchedule = fallback
            inMemoryTimestamp = now
            return@withContext Triple(
                Result.success(fallback),
                "Offline • Using standard bell schedule",
                0
            )
        } catch (e: Exception) {
            return@withContext Triple(Result.failure(e), "Cannot fetch schedule", 0)
        }
    }

    fun parseScheduleFromJson(jsonString: String): Result<List<List<ScheduleBlock>>> {
        return try {
            val json = JSONObject(jsonString)
            val data = json.getJSONObject("data")
            val dayName = data.optString("msg", "").ifBlank {
                data.optString("name", "").ifBlank {
                    data.optString("id", "Today's Schedule")
                }
            }
            if (dayName.isNotBlank()) {
                inMemoryDayName = dayName
            }
            val scheduleJson = data.getJSONArray("schedule")
            val schedule = ArrayList<List<ScheduleBlock>>(scheduleJson.length())

            for (i in 0 until scheduleJson.length()) {
                val daySchedule = scheduleJson.getJSONArray(i)
                val blocks = ArrayList<ScheduleBlock>(daySchedule.length())

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

    /**
     * Pure, zero-I/O evaluation of the current schedule state for smooth 1-second UI updates.
     */
    fun evaluateSchedule(
        schedule: List<List<ScheduleBlock>>,
        prefs: UserPreferencesSnapshot,
        now: LocalTime = LocalTime.now(),
        today: LocalDate = LocalDate.now(),
        afterSchoolQuip: String = defaultQuip()
    ): ScheduleEvaluation {
        val isWednesday = today.dayOfWeek == DayOfWeek.WEDNESDAY
        val selectedLunch = if (isWednesday) prefs.altLunchPreference else prefs.lunchPreference
        val lunchIndex = if (selectedLunch == "Lunch A") 0 else 1

        val rawBlocks = schedule.getOrElse(lunchIndex) { schedule.firstOrNull().orEmpty() }
        // Match croomsbellschedule.com schedule table filter: exclude 0, 100 (Morning), 101 (Welcome), 104 (Dismissal), 105 (After School), 106 (End)
        val displayBlocks = rawBlocks.filter { it.eventId !in setOf(0, 100, 101, 104, 105, 106) }

        val resolvedPeriods = displayBlocks.map { block ->
            val start = block.startTime
            val end = block.endTime
            val state = when {
                !now.isBefore(end) -> PeriodState.COMPLETED
                !now.isBefore(start) && now.isBefore(end) -> PeriodState.CURRENT
                else -> PeriodState.UPCOMING
            }
            val totalSeconds = ChronoUnit.SECONDS.between(start, end).coerceAtLeast(1L)
            val elapsedSeconds = ChronoUnit.SECONDS.between(start, now).coerceIn(0L, totalSeconds)
            val progress = when (state) {
                PeriodState.COMPLETED -> 1f
                PeriodState.CURRENT -> (elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
                PeriodState.UPCOMING -> 0f
            }
            val customLabel = resolveEventLabel(block.eventId, prefs)
            val defaultLabel = defaultEventLabel(block.eventId)
            ResolvedSchedulePeriod(
                block = block,
                label = customLabel,
                defaultLabel = defaultLabel,
                formattedTimeRange = block.formattedTimeRange(),
                durationMinutes = block.durationMinutes,
                state = state,
                progress = progress
            )
        }

        // Compute overall school day progress
        val dayProgress = if (displayBlocks.isNotEmpty()) {
            val dayStart = displayBlocks.first().startTime
            val dayEnd = displayBlocks.last().endTime
            val totalDaySeconds = ChronoUnit.SECONDS.between(dayStart, dayEnd).coerceAtLeast(1L)
            val elapsedDaySeconds = ChronoUnit.SECONDS.between(dayStart, now).coerceIn(0L, totalDaySeconds)
            (elapsedDaySeconds.toFloat() / totalDaySeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Find active block in rawBlocks
        for (i in rawBlocks.indices) {
            val block = rawBlocks[i]
            val start = block.startTime
            val end = block.endTime
            if (!now.isBefore(end)) continue

            val eventLabel = resolveEventLabel(block.eventId, prefs)
            if (block.eventId == 0 || block.eventId == 105 || block.eventId == 106) {
                return ScheduleEvaluation(
                    headlineTitle = if (block.eventId == 0) "No School Today" else "School's Out",
                    countdownClock = "Done",
                    subtitleText = afterSchoolQuip,
                    currentProgress = 1f,
                    dayProgress = 1f,
                    isSchoolDayActive = false,
                    isPassingPeriod = false,
                    selectedLunchLabel = selectedLunch,
                    isWednesday = isWednesday,
                    scheduleDayTitle = inMemoryDayName,
                    periods = resolvedPeriods
                )
            }

            if (block.eventId == 100) {
                // Morning block before Welcome/First Period: count down to the end of Morning (when school starts)
                val nextEventBlock = rawBlocks.firstOrNull { it.eventId !in setOf(0, 100, 101) } ?: block
                val targetStart = nextEventBlock.startTime
                val nextLabel = resolveEventLabel(nextEventBlock.eventId, prefs)
                val secondsUntilStart = ChronoUnit.SECONDS.between(now, targetStart).coerceAtLeast(0L)
                return ScheduleEvaluation(
                    headlineTitle = "Up Next: $nextLabel",
                    countdownClock = formatDurationClock(secondsUntilStart),
                    subtitleText = "School starts at ${targetStart.format(TIME_FORMATTER)}",
                    currentProgress = 0f,
                    dayProgress = 0f,
                    isSchoolDayActive = true,
                    isPassingPeriod = true,
                    selectedLunchLabel = selectedLunch,
                    isWednesday = isWednesday,
                    scheduleDayTitle = inMemoryDayName,
                    periods = resolvedPeriods
                )
            }

            if (now.isBefore(start)) {
                val secondsUntilStart = ChronoUnit.SECONDS.between(now, start).coerceAtLeast(0L)
                val prevEnd = if (i > 0) rawBlocks[i - 1].endTime else start.minusMinutes(6)
                val totalGap = ChronoUnit.SECONDS.between(prevEnd, start).coerceAtLeast(1L)
                val elapsedGap = (totalGap - secondsUntilStart).coerceIn(0L, totalGap)
                val gapProgress = (elapsedGap.toFloat() / totalGap.toFloat()).coerceIn(0f, 1f)

                return ScheduleEvaluation(
                    headlineTitle = "Up Next: $eventLabel",
                    countdownClock = formatDurationClock(secondsUntilStart),
                    subtitleText = "Starts at ${start.format(TIME_FORMATTER)}",
                    currentProgress = gapProgress,
                    dayProgress = dayProgress,
                    isSchoolDayActive = true,
                    isPassingPeriod = true,
                    selectedLunchLabel = selectedLunch,
                    isWednesday = isWednesday,
                    scheduleDayTitle = inMemoryDayName,
                    periods = resolvedPeriods
                )
            }

            val secondsUntilEnd = ChronoUnit.SECONDS.between(now, end).coerceAtLeast(0L)
            val totalDuration = ChronoUnit.SECONDS.between(start, end).coerceAtLeast(1L)
            val elapsed = (totalDuration - secondsUntilEnd).coerceIn(0L, totalDuration)
            val progress = (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

            return ScheduleEvaluation(
                headlineTitle = eventLabel,
                countdownClock = formatDurationClock(secondsUntilEnd),
                subtitleText = "Ends at ${end.format(TIME_FORMATTER)} • ${block.formattedTimeRange()}",
                currentProgress = progress,
                dayProgress = dayProgress,
                isSchoolDayActive = true,
                isPassingPeriod = false,
                selectedLunchLabel = selectedLunch,
                isWednesday = isWednesday,
                scheduleDayTitle = inMemoryDayName,
                periods = resolvedPeriods
            )
        }

        return ScheduleEvaluation(
            headlineTitle = "School's Out",
            countdownClock = "Done",
            subtitleText = afterSchoolQuip,
            currentProgress = 1f,
            dayProgress = 1f,
            isSchoolDayActive = false,
            isPassingPeriod = false,
            selectedLunchLabel = selectedLunch,
            isWednesday = isWednesday,
            scheduleDayTitle = inMemoryDayName,
            periods = resolvedPeriods
        )
    }

    private fun formatDurationClock(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%dh %02dm %02ds", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02dm %02ds", minutes, seconds)
        }
    }

    suspend fun fetchPeriodLabels(
        context: Context,
        schedule: List<List<ScheduleBlock>>
    ): List<Any> {
        val myDataStoreManager = MyDataStoreManager(context)
        val snapshot = myDataStoreManager.getSnapshot()
        val isWednesday = LocalDate.now().dayOfWeek == DayOfWeek.WEDNESDAY
        val lunch = if (!isWednesday) snapshot.lunchPreference else snapshot.altLunchPreference
        val lunchInt = if (lunch == "Lunch A") 0 else 1

        val todaySchedule = schedule.getOrElse(lunchInt) { schedule.firstOrNull().orEmpty() }
        val now = LocalTime.now()
        for (block in todaySchedule) {
            val eventEnd = block.endTime
            if (now.isAfter(eventEnd)) continue
            val processedEventId = resolveEventLabel(block.eventId, snapshot)
            return listOf(processedEventId, lunchInt)
        }
        return listOf("After School", lunchInt)
    }
}

private val AFTER_SCHOOL_QUIPS = listOf(
    "School's out. Brain going into standby mode. Please leave a message after the beep...",
    "Freedom bell! Time to activate 'do absolutely nothing' mode.",
    "Brain: Accepting applications for enjoyable distractions only.",
    "Time to skillfully transition from 'student' to 'professional doom-scroller'.",
    "Homework? Yeah, that's happening right after... just five more minutes of doomscrolling.",
    "Entering personal time. Productivity? Postponed due to urgent doomscrolling research.",
    "School day over. Doomscrolling systems online and fully operational.",
    "Achievement unlocked: Survived another school day. Reward: Unlocking next level of algorithm.",
    "Entering personal time. Productivity? Optional. Highly optional.",
    "Recharging brain. Process may involve copious amounts of… not homework.",
    "Why are you here? School is over. You have plenty of time to do... nothing."
)

@Volatile
private var cachedInMemoryQuip: String? = null

@Volatile
private var cachedInMemoryQuipTime: Instant? = null

fun defaultQuip(): String = cachedInMemoryQuip ?: AFTER_SCHOOL_QUIPS.first()

suspend fun afterSchoolQuips(context: Context): String {
    val cacheDurationHours = 1L
    val now = Instant.now()

    val memQuip = cachedInMemoryQuip
    val memTime = cachedInMemoryQuipTime
    if (!memQuip.isNullOrEmpty() && memTime != null && ChronoUnit.HOURS.between(memTime, now) < cacheDurationHours) {
        return memQuip
    }

    val myDataStoreManager = MyDataStoreManager(context)
    val cachedQuip = myDataStoreManager.getCachedOrRead(CACHED_QUIP_KEY).orEmpty()
    val cachedTimestamp = myDataStoreManager.getCachedOrRead(CACHE_QUIP_TIMESTAMP_KEY)

    if (cachedQuip.isNotEmpty() && cachedTimestamp != null) {
        val timestamp = runCatching { Instant.parse(cachedTimestamp) }.getOrNull()
        if (timestamp != null && ChronoUnit.HOURS.between(timestamp, now) < cacheDurationHours) {
            cachedInMemoryQuip = cachedQuip
            cachedInMemoryQuipTime = timestamp
            return cachedQuip
        }
    }

    val afterSchoolQuip = AFTER_SCHOOL_QUIPS.random()
    cachedInMemoryQuip = afterSchoolQuip
    cachedInMemoryQuipTime = now
    myDataStoreManager.saveData(CACHED_QUIP_KEY, afterSchoolQuip)
    myDataStoreManager.saveData(CACHE_QUIP_TIMESTAMP_KEY, now.toString())

    return afterSchoolQuip
}

suspend fun countdown(
    schedule: List<List<ScheduleBlock>>,
    processedEventId: String,
    lunchInt: Int,
    context: Context
): String {
    if (processedEventId == "After School" || processedEventId == "Nothing") {
        val quip = afterSchoolQuips(context)
        MyDataStoreManager(context).saveData(AFTER_SCHOOL_KEY, "true")
        return quip
    }

    val todaySchedule = schedule.getOrElse(lunchInt) { schedule.firstOrNull().orEmpty() }
    val now = LocalTime.now()

    for (block in todaySchedule) {
        val eventStart = block.startTime
        val eventEnd = block.endTime

        if (now.isAfter(eventEnd)) {
            continue
        }

        if (now.isBefore(eventStart)) {
            val timeUntilStart = ChronoUnit.SECONDS.between(now, eventStart).coerceAtLeast(0L)
            val minutes = timeUntilStart / 60
            val seconds = timeUntilStart % 60
            return "Time until start: ${"%02d".format(minutes)}:${"%02d".format(seconds)}"
        }

        val timeUntilEnd = ChronoUnit.SECONDS.between(now, eventEnd).coerceAtLeast(0L)
        return if (timeUntilEnd >= 3600) {
            val hours = timeUntilEnd / 3600
            val minutes = (timeUntilEnd / 60) - (hours * 60)
            val seconds = timeUntilEnd % 60
            "${"%1d".format(hours)}h ${"%02d".format(minutes)}m ${"%02d".format(seconds)}s \n Remaining in $processedEventId"
        } else {
            val minutes = timeUntilEnd / 60
            val seconds = timeUntilEnd % 60
            "${"%02d".format(minutes)}m ${"%02d".format(seconds)}s \n Remaining in $processedEventId"
        }
    }
    return "Calculation Timed Out"
}

fun formatedTimer(context: Context): Triple<String, String, String> = runBlocking {
    val (scheduleResult, connection, statusInt) = ScheduleFetcher.fetchAndParseSchedule(context, false)
    val status = statusInt.toString()

    if (scheduleResult.isSuccess) {
        val schedule = scheduleResult.getOrNull()
        if (schedule.isNullOrEmpty()) {
            Triple("No schedule available for today.", connection, status)
        } else {
            val returnedList = ScheduleFetcher.fetchPeriodLabels(context, schedule)
            val text = countdown(schedule, returnedList[0].toString(), returnedList[1] as Int, context)
            Triple(text, connection, status)
        }
    } else {
        Triple(
            "Failed to fetch the schedule: ${scheduleResult.exceptionOrNull()?.message}",
            connection,
            status
        )
    }
}
