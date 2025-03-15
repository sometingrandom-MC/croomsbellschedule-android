package com.samstechlab.croomsbellschedule

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val containerColor = if (scrollBehavior.state.collapsedFraction > 0.5f) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.background
            }
            val fontSize = if (scrollBehavior.state.collapsedFraction > 0.5f) {
                MaterialTheme.typography.titleLarge.fontSize
            } else {
                MaterialTheme.typography.headlineLarge.fontSize
            }
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Text(
                        "Crooms Bell Schedule",
                        fontSize = fontSize,
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

@Composable
fun ScrollContentHome(innerPadding: PaddingValues) {
    val context = LocalContext.current
    var timerText by remember { mutableStateOf("loading...") }
    var connectionStatus by remember { mutableStateOf("Disconnected") }
    var status by remember { mutableIntStateOf(1) }
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
    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        ElevatedCard(elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), modifier = Modifier
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
    val colors = if (status == 0) {
        AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    } else {
        AssistChipDefaults.assistChipColors()
    }
    Box(Modifier.fillMaxSize().padding(bottom = 115.dp), contentAlignment = Alignment.BottomCenter){
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
