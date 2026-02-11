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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun SimpleTickCircleAnimations(
    checked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 300.dp
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

@Composable
fun CardPopupAnimated(
    launched: Boolean,
    modifier: Modifier = Modifier,
    animDuration: Int = 500,
    content: @Composable () -> Unit
) {
    var heightPx by remember { mutableStateOf(0) }
    val translation = remember { Animatable(0f) }
    val alphaA = remember { Animatable(0f) }
    val scale = remember { Animatable(0f) }
    val scaleTimeA = animDuration * 4 / 5
    val scaleTimeB = animDuration / 5

    LaunchedEffect(heightPx, launched) {
        if (heightPx > 0 && launched) {
            coroutineScope {
                launch {
                    translation.snapTo(heightPx / 2f)
                    translation.animateTo(
                        0f,
                        animationSpec = tween(animDuration, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    alphaA.snapTo(0f)
                    alphaA.animateTo(
                        1f,
                        animationSpec = tween(animDuration, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    scale.snapTo(0f)
                    scale.animateTo(
                        1.1f,
                        animationSpec = tween(scaleTimeA, easing = FastOutSlowInEasing)
                    )
                    scale.animateTo(
                        1f,
                        animationSpec = tween(scaleTimeB, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
        if (heightPx > 0 && !launched) {
            coroutineScope {
                launch {
                    translation.snapTo(0f)
                }
                launch {
                    alphaA.snapTo(0f)
                }
                launch {
                    scale.snapTo(0f)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                heightPx = it.size.height
            }
            .graphicsLayer {
                translationY = translation.value
                alpha = alphaA.value
                scaleX = scale.value
                scaleY = scale.value
            }
    ) {
        content()
    }

}