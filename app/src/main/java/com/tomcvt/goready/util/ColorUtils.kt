package com.tomcvt.goready.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

fun Color.contrastText(): Color =
    if (luminance() > 0.5f) {
        lerp(this, Color.Black, 0.5f)
    } else {
        lerp(this, Color.White, 0.5f)
    }