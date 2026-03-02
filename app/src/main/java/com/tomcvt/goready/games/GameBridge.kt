package com.tomcvt.goready.games

import android.os.Handler
import android.webkit.JavascriptInterface

class GameBridge(
    private val mainHandler: Handler,
    private val onGameFinishedCallback: (Long) -> Unit,
    private val onInteractionCallback: () -> Unit,
    private val gameSkippable: Boolean
) {
    @JavascriptInterface
    fun onGameFinished(score: Long) {
        mainHandler.post { onGameFinishedCallback(score) }
    }
    @JavascriptInterface
    fun onInteraction() {
        mainHandler.post { onInteractionCallback() }
    }
    @JavascriptInterface
    fun isSkippable() : Boolean {
        return gameSkippable;
    }
}