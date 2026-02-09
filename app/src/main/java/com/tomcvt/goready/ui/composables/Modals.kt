package com.tomcvt.goready.ui.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.constants.SNOOZE_COUNTS
import com.tomcvt.goready.constants.SNOOZE_MINUTES
import com.tomcvt.goready.data.AlarmEntity
import java.time.DayOfWeek

@Composable
fun AlarmAddedModal(
    text: String?,
    taskData: String?,
    onDismiss: () -> Unit ,
    hour : Int,
    minute: Int,
    days: Set<DayOfWeek>,
    modifier: Modifier = Modifier,
    secondary: String? = null
) {
    BackHandler(
        enabled = true,
        onBack = onDismiss
    )
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
                Text(text = text?: "No viewmodel message")
                Text(text = taskData?: "")
                Text(
                    text = "Alarm set for %02d:%02d on %s".format(
                        hour,
                        minute,
                        if (days.isEmpty()) "no days" else days.joinToString { it.name.take(3) }
                    )
                )
                if (secondary != null) {
                    Text(text = secondary)
                }
                Button(onClick = onDismiss) { Text("OK") }
            }
        }
    }
}
@Composable
fun CustomSuccessModal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BackHandler(
        enabled = true,
        onBack = onDismiss
    )
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
                content()
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onDismiss) { Text("OK") }
                }
            }
        }
    }
}


@Composable
fun DeleteAlarmModal(onDismiss: () -> Unit, onConfirm: () -> Unit) {
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
                Text(text = "Are you sure you want to delete this alarm?")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun StandardModal(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit) {
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
                content()
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun SnoozeInputModal(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    startingCount: Int = SNOOZE_COUNTS[0],
    startingTime: Int = SNOOZE_MINUTES[2],
    onInputChange: (
        snoozeCount: Int,
        snoozeTime: Int
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    require(startingCount in SNOOZE_COUNTS)
    require(startingTime in SNOOZE_MINUTES)

    var snoozeCount by remember { mutableStateOf(startingCount) }
    var snoozeTime by remember { mutableStateOf(startingTime) }

    BackHandler(
        enabled = true,
        onBack = onDismiss
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable {
                onInputChange(snoozeCount, snoozeTime)
                onDismiss()
            },
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
                Text("Choose snooze time")
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Count:")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Minutes:")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    WheelPicker(
                        items = SNOOZE_COUNTS,
                        startingItem = snoozeCount,
                        visibleItems = 3,
                        itemHeight = 40.dp,
                        onItemSelected = { snoozeCount = it },
                        modifier = Modifier.width(60.dp)
                    ) { item, selected ->
                        Text(
                            text = item.toString(),
                            modifier = Modifier.graphicsLayer {
                                scaleX = if (selected) 1.25f else 1f
                                scaleY = if (selected) 1.25f else 1f
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    WheelPicker(
                        items = SNOOZE_MINUTES,
                        startingItem = snoozeTime,
                        visibleItems = 3,
                        itemHeight = 40.dp,
                        onItemSelected = { snoozeTime = it },
                        modifier = Modifier.width(60.dp)
                    ) { item, selected ->
                        Text(
                            text = item.toString(),
                            modifier = Modifier.graphicsLayer {
                                scaleX = if (selected) 1.25f else 1f
                                scaleY = if (selected) 1.25f else 1f
                            }
                        )
                    }
                }
            }
        }
    }
}

/*
StepTimeModal(
                initialTime = rEditorState.steps[uiState.stepModalNumber!!].second,
                onDismiss = { viewModel.clearStepModalNumber() },
                onProvidedValue = { viewModel.updateTimeForStepInEditor(it, uiState.stepModalNumber)
                                    viewModel.clearStepModalNumber()},
                list = STEP_TIMES_LIST
            )
 */

@Composable
fun StepTimeModal(
    initialTime: Int,
    onDismiss: () -> Unit,
    onProvidedValue: (Int) -> Unit,
    list: List<Int>,
) {
    BackHandler(
        enabled = true,
        onBack = onDismiss
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(enabled = false) {},
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "Choose step duration",
                    style = MaterialTheme.typography.headlineSmall
                )
                WheelPicker(
                    items = list,
                    startingItem = initialTime,
                    visibleItems = 3,
                    itemHeight = 50.dp,
                    onItemSelected = onProvidedValue,
                    modifier = Modifier.width(60.dp)
                ) { item, selected ->
                    Text(
                        text = item.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.graphicsLayer {
                            scaleX = if (selected) 1.25f else 1f
                            scaleY = if (selected) 1.25f else 1f
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun <T> SelectItemFlowRowModal(
    text: String,
    list: List<T>,
    onDismiss: () -> Unit,
    onItemSelected: (T) -> Unit,
    onConfirm: (T?) -> Unit,
    modifier: Modifier = Modifier,
    startingItem: T? = null,
    itemContent: @Composable (T, Boolean, () -> Unit) -> Unit,
) {
    require(startingItem in list || startingItem == null)
    var selectedItem by remember { mutableStateOf(startingItem) }

    val onSelected = { item: T ->
        selectedItem = item
        onItemSelected(item)
    }

    BackHandler(
        enabled = true,
        onBack = onDismiss
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(enabled = false) {},
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.headlineSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    if (list.isNotEmpty()) {
                        list.forEach { item ->
                            itemContent(
                                item,
                                selectedItem == item
                            ) {
                                onSelected(item)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onConfirm(selectedItem) }) { Text("OK") }
                }
            }
        }
    }
}