package com.tomcvt.goready.ui.imagevectors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconBell: ImageVector
    get() {
        return ImageVector.Builder(
            name = "Bell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1.0f
        ) {
            // The main body of the bell
            moveTo(12f, 22f)
            curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f)
            horizontalLineTo(10f)
            curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f)
            close()

            moveTo(18f, 16f)
            verticalLineTo(11f)
            curveTo(18f, 7.93f, 16.37f, 5.36f, 13.5f, 4.68f)
            verticalLineTo(4f)
            curveTo(13.5f, 3.17f, 12.83f, 2.5f, 12f, 2.5f)
            curveTo(11.17f, 2.5f, 10.5f, 3.17f, 10.5f, 4f)
            verticalLineTo(4.68f)
            curveTo(7.63f, 5.36f, 6f, 7.92f, 6f, 11f)
            verticalLineTo(16f)
            lineTo(4f, 18f)
            verticalLineTo(19f)
            horizontalLineTo(20f)
            verticalLineTo(18f)
            lineTo(18f, 16f)
            close()
        }.build()
    }
