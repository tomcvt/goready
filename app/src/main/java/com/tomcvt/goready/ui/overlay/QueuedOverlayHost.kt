package com.tomcvt.goready.ui.overlay

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf

class QueuedOverlayHost {
    private val _queue = mutableStateListOf<QueuedOverlayEntry>()
    val current: QueuedOverlayEntry?
        get() = _queue.firstOrNull()

    fun show(
        durationMillis: Long? = 3000L,
        content: @Composable (
            dismiss: () -> Unit,
            enter: EnterTransition,
            exit: ExitTransition
        ) -> Unit
    ) {
        _queue.add(QueuedOverlayEntry(durationMillis = durationMillis, content = {
            dismiss: () -> Unit,
            enter: EnterTransition,
            exit: ExitTransition ->
            content(dismiss, enter, exit)
        }))
    }

    // Guarded by id: if this was already removed (manual dismiss beat the
    // timer, or vice versa), this is a no-op instead of removing whatever
    // the *new* current entry happens to be.
    fun dismissCurrent(id: Any) {
        if (_queue.firstOrNull()?.id == id) {
            _queue.removeAt(0)
        }
    }
}