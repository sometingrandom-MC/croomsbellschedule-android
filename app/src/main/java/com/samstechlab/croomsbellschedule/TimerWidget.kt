package com.samstechlab.croomsbellschedule

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

object TimerWidget: GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val timerText = formatedTimer(context).first
            WidgetContent(timerText)
        }
    }
}

@Composable
private fun WidgetContent(timerText: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = timerText,
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = ColorProvider(Color.Black)
            )
        )
    }
}

class TimerWidgetReceiver: GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = TimerWidget
}
