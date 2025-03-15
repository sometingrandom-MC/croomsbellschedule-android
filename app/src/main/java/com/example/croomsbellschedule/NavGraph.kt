package com.example.croomsbellschedule
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
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
            CountdownsScreen()
        }
    }
}