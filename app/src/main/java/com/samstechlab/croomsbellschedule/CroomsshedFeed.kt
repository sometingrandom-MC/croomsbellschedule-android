package com.samstechlab.croomsbellschedule

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.samstechlab.croomsbellschedule.ScheduleFetcher.parseScheduleFromJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.serialization.Serializable


private val CACHED_FEED_KEY = stringPreferencesKey("cached_feed")
private val CACHE_TIMESTAMP_KEY = stringPreferencesKey("cache_timestamp_feed")
private const val FEED_REFRESH_DURATION = 1L



@Serializable
data class ApiResponse(
    val status: String,
    val data: List<FeedItem>
)

@Serializable
data class FeedItem(
    val data: String,
    val store: String,
    val id: String,
    val create: String,
    val delete: String,
    val createdBy: String,
    val uid: String,
    val verified: Boolean
)

suspend fun getFeedApiResponse(): String? {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.croomssched.tech/feed")
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

class CroomsshedFeed {



    suspend fun fetchAndParseFeed(context: Context, refresh: Boolean = false): Triple<Result<List<List<ScheduleBlock>>>, String, Int> =

        withContext(Dispatchers.IO) {
            try {
                val myDataStoreManager = MyDataStoreManager(context)
                val connection: String
                val status: Int
                // Try to get cached data
                val cachedSchedule = myDataStoreManager.getData(CACHED_FEED_KEY).first()
                val cachedTimestamp = myDataStoreManager.getData(CACHE_TIMESTAMP_KEY).first()
                var minutesSinceCache: Long

                // Check if cache is valid
                if (cachedSchedule != null && cachedTimestamp != null) {
                    val timestamp = Instant.parse(cachedTimestamp)
                    val now = Instant.now()
                    minutesSinceCache = ChronoUnit.MINUTES.between(timestamp, now)


                    if (minutesSinceCache < FEED_REFRESH_DURATION && !refresh) {
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
                    myDataStoreManager.saveData(CACHED_FEED_KEY, response)
                    myDataStoreManager.saveData(CACHE_TIMESTAMP_KEY, Instant.now().toString())
                    minutesSinceCache = ChronoUnit.MINUTES.between(
                        Instant.parse(Instant.now().toString()), Instant.parse(
                            Instant.now().toString()))
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
}