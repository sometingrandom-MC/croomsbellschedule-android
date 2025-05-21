package com.samstechlab.croomsbellschedule

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.graphics.lerp as colorLerp


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IndeterminateExpressiveReloadingLoop(
    modifier: Modifier = Modifier,
    animationSpec: androidx.compose.animation.core.InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = tween(durationMillis = 1500, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
) {
    val progress = rememberInfiniteTransition(label = "indeterminateProgress").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = animationSpec,
        label = "progressFloat"
    )

    LoadingIndicator(
        progress = { progress.value },
        modifier = modifier
        // You might want a simpler set of polygons for an indeterminate loop,
        // or ensure your polygon list loops well visually.
        // For example, if LoadingIndicatorDefaults.IndeterminateIndicatorPolygons exists:
        // polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
        // Or define your own:
        // polygons = listOf(LoadingIndicatorDefaults.Triangle, LoadingIndicatorDefaults.Circle) // Example
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen() {


    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(

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
                        "Crooms Bell Schedule",
                        fontSize = interpolatedFontSize,
                        fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
                        fontWeight = MaterialTheme.typography.headlineLarge.fontWeight,
                        fontStyle = MaterialTheme.typography.headlineLarge.fontStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },

                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        ScrollContentHome(innerPadding)
    }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScrollContentHome(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val state = rememberPullToRefreshState()
    val hapticFeedback = LocalHapticFeedback.current


    val scope = rememberCoroutineScope()
    var timerText by remember { mutableStateOf("loading...") }
    var connectionStatus by remember { mutableStateOf("Disconnected") }
    var status by remember { mutableIntStateOf(1) }
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = true)  { CoroutineScope(Dispatchers.IO).launch {
        val formatedTimer = formatedTimer(context)
        timerText = formatedTimer.first
        connectionStatus = formatedTimer(context).second
        status = formatedTimer(context).third.toInt()
    }}
    LaunchedEffect(key1 = true) { CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            delay(1000L)
            val formatedTimer = formatedTimer(context)
            timerText = formatedTimer.first
            connectionStatus = formatedTimer(context).second
    }

        }
    }
    PullToRefreshBox(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                try {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    delay(70)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    delay(100)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    refreshCache(context)

                    delay(70)  // Ensure refreshCache is suspend or uses withContext for blocking calls
                    println("It works")
                    delay(1000)
                } finally {
                    isRefreshing = false
                }
            }
        },
    ) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
            ElevatedCard(
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 0.dp)
                .fillMaxWidth(),
        ) {
            Text(
                "Timer",
                modifier = Modifier.padding(top = 16.dp, bottom = 5.dp, start = 16.dp, end = 0.dp),
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp, bottom = 14.dp),
                contentAlignment = Alignment.Center,
            ) {

                Text(
                    timerText,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    textAlign = TextAlign.Center,
                    lineHeight = 1.5.em
                )
            }

}
    }
    }
    val colors = if (status == 0) {
        AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    } else {
        AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    }
    Box(Modifier
        .fillMaxSize()
        .padding(bottom = 5.dp)
        .navigationBarsPadding(), contentAlignment = Alignment.BottomCenter){
        AssistChip(
            label = { Text(connectionStatus)},
            onClick = { refreshCache(context)}, leadingIcon = {Icon(Icons.Filled.Refresh,
                contentDescription = "Refresh")},
            colors = colors,
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(0.dp,
                color = MaterialTheme.colorScheme.surfaceContainer))
    }
}


fun refreshCache(context: Context) {
    runBlocking { ScheduleFetcher.fetchAndParseSchedule(context = context, true) }
}

@Composable
@Preview(showBackground = true)
fun HomeScreenPreview() {
    HomeScreen()
}
