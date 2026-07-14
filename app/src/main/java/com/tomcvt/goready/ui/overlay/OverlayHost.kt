package com.tomcvt.goready.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf


class OverlayHost {
    private val _entries = mutableStateListOf<OverlayEntry>()
    val entries: List<OverlayEntry> get() = _entries

    fun show(content: @Composable (dismiss: () -> Unit) -> Unit) {
        lateinit var entry: OverlayEntry
        entry = OverlayEntry(content = {
            content { _entries.remove(entry) }
        })
        _entries.add(entry)
    }
}