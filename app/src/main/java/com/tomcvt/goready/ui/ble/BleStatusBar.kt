package com.tomcvt.goready.ui.ble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomcvt.goready.ble.BleConnectionState
import com.tomcvt.goready.ble.BleDeviceManager
import com.tomcvt.goready.ble.BleUiConnectionState
import com.tomcvt.goready.ui.theme.*
import kotlinx.coroutines.delay

private data class StatusStyle(val bg: Color, val fg: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

@Composable
private fun styleFor(state: BleUiConnectionState): StatusStyle {
    val dark = isSystemInDarkTheme()
    return when (state) {
        is BleUiConnectionState.Connected -> StatusStyle(
            bg = if (dark) StatusOkDark else StatusOkLight,
            fg = if (dark) OnStatusOkDark else OnStatusOkLight,
            icon = Icons.Filled.BluetoothConnected,
            label = "Connected"
        )
        is BleUiConnectionState.Connecting -> StatusStyle(
            bg = if (dark) StatusWorkingDark else StatusWorkingLight,
            fg = if (dark) OnStatusWorkingDark else OnStatusWorkingLight,
            icon = Icons.Filled.BluetoothSearching,
            label = "Connecting…"
        )
        is BleUiConnectionState.Disconnected -> StatusStyle(
            bg = if (dark) StatusErrorDark else StatusErrorLight,
            fg = if (dark) OnStatusErrorDark else OnStatusErrorLight,
            icon = Icons.Filled.BluetoothDisabled,
            label = "Disconnected"
        )
    }
}

@Composable
fun BleStatusBar(state: BleUiConnectionState, modifier: Modifier = Modifier) {
    val style = styleFor(state)
    val bg by animateColorAsState(style.bg, tween(200), label = "ble_status_bg")
    val fg by animateColorAsState(style.fg, tween(200), label = "ble_status_fg")

    // Only "Connected" collapses away — Connecting and Disconnected stay visible.
    //val state = state !is BleUiConnectionState.Connected
    var expanded by remember {mutableStateOf(true)}
    LaunchedEffect(state) {
        if (state !is BleUiConnectionState.Connected) {
            expanded = true
        } else {
            delay(2000)
            expanded = false
        }
    }

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(tween(220), expandFrom = Alignment.Top) + fadeIn(tween(150)),
        exit = shrinkVertically(tween(220), shrinkTowards = Alignment.Top) + fadeOut(tween(120)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = style.icon, contentDescription = null, tint = fg, modifier = Modifier)
            Spacer(Modifier.width(6.dp))
            Text(text = style.label, color = fg, fontSize = 12.sp, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// convenience overload wired directly to the manager
@Composable
fun BleStatusBar(bleDeviceManager: BleDeviceManager, modifier: Modifier = Modifier) {
    val state by bleDeviceManager.bleUiConnectionState.collectAsStateWithLifecycle()
    BleStatusBar(state = state, modifier = modifier)
}