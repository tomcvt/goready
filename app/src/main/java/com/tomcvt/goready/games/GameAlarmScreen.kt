package com.tomcvt.goready.games

import android.os.Handler
import android.util.Log
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tomcvt.goready.ui.composables.CountdownTargetMinigame
import com.tomcvt.goready.ui.composables.FlexCloseButton

@Composable
fun WebviewGameAlarmScreen(
    webViewHandler: Handler,
    gameId: String,
    gamesRegistry: GamesRegistry,
    onStopAlarm: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    dismissable: Boolean = false
) {

    var interactionKey by remember { mutableStateOf(0L) }
    var lastInteraction by remember { mutableStateOf(0L) }


    LaunchedEffect(interactionKey) {
        if (System.currentTimeMillis() - lastInteraction > 2000L) {
            onInteraction()
            lastInteraction = System.currentTimeMillis()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            interactionKey++
                        }
                    }
                }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Solve the task!",
                        style = MaterialTheme.typography.headlineLarge,
                        //fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            WebViewContainerBox(
                webViewHandler = webViewHandler,
                gameFilename = gamesRegistry.games[gameId]?.filename?: "test.html",
                onStopAlarm = onStopAlarm,
                onInteraction = onInteraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(bottom = 64.dp)
            )
        }
        if (dismissable) {
            FlexCloseButton(
                onClose = onStopAlarm,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
fun WebViewContainerBox(
    webViewHandler: Handler,
    gameFilename: String,
    onStopAlarm: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val onGameFinished: (Long) -> Unit = {
        Log.d("GameBridge", "Game finished with score: $it")
        onStopAlarm()
    }
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(GameBridge(
                webViewHandler,
                onGameFinished,
                onInteraction,
            ), "AndroidBridge")
            loadUrl("file:///android_asset/webview_games/${gameFilename}")
        }
    }
    BoxWithConstraints(
        modifier = modifier
    ) {
        val width = maxWidth
        val height = maxHeight
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { webView },
        )
    }
}