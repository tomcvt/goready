package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek


@Composable
fun DaysRow(
    repeatDays: Set<DayOfWeek>,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        DayOfWeek.values().forEach { day ->
            val isSelected = repeatDays.contains(day)
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(32.dp)
                    .background(
                        //TODO: change to MaterialTheme color scheme when migrating to Material3, learning how to use color schemes
                        //color = if (isSelected) MaterialTheme.colors.primary else Color.LightGray,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.name.take(3),
                    color = if (isSelected) Color.White else Color.Black,
                    style = MaterialTheme.typography.bodySmall
                    //style = MaterialTheme.typography.body2 TODO: change to MaterialTheme typography when migrating to Material3, learning how to use typography
                )
            }
        }
    }
}