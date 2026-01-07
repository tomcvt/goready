package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek

@Composable
fun AlarmAddedModal(text: String?, modifier: Modifier = Modifier, onDismiss: () -> Unit , hour : Int, minute: Int, days: Set<DayOfWeek>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.clickable(enabled = false) {},
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(text = text ?: "No viewModel action message")
                Text(
                    text = "Alarm set for %02d:%02d on %s".format(
                        hour,
                        minute,
                        if (days.isEmpty()) "no days" else days.joinToString { it.name.take(3) }
                    )
                )
                Button(onClick = onDismiss) { Text("OK") }
            }
        }
    }
}