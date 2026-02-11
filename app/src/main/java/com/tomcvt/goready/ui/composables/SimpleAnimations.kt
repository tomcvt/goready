package com.tomcvt.goready.ui.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun SimpleTickCircleAnimations(
    checked: Boolean,
    size: Dp = 300.dp,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(0.5f) }
    val iconSize = size * 3 / 4


    LaunchedEffect(checked) {
        if (checked) {
            coroutineScope {
                launch {
                    rotation.animateTo(
                        380f,
                        animationSpec = tween(600, easing = FastOutSlowInEasing)
                    )
                    rotation.animateTo(
                        360f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    scale.animateTo(
                        1.2f,
                        animationSpec = tween(600, easing = FastOutSlowInEasing)
                    )
                    scale.animateTo(
                        1f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    )
                }
            }
        } else {
            rotation.snapTo(0f)
            scale.snapTo(0.5f)
        }
    }
    Box (
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation.value
                scaleX = scale.value
                scaleY = scale.value
            }
            .background(
                color = Color.Green,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Tick",
            tint = Color.White,
            modifier = Modifier.align(Alignment.Center)
                .size(iconSize)
        )
    }

}