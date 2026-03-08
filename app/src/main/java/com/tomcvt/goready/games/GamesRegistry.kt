package com.tomcvt.goready.games

class GamesRegistry() {
    val games = mutableMapOf<String, GameEntry>(
        Pair("test",GameEntry("test", "TEST", "test.html", false)),
        Pair("gltest",GameEntry("gltest", "GL Test", "index3.html", false, "spikes")),
        Pair("spikes",GameEntry("spikes", "Spikes", "index.html", false, "spikes")),
        Pair("spikesgl",GameEntry("spikesgl", "Spikes GL", "index2.html", false, "spikes"))
    )

    fun addGame(gameEntry: GameEntry) {
        games[gameEntry.id] = gameEntry
    }

    fun getList() : List<GameEntry> {
        return games.values.toList().filter { it.id != "test" }
    }
}