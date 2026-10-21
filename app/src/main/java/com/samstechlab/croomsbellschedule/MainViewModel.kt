package com.samstechlab.croomsbellschedule

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val evaluation: ScheduleEvaluation? = null,
    val connectionMessage: String = "Loading schedule…",
    val statusCode: Int = 1,
    val errorMessage: String? = null
)

@Immutable
data class FeedUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: List<FeedItem> = emptyList(),
    val statusMessage: String = "Loading feed…",
    val statusCode: Int = 1,
    val errorMessage: String? = null,
    val hasUnread: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Secondary constructor for backward compatibility if called with Context
    constructor(context: Context) : this(context.applicationContext as Application)

    private val appContext: Context = application.applicationContext
    private val dataStoreManager = MyDataStoreManager(appContext)
    private val feedRepository = CroomsshedFeed()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _rawSchedule = MutableStateFlow<List<List<ScheduleBlock>>>(emptyList())
    private val _scheduleConnection = MutableStateFlow("Loading schedule…")
    private val _scheduleStatusCode = MutableStateFlow(1)
    private val _scheduleError = MutableStateFlow<String?>(null)
    private val _isScheduleLoading = MutableStateFlow(true)
    private val _isScheduleRefreshing = MutableStateFlow(false)
    private val _quipText = MutableStateFlow(defaultQuip())

    val preferencesState: StateFlow<UserPreferencesSnapshot> = dataStoreManager.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferencesSnapshot()
        )

    private val tickerFlow = flow {
        while (currentCoroutineContext().isActive) {
            emit(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    val homeUiState: StateFlow<HomeUiState> = combine(
        _rawSchedule,
        preferencesState,
        tickerFlow,
        combine(
            _isScheduleLoading,
            _isScheduleRefreshing,
            _scheduleConnection,
            _scheduleStatusCode,
            _scheduleError
        ) { loading, refreshing, conn, code, err ->
            ScheduleMeta(loading, refreshing, conn, code, err)
        }
    ) { schedule, prefs, _, meta ->
        val evaluation = if (schedule.isNotEmpty()) {
            ScheduleFetcher.evaluateSchedule(
                schedule = schedule,
                prefs = prefs,
                now = LocalTime.now(),
                today = LocalDate.now(),
                afterSchoolQuip = _quipText.value
            )
        } else {
            null
        }
        HomeUiState(
            isLoading = meta.isLoading && evaluation == null,
            isRefreshing = meta.isRefreshing,
            evaluation = evaluation,
            connectionMessage = meta.connectionMessage,
            statusCode = meta.statusCode,
            errorMessage = meta.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    private val _feedUiState = MutableStateFlow(FeedUiState())
    val feedUiState: StateFlow<FeedUiState> = _feedUiState.asStateFlow()

    init {
        loadInitialSchedule()
        loadFeed(refresh = false)
    }

    private fun loadInitialSchedule() {
        viewModelScope.launch {
            _isScheduleLoading.value = true
            runCatching {
                _quipText.value = afterSchoolQuips(appContext)
            }
            val (result, connection, status) = ScheduleFetcher.fetchAndParseSchedule(appContext, refresh = false)
            result.fold(
                onSuccess = { blocks ->
                    _rawSchedule.value = blocks
                    _scheduleError.value = null
                },
                onFailure = { err ->
                    _scheduleError.value = err.message ?: "Unable to load schedule"
                }
            )
            _scheduleConnection.value = connection
            _scheduleStatusCode.value = status
            _isScheduleLoading.value = false
            _isReady.value = true
            updateWidgets()
        }
    }

    fun refreshSchedule() {
        viewModelScope.launch {
            _isScheduleRefreshing.value = true
            val (result, connection, status) = ScheduleFetcher.fetchAndParseSchedule(appContext, refresh = true)
            result.fold(
                onSuccess = { blocks ->
                    _rawSchedule.value = blocks
                    _scheduleError.value = null
                },
                onFailure = { err ->
                    if (_rawSchedule.value.isEmpty()) {
                        _scheduleError.value = err.message ?: "Unable to refresh schedule"
                    }
                }
            )
            _scheduleConnection.value = connection
            _scheduleStatusCode.value = status
            _isScheduleRefreshing.value = false
            updateWidgets()
        }
    }

    fun loadFeed(refresh: Boolean = false) {
        viewModelScope.launch {
            _feedUiState.update { state ->
                if (refresh) state.copy(isRefreshing = true, errorMessage = null)
                else state.copy(isLoading = state.items.isEmpty(), errorMessage = null)
            }

            val (result, statusMsg, statusCode) = feedRepository.fetchAndGetFeedItems(appContext, refresh = refresh)
            result.fold(
                onSuccess = { fetchedItems ->
                    _feedUiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            items = fetchedItems,
                            statusMessage = statusMsg,
                            statusCode = statusCode,
                            errorMessage = if (fetchedItems.isEmpty()) "No announcements in the feed right now." else null,
                            hasUnread = fetchedItems.isNotEmpty() && state.items.isEmpty()
                        )
                    }
                },
                onFailure = { err ->
                    _feedUiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            statusMessage = statusMsg,
                            statusCode = statusCode,
                            errorMessage = err.message ?: "Failed to load feed announcements."
                        )
                    }
                }
            )
        }
    }

    fun markFeedSeen() {
        _feedUiState.update { it.copy(hasUnread = false) }
    }

    fun updateLunchPreference(lunch: String) {
        viewModelScope.launch {
            dataStoreManager.saveData(PreferenceKeys.LUNCH_PREF, lunch)
            updateWidgets()
        }
    }

    fun updateAltLunchPreference(lunch: String) {
        viewModelScope.launch {
            dataStoreManager.saveData(PreferenceKeys.ALT_LUNCH_PREF, lunch)
            updateWidgets()
        }
    }

    fun updatePeriodName(periodNumber: Int, customName: String) {
        viewModelScope.launch {
            val trimmed = customName.trim()
            val defaultLabel = if (periodNumber == 8) "Homeroom" else "Period $periodNumber"
            val valueToSave = trimmed.ifEmpty { defaultLabel }
            dataStoreManager.saveData(PreferenceKeys.periodKey(periodNumber), valueToSave)
            updateWidgets()
        }
    }

    fun resetAllPeriodNames() {
        viewModelScope.launch {
            for (i in 1..7) {
                dataStoreManager.saveData(PreferenceKeys.periodKey(i), "Period $i")
            }
            dataStoreManager.saveData(PreferenceKeys.periodKey(8), "Homeroom")
            updateWidgets()
        }
    }

    private fun updateWidgets() {
        viewModelScope.launch {
            runCatching {
                TimerWidget.updateAll(appContext)
            }
        }
    }

    private data class ScheduleMeta(
        val isLoading: Boolean,
        val isRefreshing: Boolean,
        val connectionMessage: String,
        val statusCode: Int,
        val errorMessage: String?
    )
}
