package com.example.croomsbellschedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainViewModel(private val context: Context): ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()
    init {
        viewModelScope.launch {
            runBlocking {
                ScheduleFetcher.fetchAndParseSchedule(context, false)
            }
            _isReady.value = true
        }
    }
}