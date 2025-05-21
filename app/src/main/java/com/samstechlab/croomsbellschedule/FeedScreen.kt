package com.samstechlab.croomsbellschedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.graphics.lerp as colorLerp // Alias for color lerp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.samstechlab.croomsbellschedule.CroomsshedFeed

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeedScreen() {

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val collapsedFraction = scrollBehavior.state.collapsedFraction

            // Smoothly interpolate color
            val interpolatedContainerColor = colorLerp(
                start = MaterialTheme.colorScheme.background,
                stop = MaterialTheme.colorScheme.surfaceContainer,
                fraction = collapsedFraction
            )

            // Smoothly interpolate font size
            val interpolatedFontSize = lerp(
                start = MaterialTheme.typography.headlineLarge.fontSize,
                stop = MaterialTheme.typography.titleLarge.fontSize,
                fraction = collapsedFraction
            )
            MediumFlexibleTopAppBar(

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = interpolatedContainerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {

                    Text(
                        "Feed",
                        fontSize = interpolatedFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },

                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        ScrollContentCountdowns(innerPadding)
    }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScrollContentCountdowns(innerPadding: PaddingValues) {
    // remember the instance of CroomsshedFeed
    val feedManager = remember { CroomsshedFeed() }
    val context = LocalContext.current

    // State to hold the list of messages
    var messagesList by remember { mutableStateOf<List<String>?>(null) }
    // State to hold any error message from fetching/parsing
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // State to hold the status message (e.g., "Fetched from cache X minutes ago")
    var fetchStatusMessage by remember { mutableStateOf<String?>(null) }
    // State to track loading status
    var isLoading by remember { mutableStateOf(true) }

    // Use LaunchedEffect to fetch data when the composable enters the composition
    // It will re-launch if feedManager or context change, though they are stable here.
    LaunchedEffect(key1 = Unit) { // key1 = Unit ensures it runs once on composition
        isLoading = true
        errorMessage = null
        messagesList = null
        fetchStatusMessage = null
        println("LaunchedEffect: Starting to fetch feed messages...")

        try {
            // fetchAndGetFeedMessages is a suspend function and handles Dispatchers.IO internally
            val (result, statusMsg, statusCode) = feedManager.fetchAndGetFeedMessages(context, refresh = false) // Set refresh to true to bypass cache

            fetchStatusMessage = statusMsg // Store the status message

            result.fold(
                onSuccess = { fetchedMessages ->
                    println("LaunchedEffect: Successfully fetched messages. Count: ${fetchedMessages.size}")
                    messagesList = fetchedMessages
                    if (fetchedMessages.isEmpty()) {
                        errorMessage = "No messages found in the feed."
                    }
                },
                onFailure = { exception ->
                    println("LaunchedEffect: Error fetching messages: ${exception.message}")
                    errorMessage = "Error: ${exception.message ?: "Unknown error"}"
                }
            )
        } catch (e: Exception) {
            // This catch block is for unexpected errors during the LaunchedEffect itself,
            // though fetchAndGetFeedMessages should ideally encapsulate its errors in the Result.
            println("LaunchedEffect: Uncaught exception: ${e.message}")
            errorMessage = "An unexpected error occurred: ${e.message}"
        } finally {
            isLoading = false
            println("LaunchedEffect: Fetching complete. isLoading: $isLoading")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding), // Apply innerPadding to the Box
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            LoadingIndicator()
        } else {
            // Use a local variable for smart casting and null check
            val currentMessages = messagesList
            if (currentMessages != null && currentMessages.isNotEmpty()) {
                // Display the fetched messages in a LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize() // LazyColumn should fill the Box
                        .padding(horizontal = 8.dp) // Add some padding for the list items
                ) {
                    // You can also display the fetchStatusMessage if desired
                    // item { Text(text = fetchStatusMessage ?: "Checking feed...") }

                    items(count = currentMessages.size) { message -> // Use the smart-casted non-null list
                        ElevatedCard(Modifier.padding(vertical = 4.dp)){
                            Text(
                                text = currentMessages[message],

                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 10.dp)
                            )
                        }

                    }
                }
            } else {
                // Display an error message or "No data available"
                Text(text = errorMessage ?: "No messages available or failed to load.")
            }
        }
        // Optionally, display the fetchStatusMessage somewhere, e.g., at the bottom
        // if (fetchStatusMessage != null && !isLoading) {
        //     Text(
        //         text = fetchStatusMessage!!,
        //         modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        //     )
        // }
    }
}
