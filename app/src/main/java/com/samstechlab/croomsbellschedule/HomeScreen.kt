package com.samstechlab.croomsbellschedule

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samstechlab.compose.AppTheme
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IndeterminateExpressiveReloadingLoop(
    modifier: Modifier = Modifier,
    animationSpec: androidx.compose.animation.core.InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = tween(durationMillis = 1500, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
) {
    val progress = rememberInfiniteTransition(label = "indeterminateProgress").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = animationSpec,
        label = "progressFloat"
    )

    LoadingIndicator(
        progress = { progress.value },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel(),
    bottomNavPadding: PaddingValues = PaddingValues(0.dp)
) {
    val homeState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val collapsedFraction = scrollBehavior.state.collapsedFraction
            val interpolatedContainerColor = colorLerp(
                start = MaterialTheme.colorScheme.background,
                stop = MaterialTheme.colorScheme.surfaceContainer,
                fraction = collapsedFraction
            )
            val interpolatedFontSize = lerp(
                start = MaterialTheme.typography.headlineLarge.fontSize,
                stop = MaterialTheme.typography.titleLarge.fontSize,
                fraction = collapsedFraction
            )

            MediumFlexibleTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = interpolatedContainerColor,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Text(
                        text = "Crooms Bell Schedule",
                        fontSize = interpolatedFontSize,
                        fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
                        fontWeight = MaterialTheme.typography.headlineLarge.fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.refreshSchedule()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Schedule"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        HomeContent(
            uiState = homeState,
            innerPadding = innerPadding,
            bottomNavPadding = bottomNavPadding,
            onRefresh = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.refreshSchedule()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    innerPadding: PaddingValues,
    bottomNavPadding: PaddingValues = PaddingValues(0.dp),
    onRefresh: () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        state = pullToRefreshState,
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        if (uiState.isLoading && uiState.evaluation == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LoadingIndicator(modifier = Modifier.size(56.dp))
                    Text(
                        text = "Syncing today's bell schedule…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val evaluation = uiState.evaluation
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 112.dp + bottomNavPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "status_header") {
                    ConnectionStatusRow(
                        connectionMessage = uiState.connectionMessage,
                        isOnline = uiState.statusCode == 1,
                        selectedLunch = evaluation?.selectedLunchLabel ?: "Lunch A",
                        isWednesday = evaluation?.isWednesday == true,
                        onRefreshClick = onRefresh
                    )
                }

                if (evaluation != null) {
                    item(key = "hero_countdown") {
                        ExpressiveHeroCountdownCard(evaluation = evaluation)
                    }

                    if (evaluation.periods.isNotEmpty()) {
                        item(key = "schedule_section_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = evaluation.scheduleDayTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${(evaluation.dayProgress * 100).toInt()}% of day",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = evaluation.periods,
                            key = { index, period -> "${period.block.eventId}_${period.block.startHour}_${period.block.startMinute}_$index" }
                        ) { _, period ->
                            SchedulePeriodCard(period = period)
                        }
                    }
                } else {
                    item(key = "error_card") {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = uiState.errorMessage ?: "No schedule available for today.",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusRow(
    connectionMessage: String,
    isOnline: Boolean,
    selectedLunch: String,
    isWednesday: Boolean,
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val chipColors = if (!isOnline) {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        } else {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                leadingIconContentColor = MaterialTheme.colorScheme.primary
            )
        }

        AssistChip(
            onClick = onRefreshClick,
            label = {
                Text(
                    text = connectionMessage,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isOnline) Icons.Rounded.Refresh else Icons.Rounded.CloudOff,
                    contentDescription = "Refresh status",
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = chipColors,
            shape = CircleShape,
            border = BorderStroke(0.dp, MaterialTheme.colorScheme.surfaceContainerHigh)
        )

        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isWednesday) "$selectedLunch (Wed)" else selectedLunch,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveHeroCountdownCard(evaluation: ScheduleEvaluation) {
    val containerColor by animateColorAsState(
        targetValue = when {
            !evaluation.isSchoolDayActive -> MaterialTheme.colorScheme.surfaceContainerHigh
            evaluation.isPassingPeriod -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        label = "heroContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !evaluation.isSchoolDayActive -> MaterialTheme.colorScheme.onSurface
            evaluation.isPassingPeriod -> MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        label = "heroContentColor"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = evaluation.currentProgress.coerceIn(0f, 1f),
        label = "heroWavyProgress"
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Status Badge + Progress Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = contentColor.copy(alpha = 0.12f),
                    contentColor = contentColor,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val badgeIcon = when {
                            !evaluation.isSchoolDayActive -> Icons.Rounded.Celebration
                            evaluation.isPassingPeriod -> Icons.AutoMirrored.Rounded.DirectionsWalk
                            else -> Icons.Rounded.Schedule
                        }
                        val badgeText = when {
                            !evaluation.isSchoolDayActive -> "AFTER SCHOOL"
                            evaluation.isPassingPeriod -> "PASSING PERIOD"
                            else -> "CURRENT PERIOD"
                        }
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (evaluation.isSchoolDayActive) {
                    Text(
                        text = "${(evaluation.currentProgress * 100).toInt()}% elapsed",
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }

            // Period Name & Big Expressive Countdown
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedContent(
                    targetState = evaluation.headlineTitle,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "headlineTitleAnim"
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = contentColor
                    )
                }

                if (evaluation.isSchoolDayActive) {
                    Text(
                        text = evaluation.countdownClock,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        color = contentColor
                    )
                }
            }

            // Subtitle or Quip
            Text(
                text = evaluation.subtitleText,
                style = if (evaluation.isSchoolDayActive) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = contentColor.copy(alpha = 0.88f)
            )

            // Material 3 Expressive Wavy Progress Indicator during active school day
            if (evaluation.isSchoolDayActive) {
                LinearWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SchedulePeriodCard(period: ResolvedSchedulePeriod) {
    val isCurrent = period.state == PeriodState.CURRENT
    val isCompleted = period.state == PeriodState.COMPLETED

    val containerColor = when (period.state) {
        PeriodState.CURRENT -> MaterialTheme.colorScheme.secondaryContainer
        PeriodState.UPCOMING -> MaterialTheme.colorScheme.surfaceContainer
        PeriodState.COMPLETED -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = when (period.state) {
        PeriodState.CURRENT -> MaterialTheme.colorScheme.onSecondaryContainer
        PeriodState.UPCOMING -> MaterialTheme.colorScheme.onSurface
        PeriodState.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isCompleted) 0.72f else 1f),
        shape = if (isCurrent) MaterialTheme.shapes.large else MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = if (isCurrent) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Period Badge / Status Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when (period.state) {
                                    PeriodState.CURRENT -> MaterialTheme.colorScheme.primary
                                    PeriodState.UPCOMING -> MaterialTheme.colorScheme.primaryContainer
                                    PeriodState.COMPLETED -> MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isCompleted -> {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            period.block.eventId in 1..7 -> {
                                Text(
                                    text = "P${period.block.eventId}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                            period.block.eventId == 8 || period.block.eventId == 103 -> {
                                Text(
                                    text = "HR",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                            period.block.eventId == 102 -> {
                                Icon(
                                    imageVector = Icons.Rounded.Restaurant,
                                    contentDescription = "Lunch",
                                    tint = if (isCurrent) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    tint = if (isCurrent) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = period.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (period.label != period.defaultLabel) {
                                Text(
                                    text = "• ${period.defaultLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = contentColor.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = period.formattedTimeRange,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right side pill: NOW badge or duration
                Surface(
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        contentColor.copy(alpha = 0.08f)
                    },
                    contentColor = if (isCurrent) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        contentColor.copy(alpha = 0.85f)
                    },
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isCurrent) "NOW • ${period.durationMinutes}m" else "${period.durationMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (isCurrent) {
                LinearWavyProgressIndicator(
                    progress = { period.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

fun refreshCache(context: Context) {
    runBlocking { ScheduleFetcher.fetchAndParseSchedule(context = context, refresh = true) }
}

@Composable
@Preview(showBackground = true)
fun HomeScreenPreview() {
    AppTheme {
        val sampleEval = ScheduleFetcher.evaluateSchedule(
            schedule = FallbackScheduleProvider.getTodayFallbackSchedule(),
            prefs = UserPreferencesSnapshot(
                lunchPreference = "Lunch A",
                periodNames = mapOf(1 to "Digital Design", 2 to "AP English", 3 to "AP Calculus")
            )
        )
        HomeContent(
            uiState = HomeUiState(
                isLoading = false,
                evaluation = sampleEval,
                connectionMessage = "Updated just now",
                statusCode = 1
            ),
            innerPadding = PaddingValues(0.dp),
            onRefresh = {}
        )
    }
}
