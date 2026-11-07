package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun InteractionDetectorWrapper(
    modifier: Modifier = Modifier,
    debounceTime: Long = 1000L,
    onInteraction: () -> Unit,
    content: @Composable () -> Unit) {
    var interactionKey by remember { mutableLongStateOf(0L) }
    var lastInteraction by remember { mutableLongStateOf(0L) }
    LaunchedEffect(interactionKey) {
        if (System.currentTimeMillis() - lastInteraction > debounceTime) {
            onInteraction()
            lastInteraction = System.currentTimeMillis()
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        interactionKey++
                    }
                }
            }
    ) {
        content()
    }
}