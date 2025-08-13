package com.samstechlab.croomsbellschedule

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButton
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.graphics.lerp as colorLerp // Alias for color lerp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { Modifier.padding(0.dp)
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
            LargeFlexibleTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = interpolatedContainerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Text(
                       "Settings",
                        fontSize = interpolatedFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },

                scrollBehavior = scrollBehavior
            )
        },

    ) { innerPadding ->
        ScrollContent(innerPadding)
    }

}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
        Text(
            "Lunch Period Selection",
            modifier = Modifier.padding(10.dp),
            style = LocalTextStyle.current
        )

        val myDataStoreManager = MyDataStoreManager(context)
        var lunchIndex by remember { mutableIntStateOf(0) }
        var altlunchIndex by remember { mutableIntStateOf(0) }

        val p = Array(8) { "" }



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
            for (i in 1..7) {
                p[i] =
                    myDataStoreManager.getData(stringPreferencesKey("p$i")).first() ?: "Period $i"
            }
        }
        var p1l by remember { mutableStateOf((if (p[1]!= "Period 1") p[1] else "")) }
        var p2l by remember { mutableStateOf((if (p[2] != "Period 2") p[2] else "")) }
        var p3l by remember { mutableStateOf((if (p[3] != "Period 3") p[3] else "")) }
        var p4l by remember { mutableStateOf((if (p[4] != "Period 4") p[4] else "")) }
        var p5l by remember { mutableStateOf((if (p[5] != "Period 5") p[5] else "")) }
        var p6l by remember { mutableStateOf((if (p[6] != "Period 6") p[6] else "")) }
        var p7l by remember { mutableStateOf((if (p[7] != "Period 7") p[7] else "")) }

        var p1e by remember { mutableStateOf(true) }
        var p2e by remember { mutableStateOf(true) }
        var p3e by remember { mutableStateOf(true) }
        var p4e by remember { mutableStateOf(true) }
        var p5e by remember { mutableStateOf(true) }
        var p6e by remember { mutableStateOf(true) }
        var p7e by remember { mutableStateOf(true) }


        val options = listOf("Lunch A", "Lunch B")
        AnimatedVisibility(visible = true, enter = fadeIn()) {

        }
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(label) },
                    onClick = {
                        lunchIndex = index
                        coroutineScope.launch {
                            myDataStoreManager.saveData(
                                stringPreferencesKey("lunch_preference"),
                                options[index]
                            )
                        }
                    },
                    selected = index == lunchIndex
                )

            }
        }

        Text(
            "Wednesday Lunch Period Selection",
            modifier = Modifier.padding(10.dp),
            style = LocalTextStyle.current
        )

        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(label) },
                    onClick = {
                        altlunchIndex = index
                        coroutineScope.launch {
                            myDataStoreManager.saveData(
                                stringPreferencesKey("alt_lunch_preference"),
                                options[index]
                            )
                        }
                    },
                    selected = index == altlunchIndex
                )

            }
        }


        Text("Period Names", modifier = Modifier.padding(10.dp), style = LocalTextStyle.current)
        Row {
            TextField(
                value = (p1l),
                onValueChange = {
                    p1l = it
                    coroutineScope.launch {
                        myDataStoreManager.saveData(
                            stringPreferencesKey("p1"),
                            p1l
                        )
                    }
                },
                label = ({ Text("Period 1") })

            )
            Spacer(
                Modifier.width(8.dp)
            )
            /*Switch(
                checked = p1e,
                onCheckedChange = { p1e = it }
            )*/
        }

        Row {
            TextField(
                value = (p2l),
                onValueChange = {
                    p2l = it
                    coroutineScope.launch {
                        myDataStoreManager.saveData(
                            stringPreferencesKey("p2"),
                            p2l
                        )
                    }
                },
                label = ({ Text("Period 2") })

            )
            Spacer(
                Modifier.width(8.dp)
            )
            /*Switch(
            checked = p1e,
            onCheckedChange = { p1e = it }
                        )*/
        }
        Row {
            TextField(
                value = (p3l),
                onValueChange = {
                    p3l = it
                    coroutineScope.launch {
                        myDataStoreManager.saveData(
                            stringPreferencesKey("p3"),
                            p3l
                        )
                    }
                },
                label = ({ Text("Period 3") })

            )
            Spacer(
                Modifier.width(8.dp)
            )
            /*Switch(
                checked = p3e,
                onCheckedChange = { p3e = it }
            )*/
        }

        Row {
            TextField(
                value = (p4l),
                onValueChange = {
                    p4l = it
                    coroutineScope.launch {
                        myDataStoreManager.saveData(
                            stringPreferencesKey("p4"),
                            p4l
                        )
                    }
                },
                label = ({ Text("Period 4") })

            )
            Spacer(
                Modifier.width(8.dp)
            )
            /*Switch(
                checked = p4e,
                onCheckedChange = { p4e = it }
            )*/
        }

        Row {
            TextField(
                value = (p5l),
                onValueChange = {
                    p5l = it
                    coroutineScope.launch {
                        myDataStoreManager.saveData(
                            stringPreferencesKey("p5"),
                            p5l
                        )
                    }
                },
                label = ({ Text("Period 5") })

            )
            Spacer(
                Modifier.width(8.dp)
            )
            /*Switch(
                checked = p5e,
                onCheckedChange = { p5e = it }
            )*/
        }

        Row {
            TextField(
                value = (p6l),
                onValueChange = {
                    p6l = it
                    coroutineScope.launch {
                        myDataStoreManager.saveData(
                            stringPreferencesKey("p6"),
                            p6l
                        )
                    }
                },
                label = ({ Text("Period 6") })

            )
            Spacer(
                Modifier.width(8.dp)
            )
            /*Switch(
                checked = p6e,
                onCheckedChange = { p6e = it }
            )*/
        }

        Row {
            TextField(
                value = (p7l),
                onValueChange = {
                    p7l = it
                    coroutineScope.launch {
                        myDataStoreManager.saveData(
                            stringPreferencesKey("p7"),
                            p7l
                        )
                    }
                },
                label = ({ Text("Period 7") })

            )
            Spacer(
                Modifier.width(8.dp)
            )
            /*Switch(
                checked = p7e,
                onCheckedChange = { p7e = it }
            )*/
        }
    }

    }
}


@Composable
@Preview(showBackground = true)
fun SettingsScreenPreview() {
    SettingsScreen()
}