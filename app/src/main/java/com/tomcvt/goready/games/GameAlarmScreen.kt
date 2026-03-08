package com.tomcvt.goready.games

import android.os.Handler
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.webkit.WebViewAssetLoader
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
                relPath = gamesRegistry.games[gameId]?.relPath,
                onStopAlarm = onStopAlarm,
                onInteraction = onInteraction,
                skippable = dismissable,
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
    modifier: Modifier = Modifier,
    skippable: Boolean = false,
    relPath: String? = null
) {
    val context = LocalContext.current
    val onGameFinished: (Long) -> Unit = {
        Log.d("GameBridge", "Game finished with score: $it")
        onStopAlarm()
    }
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val response = assetLoader.shouldInterceptRequest(request?.url?: return null)
                    if (request.url.path?.endsWith(".js") == true && response != null) {
                        response.mimeType = "application/javascript"
                    }
                    Log.d("WebView",
                        "Request: ${subend(request.url.toString())}, Response: ${substart(response?.toString()?: "")}")
                    return response
                }
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    Log.e("WebView", "Error: ${error?.description} for ${request?.url}")
                }
            }
            //settings.allowFileAccessFromFileURLs = true
            //settings.allowUniversalAccessFromFileURLs = true
            //TODO remember to change this
            addJavascriptInterface(GameBridge(
                webViewHandler,
                onGameFinished,
                onInteraction,
                skippable
            ), "AndroidBridge")
            if (relPath != null) {
                //loadUrl("file:///android_asset/webview_games/$relPath/$gameFilename")
                loadUrl("https://appassets.androidplatform.net/assets/webview_games/$relPath/$gameFilename")
            } else {
                //loadUrl("file:///android_asset/webview_games/${gameFilename}")
                loadUrl("https://appassets.androidplatform.net/assets/webview_games/${gameFilename}")
            }
        }
    }
    BoxWithConstraints(
        modifier = modifier
    ) {
        val width = maxWidth
        val height = maxHeight
        Log.d("WebViewContainerBox", "Width: $width, Height: $height")
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { webView },
        )
    }
}

fun subend(str: String) : String {
    return str.substring((str.length - 20).coerceAtLeast(0), str.length)
}
fun substart(str: String) : String {
    return str.substring(0, 20.coerceAtMost(str.length))
}
