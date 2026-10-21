package com.samstechlab.croomsbellschedule

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    mainViewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetX = { it / 6 }
                )
        },
        exitTransition = {
            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                slideOutHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    targetOffsetX = { -it / 6 }
                )
        },
        popEnterTransition = {
            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetX = { -it / 6 }
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                slideOutHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    targetOffsetX = { it / 6 }
                )
        },
        modifier = modifier
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                viewModel = mainViewModel,
                bottomNavPadding = innerPadding
            )
        }
        composable(route = Screen.Countdowns.route) {
            FeedScreen(
                viewModel = mainViewModel,
                bottomNavPadding = innerPadding
            )
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                viewModel = mainViewModel,
                bottomNavPadding = innerPadding
            )
        }
    }
}

@Preview
@Composable
fun SetupNavGraphPreview() {
    SetupNavGraph(
        navController = NavHostController(LocalContext.current),
        innerPadding = PaddingValues(0.dp)
    )
}
