package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly

@Composable
fun TextInputCard(
    onTextChange: (String) -> Unit,
    onFocusLost: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var internalText by remember { mutableStateOf("") }

    OutlinedTextField(
        value = internalText,
        onValueChange = { internalText = it; onTextChange(it) },
        placeholder = { Text(placeholder) },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (!state.isFocused) {
                    // Call parent lambda when focus leaves
                    onFocusLost(internalText)
                }
            },
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = false,        // allows multiple lines
        maxLines = 5              // or any number you want
    )
}

@Composable
fun NumbersInput(
    onNumbersChange: (String) -> Unit,
    onFocusLost: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var internalText by remember { mutableStateOf("") }

    OutlinedTextField(
        value = internalText,
        onValueChange = { if (it.isDigitsOnly()) internalText = it;
                            onNumbersChange(it)},
        placeholder = { Text(placeholder) },
        modifier = Modifier
            .size(50.dp)
            .padding(8.dp)
            .onFocusChanged { state ->
                if(!state.isFocused) {
                    // Call parent lambda when focus leaves
                    onFocusLost(internalText)
                }
            },

        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = true
    )
}

@Composable
fun MathTaskInput(
    onInputChange: (String) -> Unit,
    onFocusLost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box() {
        Text("Math Task Input")
    }
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