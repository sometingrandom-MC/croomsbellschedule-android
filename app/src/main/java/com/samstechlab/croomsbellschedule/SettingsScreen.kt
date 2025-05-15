package com.samstechlab.croomsbellschedule

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val containerColor = if (scrollBehavior.state.collapsedFraction > 0.5f){
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.background}
            val fontSize = if (scrollBehavior.state.collapsedFraction > 0.5f){
                MaterialTheme.typography.titleLarge.fontSize
            } else {
                MaterialTheme.typography.headlineLarge.fontSize}
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Text(
                        "Settings",
                        fontSize = fontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },

                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = { // Added FloatingActionButton here
            FloatingActionButton(
                onClick = { println("Clicked") },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Check, "Save settings") // Using a Save icon
            }
        }
    ) { innerPadding ->
        ScrollContent(innerPadding)
    }

}


@OptIn(ExperimentalFoundationApi::class)
@Composable

fun ScrollContent(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Get the coroutine scope here!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.TopStart
    ) { Column(Modifier
        .fillMaxSize()
        .padding(20.dp)
        .verticalScroll(state = ScrollState(0), enabled = true, reverseScrolling = false)) {
        Text("Lunch Period Selection", modifier = Modifier.padding(10.dp),style = LocalTextStyle.current)

        val myDataStoreManager = MyDataStoreManager(context)
        var lunchIndex by remember { mutableIntStateOf(0) }
        var altlunchIndex by remember { mutableIntStateOf(0) }

        var p1 by remember { mutableStateOf("Period 1") }
        var p2 by remember { mutableStateOf("Period 2") }
        var p3 by remember { mutableStateOf("Period 3") }
        var p4 by remember { mutableStateOf("Period 4") }
        var p5 by remember { mutableStateOf("Period 5") }
        var p6 by remember { mutableStateOf("Period 6") }
        var p7 by remember { mutableStateOf("Period 7") }

        var lunchselectedIndexStr: String
        var altlunchselectedIndexStr: String

        runBlocking {
                val lunchPreference =
                    myDataStoreManager.getData(stringPreferencesKey("lunch_preference")).first()
                        ?: 0
                lunchselectedIndexStr = lunchPreference.toString()
                lunchIndex = if (lunchselectedIndexStr == "Lunch A")
                    0
                else
                    1
            }

        runBlocking {
            val altLunchPreference =
                myDataStoreManager.getData(stringPreferencesKey("alt_lunch_preference")).first()
                    ?: 0
            altlunchselectedIndexStr = altLunchPreference.toString()
            altlunchIndex = if (altlunchselectedIndexStr == "Lunch A")
                0
            else
                1
        }




        runBlocking {

                p1 =
                    myDataStoreManager.getData(stringPreferencesKey("p1")).first() ?: "Period 1"
                p2 =
                    myDataStoreManager.getData(stringPreferencesKey("p2")).first() ?: "Period 2"
                p3 =
                    myDataStoreManager.getData(stringPreferencesKey("p3")).first() ?: "Period 3"
                p4 =
                    myDataStoreManager.getData(stringPreferencesKey("p4")).first() ?: "Period 4"
                p5 =
                    myDataStoreManager.getData(stringPreferencesKey("p5")).first() ?: "Period 5"
                p6 =
                    myDataStoreManager.getData(stringPreferencesKey("p6")).first() ?: "Period 6"
                p7 =
                    myDataStoreManager.getData(stringPreferencesKey("p7")).first() ?: "Period 7"
            }



        val options = listOf("Lunch A", "Lunch B")
        AnimatedVisibility(visible = true, enter = fadeIn()) {

        }
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(label) },
                    onClick = { lunchIndex = index
                              coroutineScope.launch {myDataStoreManager.saveData(stringPreferencesKey("lunch_preference"), options[index])}},
                    selected = index == lunchIndex
                )

            }
        }

        Text("Wednesday Lunch Period Selection", modifier = Modifier.padding(10.dp),style = LocalTextStyle.current)

        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(label) },
                    onClick = { altlunchIndex = index
                        coroutineScope.launch {myDataStoreManager.saveData(stringPreferencesKey("alt_lunch_preference"), options[index])}},
                    selected = index == altlunchIndex
                )

            }
        }


        Text("Period Names", modifier = Modifier.padding(10.dp), style = LocalTextStyle.current)
        TextField(
            value = if (p1 != "Period 1") p1 else "",
            onValueChange = {p1 = it
                coroutineScope.launch { myDataStoreManager.saveData(stringPreferencesKey("p1"), p1) }},
            label = ({ Text("Period 1") })

        )
        TextField(
            value = if(p2 != "Period 2") p2 else "",
            onValueChange = {p2 = it
                coroutineScope.launch { myDataStoreManager.saveData(stringPreferencesKey("p2"), p2) }},
            label = ({ Text("Period 2") })
        )
        TextField(
            value = if(p3 != "Period 3") p3 else "",
            onValueChange = {p3 = it
                coroutineScope.launch { myDataStoreManager.saveData(stringPreferencesKey("p3"), p3) }},
            label = ({ Text("Period 3") })
        )
        TextField(
            value = if(p4 != "Period 4") p4 else "",
            onValueChange = {p4 = it
                coroutineScope.launch { myDataStoreManager.saveData(stringPreferencesKey("p4"), p4,) }},
            label = ({ Text("Period 4") })
        )
        TextField(
            value = if(p5 != "Period 5") p5 else "",
            onValueChange = {p5 = it
                coroutineScope.launch { myDataStoreManager.saveData(stringPreferencesKey("p5"), p5,) }},
            label = ({ Text("Period 5") })
        )
        TextField(
            value = if(p6 != "Period 6") p6 else "",
            onValueChange = {p6 = it
                coroutineScope.launch { myDataStoreManager.saveData(stringPreferencesKey("p6"), p6,) }},
            label = ({ Text("Period 6") })
        )
        TextField(
            value = if(p7 != "Period 7") p7 else "",
            onValueChange = {p7 = it
                coroutineScope.launch { myDataStoreManager.saveData(stringPreferencesKey("p7"), p7,) }},
            label = ({ Text("Period 7") })
        )


    }

    }
}


@Composable
@Preview(showBackground = true)
fun SettingsScreenPreview() {
    SettingsScreen()
}