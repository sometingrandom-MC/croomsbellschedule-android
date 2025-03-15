package com.example.croomsbellschedule

sealed class Screen (val route: String) {
    object Home: Screen(route = "Home")
    object Countdowns: Screen(route = "Countdowns")
    object Settings: Screen(route = "Settings")
}