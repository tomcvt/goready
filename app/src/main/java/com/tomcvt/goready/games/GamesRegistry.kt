package com.tomcvt.goready.games

class GamesRegistry() {
    val games = mutableMapOf<String, GameEntry>(Pair("test",GameEntry("test", "test.html", false)))

    fun addGame(gameEntry: GameEntry) {
        games[gameEntry.id] = gameEntry
    }
}