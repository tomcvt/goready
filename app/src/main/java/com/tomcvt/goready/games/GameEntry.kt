package com.tomcvt.goready.games

data class GameEntry(
    val id: String,
    val name: String,
    val filename: String,
    val premium: Boolean,
    val relPath: String? = null
)