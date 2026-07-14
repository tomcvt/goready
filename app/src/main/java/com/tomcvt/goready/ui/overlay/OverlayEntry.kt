package com.tomcvt.goready.ui.overlay

import androidx.compose.runtime.Composable

data class OverlayEntry(val id: Any = Any(), val content: @Composable () -> Unit)

