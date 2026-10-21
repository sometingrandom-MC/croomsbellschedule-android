package com.samstechlab.croomsbellschedule

sealed class Screen(val route: String) {
    data object Home : Screen(route = "Home")
    data object Countdowns : Screen(route = "Feed")
    data object Settings : Screen(route = "Settings")
}
