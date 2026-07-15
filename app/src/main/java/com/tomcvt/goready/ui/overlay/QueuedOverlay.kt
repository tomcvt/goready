package com.tomcvt.goready.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.ui.overlay.QueuedOverlayHost
import kotlinx.coroutines.delay

@Composable
fun QueuedOverlay(
    host: QueuedOverlayHost,
    modifier: Modifier = Modifier,
    defaultEnterTransition: EnterTransition = fadeIn() + slideInVertically(initialOffsetY = { -it }),
    defaultExitTransition: ExitTransition = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
    content: @Composable () -> Unit,
) {
    val entry = host.current

    Box(modifier = modifier.fillMaxSize()) {
        content() // your screens, rendered plainly, no animation wrapper
        Box(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
            if (entry != null) {
                key(entry.id) {
                    if (entry.durationMillis != null) {
                        LaunchedEffect(entry.id, entry.durationMillis) {
                            delay(entry.durationMillis)
                            host.dismissCurrent(entry.id)
                        }
                    }
                    entry.content(
                        { host.dismissCurrent(entry.id) },
                        defaultEnterTransition,
                        defaultExitTransition
                    )
                }
            }
        }
    }
}

@Composable
fun ToastNotification(
    message: String,
    onClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.let { m -> if (onClick != null) m.clickable { onClick() } else m },
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = if (onDismiss != null) 4.dp else 16.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = message, modifier = Modifier.weight(1f))
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                }
            }
        }
    }
}

@Composable
fun OverlayToastWrapper(
    message: String,
    dismiss: () -> Unit,
    enter: EnterTransition,
    exit: ExitTransition,
    onClick: (() -> Unit)? = null,
    showCloseButton: Boolean = true,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = true, // see note below
            enter = enter,
            exit = exit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
        ) {
            ToastNotification(
                message = message,
                onClick = onClick,
                onDismiss = if (showCloseButton) dismiss else null
            )
        }
    }
}