package com.tomcvt.goready.ui.composables

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.constants.TaskType
import kotlin.random.Random


@Composable
fun CountdownTargetMinigame(
    rounds: Int,
    targetSize: Dp = 32.dp,
    dismissable: Boolean,
    onDismiss: () -> Unit,
    onInteraction: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentRound by remember { mutableStateOf(0) }
    val random = remember { Random(System.currentTimeMillis()) }

    var offsetX = remember { mutableStateOf(0) }
    var offsetY = remember { mutableStateOf(0) }

    var onPressed = {

    }

    LaunchedEffect(currentRound) {

        if (currentRound == rounds) {
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
        val maxWidthDp = maxWidth
        val maxHeightDp = maxHeight

        val width = maxWidthDp.value
        val height = maxHeightDp.value
        val targetSizeFloat = targetSize.value

        offsetX = random.nextInt((width-targetSizeFloat).toInt())
        offsetY = random.nextInt((height-targetSizeFloat).toInt())

        Box(modifier = Modifier
            .fillMaxSize()
            .align(Alignment.TopStart)
        ) {
            GameTarget(

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