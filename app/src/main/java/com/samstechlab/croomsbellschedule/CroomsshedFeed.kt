package com.samstechlab.croomsbellschedule

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.core.text.HtmlCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val CACHED_FEED_KEY = stringPreferencesKey("cached_feed_messages_v2")
private val CACHE_TIMESTAMP_KEY = stringPreferencesKey("cache_timestamp_feed_messages_v2")

private const val FEED_REFRESH_DURATION_MINUTES = 60L

@Serializable
data class ApiResponse(
    val status: String,
    val data: List<FeedItem> = emptyList()
)

@Immutable
@Serializable
data class FeedItem(
    val data: String,
    val title: String = "",
    val store: String = "",
    val id: String = "",
    val create: String = "",
    val delete: String = "",
    val createdBy: String? = null,
    val uid: String? = null,
    val verified: Boolean = false,
    val priority: Boolean = false,
    val targets: List<String> = emptyList()
) {
    val authorDisplayName: String
        get() = when {
            !createdBy.isNullOrBlank() -> createdBy
            title.isNotBlank() -> title
            else -> "Crooms Announcement"
        }

    fun formattedTimestamp(): String {
        if (create.isBlank()) return ""
        return try {
            val instant = runCatching { Instant.parse(create) }.getOrNull()
                ?: create.toLongOrNull()?.let { epoch ->
                    if (epoch > 10_000_000_000L) Instant.ofEpochMilli(epoch) else Instant.ofEpochSecond(epoch)
                }
            if (instant != null) {
                val now = Instant.now()
                val minutes = ChronoUnit.MINUTES.between(instant, now)
                val hours = ChronoUnit.HOURS.between(instant, now)
                val days = ChronoUnit.DAYS.between(instant, now)
                when {
                    minutes < 1 -> "Just now"
                    minutes < 60 -> "${minutes}m ago"
                    hours < 24 -> "${hours}h ago"
                    days < 7 -> "${days}d ago"
                    else -> FEED_DATE_FORMATTER.format(instant.atZone(ZoneId.systemDefault()))
                }
            } else {
                create
            }
        } catch (_: Exception) {
            create
        }
    }

    companion object {
        private val FEED_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    }
}

/**
 * Fetches announcements from the updated Crooms Bell Schedule API.
 */
suspend fun getFeedApiResponseString(): String? = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url("https://api.croomsbellschedule.com/announcements/website")
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

/**
 * Converts HTML announcement content (`<div>`, `<ul>`, `<li>`, `<p>`, `<br>`) into clean, readable text.
 */
