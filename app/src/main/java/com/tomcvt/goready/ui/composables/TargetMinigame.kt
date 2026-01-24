package com.tomcvt.goready.ui.composables

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.constants.TaskType
import kotlin.random.Random


@Composable
fun CountdownTargetMinigame(
    rounds: Int,
    dismissable: Boolean,
    onDismiss: () -> Unit,
    onInteraction: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    targetSize: Dp = 64.dp,
) {
    var currentRound by remember { mutableStateOf(0) }
    val random = remember { Random(System.currentTimeMillis()) }

    var onPressed = {
        currentRound++
        onInteraction()
    }

    LaunchedEffect(currentRound) {

        if (currentRound >= rounds) {
            onFinish()
        }
    }

    BackHandler(
        enabled = false,
        onBack = { }
    )
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val maxWidth = constraints.maxWidth
        val maxHeight = constraints.maxHeight

        var offset by remember { mutableStateOf(Offset.Zero)}
        val targetSizeFloat = targetSize.value

        LaunchedEffect(currentRound, maxWidth, maxHeight) {
            offset = Offset(
                random.nextInt(maxWidth - targetSizeFloat.toInt()).toFloat(),
                random.nextInt(maxHeight - targetSizeFloat.toInt()).toFloat()
            )
        }

        Box(modifier = Modifier
            .size(targetSize)
            .align(Alignment.TopStart)
            .offset {
                IntOffset(offset.x.toInt(), offset.y.toInt())
            }
        ) {
            GameTarget(
                label = currentRound.toString(),
                onClick = onPressed,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (dismissable) {
            SimpleDeleteButton(
                onDelete = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
fun GameTarget(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box (
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
        ) {

        }
        Text(
            text = label,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center
        )
    }
}