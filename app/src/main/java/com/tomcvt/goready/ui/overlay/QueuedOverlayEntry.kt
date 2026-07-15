package com.tomcvt.goready.ui.overlay

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable

data class QueuedOverlayEntry(
    val id: Any = Any(),
    val durationMillis: Long? = null, // null = no auto-dismiss, manual only
    val content: @Composable (
        dismiss: () -> Unit,
        enter: EnterTransition,
        exit: ExitTransition
    ) -> Unit
)
