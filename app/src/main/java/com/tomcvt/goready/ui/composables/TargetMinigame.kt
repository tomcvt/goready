package com.tomcvt.goready.ui.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tomcvt.goready.constants.TaskType


@Composable
fun CountdownTargetMinigame(
    rounds: Int,
    dismissable: Boolean,
    onDismiss: () -> Unit,
    onInteraction: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRounds by remember { mutableStateOf(0) }

    BackHandler(
        enabled = false,
        onBack = { }
    )
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

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
    Box (modifier = modifier) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
        ) {

        }
    }
}