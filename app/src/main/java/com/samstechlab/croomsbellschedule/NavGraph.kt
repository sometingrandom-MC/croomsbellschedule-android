package com.samstechlab.croomsbellschedule
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        modifier = Modifier.padding(bottom = 75.dp)
    ) {
        composable(
            route = Screen.Home.route
        ) {
            HomeScreen()
        }
        composable(
            route = Screen.Settings.route
        ) {
            SettingsScreen()
        }
        composable(
            route = Screen.Countdowns.route
        ) {
            FeedScreen()
        }
    }
}

@Preview
@Composable
fun SetupNavGraphPreview() {
    SetupNavGraph(navController = NavHostController(LocalContext.current), innerPadding = PaddingValues(0.dp))
}