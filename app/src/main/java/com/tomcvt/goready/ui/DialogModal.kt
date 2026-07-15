package com.tomcvt.goready.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A composable that mimics [androidx.compose.ui.window.Dialog] but renders inline
 * (no separate window). Provides a dimmed backdrop, BackHandler, and optional
 * entry/exit animation via AnimatedVisibility parameters.
 *
 * Usage:
 *   overlayHost.show { dismiss ->
 *       DialogModal(onDismissRequest = dismiss) { MyContent() }
 *   }
 */
@Composable
fun DialogModal(
    onDismissRequest: () -> Unit,
    enter: EnterTransition = fadeIn(tween(200)) + scaleIn(initialScale = 0.92f, animationSpec = tween(200)),
    exit: ExitTransition = fadeOut(tween(180)) + scaleOut(targetScale = 0.92f, animationSpec = tween(180)),
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    BackHandler(onBack = onDismissRequest)

    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismissRequest() },
            contentAlignment = Alignment.Center
        ) {
            // Stop clicks inside the dialog from propagating to the backdrop
            Box(modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {}) {
                content()
            }
        }
    }
}
