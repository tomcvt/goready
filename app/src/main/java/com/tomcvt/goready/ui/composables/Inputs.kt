package com.tomcvt.goready.ui.composables

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.core.text.isDigitsOnly
import com.tomcvt.goready.constants.MathType

@Composable
fun TextInputCard(
    value: String,
    onTextChange: (String) -> Unit,
    onFocusLost: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {

    OutlinedTextField(
        value = value,
        onValueChange = { onTextChange(it) },
        placeholder = { Text(placeholder) },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (!state.isFocused) {
                    // Call parent lambda when focus leaves
                    onFocusLost(value)
                }
            },
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = false,        // allows multiple lines
        maxLines = 5              // or any number you want
    )
}

@Composable
fun NumbersInput(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusLost: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(digitsOnly(it))},
        placeholder = { Text(placeholder) },
        modifier = modifier
            .padding(8.dp)
            .onFocusChanged { state ->
                if(!state.isFocused) {
                    // Call parent lambda when focus leaves
                    onFocusLost(value)
                }
            },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textAlign = TextAlign.Center, fontSize = fontSize
        ),
        singleLine = true
    )
}

fun digitsOnly(value: String): String {
    return value.filter { it.isDigit() }
}

@Preview(showBackground = true)
@Composable
fun MathTypeInputPreview() {
    MaterialTheme {
        MathTaskInput(
            value = "FIRST|7",
            onInputChange = {},
            onFocusLost = {}
        )
    }
}

@Composable
fun MathTaskInput(
    value: String,
    onInputChange: (String) -> Unit,
    onFocusLost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultValue = remember(value) { setDefault(value) }
    Log.d("MathTaskInput", "defaultValue: $defaultValue")
    //var selectedType by rememberUpdatedState(typeFromValue)
    //var selectedRange by rememberUpdatedState(rangeFromValue)
    val mathTypes = MathType.getList()
    //val defaultValue = setDefault(value)
    var selectedType by remember { mutableStateOf(defaultValue.first) }
    val range = (1..15).toList()
    var selectedRange by remember { mutableStateOf(defaultValue.second) }
    var inputData by remember { mutableStateOf("${selectedType.name}|$selectedRange") }
    val encodeData = {
        inputData = "${selectedType.name}|${selectedRange}"
    }
    LaunchedEffect(selectedType, selectedRange) {
        encodeData()
        onInputChange(inputData)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose level and number of tasks:",
            style = MaterialTheme.typography.bodyLarge
        )
        Row(
            modifier = modifier.fillMaxWidth().padding(PaddingValues(32.dp, 0.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WheelPicker(
                items = mathTypes,
                startingItem = defaultValue.first,
                visibleItems = 3,
                itemHeight = 40.dp,
                onItemSelected = { selectedType = it }
            ) { item, selected ->
                Text(
                    text = item.label,
                    fontSize = 16.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = if (selected) 1.25f else 1f
                        scaleY = if (selected) 1.25f else 1f
                    },
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
            WheelPicker(
                items = range,
                startingItem = defaultValue.second,
                visibleItems = 3,
                itemHeight = 40.dp,
                onItemSelected = { selectedRange = it },
                width = 40.dp
            ) { item, selected ->
                Text(
                    text = item.toString(),
                    fontSize = 16.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = if (selected) 1.25f else 1f
                        scaleY = if (selected) 1.25f else 1f
                    },
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}

fun setDefault(value: String) : Pair<MathType, Int> {
    if (value.isBlank()) {
        return MathType.SECOND to 3

    }
    var type = MathType.SECOND
    var range = 3
    if (!value.contains("|")) {
        return MathType.SECOND to 3
    }
    try {
        type = MathType.valueOf(value.split("|")[0])
        range = value.split("|")[1].toInt()
    } catch (e: Exception) {
        return MathType.SECOND to 3
    }
    return type to range
}

@Preview(showBackground = true)
@Composable
fun TextInputCardPreview() {
    MaterialTheme{
        SnapWheelPicker(
            items = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5"),
            onValueChange = {}
        )
    }
}



@Deprecated("Use WheelPicker instead")
@Composable
fun SnapWheelPicker(
    items: List<String>,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    visibleItems: Int = 3,
    onValueChange: (Int) -> Unit
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = 0)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
    val density = LocalDensity.current

    Box(
        modifier = modifier.height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = state,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(items) { index, item ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.toString(),
                        fontSize = 20.sp,
                        color = if (state.firstVisibleItemIndex == index) Color.Black else Color.Gray
                    )
                }
            }
        }

        // Draw selection overlay
        Box(
            Modifier
                .height(itemHeight)
                .fillMaxWidth()
                .background(Color.LightGray.copy(alpha = 0.2f))
        )
    }

    // Update selected value reliably using scroll position
    LaunchedEffect(state.firstVisibleItemScrollOffset, state.firstVisibleItemIndex) {
        val offsetPx = with(density) { itemHeight.toPx() }
        val selectedIndex = (state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset / offsetPx)
            .toInt()
            .coerceIn(0, items.lastIndex)
        onValueChange(selectedIndex)
    }
}