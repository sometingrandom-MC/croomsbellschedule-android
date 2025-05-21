package com.samstechlab.croomsbellschedule

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit

// DataStore keys
private val CACHED_FEED_KEY = stringPreferencesKey("cached_feed_messages") // Changed key name for clarity
private val CACHE_TIMESTAMP_KEY = stringPreferencesKey("cache_timestamp_feed_messages") // Changed key name for clarity

private const val FEED_REFRESH_DURATION_MINUTES = 60L // Cache duration, e.g., 60 minutes

@kotlinx.serialization.Serializable
data class ApiResponse(
    val status: String,
    val data: List<FeedItem>
)

@kotlinx.serialization.Serializable
data class FeedItem(
    val data: String, // This is the message content
    val store: String,
    val id: String,
    val create: String,
    val delete: String,
    val createdBy: String? = null,
    val uid: String? = null,
    val verified: Boolean
)

/**
 * Fetches the raw API response string for the feed.
 */
suspend fun getFeedApiResponseString(): String? {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.croomssched.tech/feed") // Centralized URL
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                println("API request failed with code: ${response.code}")
                null
            }
        } catch (e: IOException) {
            println("Error during API request: ${e.message}")
            null
        }
    }
}

/**
 * Configured Json instance for Kotlinx Serialization.
 */
private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Parses the JSON string containing feed items into a list of message strings.
 * Uses Kotlinx Serialization.
 *
 * @param jsonString The JSON string to parse.
 * @return A Result containing a list of messages (String) or an error.
 */
fun parseMessagesFromFeedJson(jsonString: String): Result<List<String>> {
    return try {
        val apiResponse = jsonParser.decodeFromString<ApiResponse>(jsonString)
        if (apiResponse.status == "OK") {
            val messages = apiResponse.data.map { it.data } // Extract the 'data' field (message content)
            Result.success(messages)
        } else {
            Result.failure(Exception("API status was not OK: ${apiResponse.status}"))
        }
    } catch (e: Exception) { // Catching general Exception for serialization and other issues
        println("Error parsing feed messages JSON: ${e.message}")
        Result.failure(e)
    }
}


class CroomsshedFeed {

    /**
     * Fetches the feed and parses it into a list of message strings.
     *
     * @param context The application context for accessing DataStore.
     * @param refresh Whether to force a refresh, ignoring the cache.
     * @return A Triple containing:
     * 1. Result<List<String>>: The list of messages or an error.
     * 2. String: A status message about the data fetch.
     * 3. Int: A status code (e.g., 1 for success, 0 for error/fallback).
     */
    public suspend fun fetchAndGetFeedMessages(context: Context, refresh: Boolean): Triple<Result<List<String>>, String, Int> =
        withContext(Dispatchers.IO) {
            try {
                val myDataStoreManager = MyDataStoreManager(context)
                val connectionStatusMessage: String
                val statusCode: Int // 0 for error/fallback, 1 for success

                // Try to get cached raw JSON data
                val cachedRawJson = myDataStoreManager.getData(CACHED_FEED_KEY).first()
                val cachedTimestampString = myDataStoreManager.getData(CACHE_TIMESTAMP_KEY).first()
                var minutesSinceCache: Long = Long.MAX_VALUE

                if (cachedTimestampString != null) {
                    try {
                        val timestamp = Instant.parse(cachedTimestampString)
                        minutesSinceCache = ChronoUnit.MINUTES.between(timestamp, Instant.now())
                    } catch (e: Exception) {
                        println("Error parsing cached timestamp for messages: ${e.message}")
                        // Invalidate cache by keeping minutesSinceCache high
                    }
                }

                // Check if cache is valid and refresh is not forced
                if (cachedRawJson != null && minutesSinceCache < FEED_REFRESH_DURATION_MINUTES && !refresh) {
                    statusCode = 1
                    connectionStatusMessage = "Fetched messages $minutesSinceCache minutes ago (cached)"
                    // Parse the cached JSON for messages
                    return@withContext Triple(parseMessagesFromFeedJson(cachedRawJson), connectionStatusMessage, statusCode)
                }

                // If cache is invalid, missing, or refresh is forced, fetch new data
                val newRawJson = getFeedApiResponseString() // Fetches the raw JSON string
                if (newRawJson != null) {
                    // Save new raw JSON data to cache
                    myDataStoreManager.saveData(CACHED_FEED_KEY, newRawJson)
                    myDataStoreManager.saveData(CACHE_TIMESTAMP_KEY, Instant.now().toString())
                    statusCode = 1
                    connectionStatusMessage = "Fetched fresh messages just now"
                    // Parse the new JSON for messages
                    return@withContext Triple(parseMessagesFromFeedJson(newRawJson), connectionStatusMessage, statusCode)
                } else {
                    // API call failed, try to use stale cache as fallback if available
                    if (cachedRawJson != null) {
                        statusCode = 0 // Indicate fallback/error
                        connectionStatusMessage = "Failed to update messages, using data from $minutesSinceCache minutes ago (stale cache)"
                        return@withContext Triple(parseMessagesFromFeedJson(cachedRawJson), connectionStatusMessage, statusCode)
                    }
                    // No new data and no cache at all
                    throw IOException("Failed to fetch messages and no cache available")
                }
            } catch (e: Exception) {
                println("Exception in fetchAndGetFeedMessages: ${e.message}")
                return@withContext Triple(Result.failure(e), "Cannot fetch message data", 0)
            }
        }
}