fun cleanAnnouncementHtml(rawHtml: String): String {
    if (rawHtml.isBlank()) return ""
    // Pre-process list items and block tags into clean line breaks and bullet points
    val structured = rawHtml
        .replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "\n• ")
        .replace(Regex("</li>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</(div|p|ul|ol|h[1-6])>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&#x27;", "'")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    val plain = runCatching {
        HtmlCompat.fromHtml(structured, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
    }.getOrDefault(structured)

    return plain
        .lines()
        .map { it.trim() }
        .fold(mutableListOf<String>()) { acc, line ->
            if (line.isNotEmpty() || (acc.isNotEmpty() && acc.last().isNotEmpty())) {
                acc.add(line)
            }
            acc
        }
        .joinToString("\n")
        .trim()
}

/**
 * Parses both the new `/announcements/website` JSON format and the legacy `/feed` JSON format.
 */
fun parseFeedItemsFromJson(jsonString: String): Result<List<FeedItem>> {
    return try {
        val root = JSONObject(jsonString)
        val status = root.optString("status", "")
        if (!status.equals("OK", ignoreCase = true)) {
            return Result.failure(Exception("API status was not OK: $status"))
        }

        val dataArray = root.getJSONArray("data")
        val items = ArrayList<FeedItem>(dataArray.length())

        for (i in 0 until dataArray.length()) {
            val itemObj = dataArray.getJSONObject(i)
            val id = itemObj.optString("id", "item_$i")
            val dataField = itemObj.opt("data")

            if (dataField is JSONObject) {
                // New Crooms Bell Schedule API format (/announcements/website)
                val title = dataField.optString("title", "Announcement")
                val rawMessage = dataField.optString("message", "")
                val cleanedMessage = cleanAnnouncementHtml(rawMessage)
                val priority = itemObj.optBoolean("priority", false)
                val created = itemObj.optString("created", "")
                val expires = itemObj.optString("expires", "")
                val targetsJson = itemObj.optJSONArray("targets")
                val targets = mutableListOf<String>()
                if (targetsJson != null) {
                    for (t in 0 until targetsJson.length()) {
                        targets.add(targetsJson.optString(t))
                    }
                }

                items.add(
                    FeedItem(
                        id = id,
                        title = title,
                        data = cleanedMessage,
                        create = created,
                        delete = expires,
                        createdBy = "Crooms Bell Schedule Team",
                        verified = true,
                        priority = priority,
                        targets = targets
                    )
                )
            } else {
                // Legacy /feed format where "data" is a plain string
                val message = itemObj.optString("data", "")
                val store = itemObj.optString("store", "")
                val create = itemObj.optString("create", "")
                val delete = itemObj.optString("delete", "")
                val createdBy = if (itemObj.has("createdBy") && !itemObj.isNull("createdBy")) {
                    itemObj.optString("createdBy")
                } else {
                    null
                }
                val uid = if (itemObj.has("uid") && !itemObj.isNull("uid")) {
                    itemObj.optString("uid")
                } else {
                    null
                }
                val verified = itemObj.optBoolean("verified", false)

                items.add(
                    FeedItem(
                        id = id,
                        title = createdBy ?: "Crooms Announcement",
                        data = message,
                        store = store,
                        create = create,
                        delete = delete,
                        createdBy = createdBy,
                        uid = uid,
                        verified = verified,
                        priority = false
                    )
                )
            }
        }

        Result.success(items)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

fun parseMessagesFromFeedJson(jsonString: String): Result<List<String>> {
    return parseFeedItemsFromJson(jsonString).map { list ->
        list.map { item ->
            if (item.title.isNotBlank() && item.title != item.createdBy) {
                "${item.title}\n${item.data}"
            } else {
                item.data
            }
        }
    }
}

class CroomsshedFeed {

    suspend fun fetchAndGetFeedItems(
        context: Context,
        refresh: Boolean = false
    ): Triple<Result<List<FeedItem>>, String, Int> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now()
            val memItems = cachedItems
            val memTime = cachedTimestamp

            if (!refresh && memItems != null && memTime != null) {
                val mins = ChronoUnit.MINUTES.between(memTime, now).coerceAtLeast(0L)
                if (mins < FEED_REFRESH_DURATION_MINUTES) {
                    val statusMsg = if (mins == 0L) "Updated just now" else "Updated ${mins}m ago"
                    return@withContext Triple(Result.success(memItems), statusMsg, 1)
                }
            }

            val myDataStoreManager = MyDataStoreManager(context)
            val cachedRawJson = myDataStoreManager.getCachedOrRead(CACHED_FEED_KEY)
            val cachedTimestampString = myDataStoreManager.getCachedOrRead(CACHE_TIMESTAMP_KEY)
            var minutesSinceCache: Long = Long.MAX_VALUE

            if (cachedTimestampString != null) {
                runCatching {
                    val timestamp = Instant.parse(cachedTimestampString)
                    minutesSinceCache = ChronoUnit.MINUTES.between(timestamp, now).coerceAtLeast(0L)
                }
            }

            if (cachedRawJson != null && minutesSinceCache < FEED_REFRESH_DURATION_MINUTES && !refresh) {
                val parsed = parseFeedItemsFromJson(cachedRawJson)
                if (parsed.isSuccess) {
                    cachedItems = parsed.getOrNull()
                    cachedTimestamp = runCatching { Instant.parse(cachedTimestampString) }.getOrNull() ?: now
                }
                val msg = if (minutesSinceCache == 0L) "Updated just now" else "Updated ${minutesSinceCache}m ago"
                return@withContext Triple(parsed, msg, 1)
            }

            val newRawJson = getFeedApiResponseString()
            if (newRawJson != null) {
                val parsed = parseFeedItemsFromJson(newRawJson)
                if (parsed.isSuccess) {
                    val currentInstant = Instant.now()
                    cachedItems = parsed.getOrNull()
                    cachedTimestamp = currentInstant
                    myDataStoreManager.saveData(CACHED_FEED_KEY, newRawJson)
                    myDataStoreManager.saveData(CACHE_TIMESTAMP_KEY, currentInstant.toString())
                    return@withContext Triple(parsed, "Updated just now", 1)
                }
            }

            if (cachedRawJson != null) {
                val parsed = parseFeedItemsFromJson(cachedRawJson)
                if (parsed.isSuccess) {
                    cachedItems = parsed.getOrNull()
                }
                val connectionStatusMessage = if (minutesSinceCache != Long.MAX_VALUE) {
                    "Offline • Cached ${minutesSinceCache}m ago"
                } else {
                    "Offline • Using cached announcements"
                }
                return@withContext Triple(parsed, connectionStatusMessage, 0)
            }

            throw IOException("Unable to reach server and no cached announcements found.")
        } catch (e: Exception) {
            return@withContext Triple(Result.failure(e), "Offline • Cannot fetch announcements", 0)
        }
    }

    suspend fun fetchAndGetFeedMessages(
        context: Context,
        refresh: Boolean
    ): Triple<Result<List<String>>, String, Int> {
        val (result, msg, code) = fetchAndGetFeedItems(context, refresh)
        return Triple(result.map { items -> items.map { it.data } }, msg, code)
    }

    companion object {
        @Volatile
        private var cachedItems: List<FeedItem>? = null

        @Volatile
        private var cachedTimestamp: Instant? = null
    }
}
