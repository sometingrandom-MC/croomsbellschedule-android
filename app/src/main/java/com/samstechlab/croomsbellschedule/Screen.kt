package com.samstechlab.croomsbellschedule

sealed class Screen (val route: String) {
    object Home: Screen(route = "Home")
    object Countdowns: Screen(route = "Feed")
    object Settings: Screen(route = "Settings")
}