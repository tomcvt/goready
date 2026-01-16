package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomcvt.goready.constants.MathType
import kotlin.math.roundToInt

/**
 * A preview composable for the `WheelPicker` component.
 * Demonstrates how the `WheelPicker` looks and behaves with sample data.
 */
@Preview(showBackground = true)
@Composable
fun WheelPickerPreview() {
    MaterialTheme {
        WheelPicker(
            items = MathType.values().toList(),
            startingItem = MathType.FOURTH,
            visibleItems = 3,
            itemHeight = 48.dp,
            onItemSelected = { mode ->
                println("Selected: $mode")
            }
        ) { item, selected ->
            Text(
                text = item.name,
                fontSize = 16.sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = if (selected) 1.25f else 1f
                    scaleY = if (selected) 1.25f else 1f
                },
                color = if (selected) Color.Black else Color.Gray
            )
        }
    }
}


/**
 * A composable that displays a vertically scrollable wheel picker.
 *
 * @param T The type of items in the picker.
 * @param items The list of items to display in the picker.
 * @param modifier The modifier to be applied to the picker.
 * @param startingItem The item to start the picker with.
 * @param visibleItems The number of items visible at a time (must be odd).
 * @param itemHeight The height of each item in the picker.
 * @param width The width of the picker.
 * @param onItemSelected A callback invoked when an item is selected.
 * @param itemContent A composable lambda to define the appearance of each item.
 *
 * @sample WheelPickerPreview
 */
@Composable
fun <T> WheelPicker(
    items: List<T>,
    modifier: Modifier = Modifier,
    startingItem: T = items[0],
    visibleItems: Int = 3,
    itemHeight: Dp = 48.dp,
    width: Dp = 240.dp,
    onItemSelected: (T) -> Unit,
    itemContent: @Composable (item: T, selected: Boolean) -> Unit
) {
    require(visibleItems % 2 == 1) { "visibleItems must be odd" }

    val padding = visibleItems / 2
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val density = LocalDensity.current

    var selectedIndex by remember { mutableStateOf(items.indexOf(startingItem)) }

    LaunchedEffect(Unit) {
        listState.scrollToItem(selectedIndex)
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItems)
            .width(width),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(padding) {
                item { Spacer(Modifier.height(itemHeight)) }
            }

            itemsIndexed(items) { index, item ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    itemContent(item, index == selectedIndex)
                }
            }

            repeat(padding) {
                item { Spacer(Modifier.height(itemHeight)) }
            }
        }

        // selection overlay
        Box(
            Modifier
                .height(itemHeight)
                .width(width)
                .background(Color.LightGray.copy(alpha = 0.2f))
        )
    }

    // single source of truth for selection
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val itemPx = with(density) { itemHeight.toPx() }

        val centeredLazyIndex =
            listState.firstVisibleItemIndex +
                    (listState.firstVisibleItemScrollOffset / itemPx).roundToInt()

        val realIndex = centeredLazyIndex// - padding
        val clamped = realIndex.coerceIn(0, items.lastIndex)

        if (clamped != selectedIndex) {
            selectedIndex = clamped
            onItemSelected(items[clamped])
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> InfiniteCircularList(
    width: Dp,
    itemHeight: Dp,
    numberOfDisplayedItems: Int = 3,
    items: List<T>,
    initialItem: T,
    itemScaleFact: Float = 1.5f,
    textStyle: TextStyle,
    textColor: Color,
    selectedTextColor: Color,
    onItemSelected: (index: Int, item: T) -> Unit = { _, _ -> }
) {
    val itemHalfHeight = LocalDensity.current.run { itemHeight.toPx() / 2f }
    val scrollState = rememberLazyListState(0)
    var lastSelectedIndex by remember {
        mutableIntStateOf(0)
    }
    var itemsState by remember {
        mutableStateOf(items)
    }
    LaunchedEffect(items) {
        var targetIndex = items.indexOf(initialItem) - 1
        targetIndex += ((Int.MAX_VALUE / 2) / items.size) * items.size
        itemsState = items
        lastSelectedIndex = targetIndex
        scrollState.scrollToItem(targetIndex)
    }
    LazyColumn(
        modifier = Modifier
            .width(width)
            .height(itemHeight * numberOfDisplayedItems),
        state = scrollState,
        flingBehavior = rememberSnapFlingBehavior(
            lazyListState = scrollState
        )
    ) {
        items(
            count = Int.MAX_VALUE,
            itemContent = { i ->
                val item = itemsState[i % itemsState.size]
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            val y = coordinates.positionInParent().y - itemHalfHeight
                            val parentHalfHeight = (itemHalfHeight * numberOfDisplayedItems)
                            val isSelected = (y > parentHalfHeight - itemHalfHeight && y < parentHalfHeight + itemHalfHeight)
                            val index = i - 1
                            if (isSelected && lastSelectedIndex != index) {
                                onItemSelected(index % itemsState.size, item)
                                lastSelectedIndex = index
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.toString(),
                        style = textStyle,
                        color = if (lastSelectedIndex == i) {
                            selectedTextColor
                        } else {
                            textColor
                        },
                        fontSize = if (lastSelectedIndex == i) {
                            textStyle.fontSize * itemScaleFact
                        } else {
                            textStyle.fontSize
                        }
                    )
                }
            }
        )
    }
}