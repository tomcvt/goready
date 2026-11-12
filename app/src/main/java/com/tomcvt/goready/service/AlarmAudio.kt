package com.tomcvt.goready.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.util.Log

class AlarmAudio(private val ctx: Context) {
    enum class Mode { IDLE, RINGING, MUTED }

    private class Track(val player: MediaPlayer) { @Volatile var ready = false }

    private var mode = Mode.IDLE
    private var ring: Track? = null
    private var calm: Track? = null

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private fun build(uri: Uri, gain: Float): Track? = try {
        val mp = MediaPlayer()
        val t = Track(mp)
        mp.setAudioAttributes(attrs)
        mp.setWakeMode(ctx, PowerManager.PARTIAL_WAKE_LOCK)
        mp.setDataSource(ctx, uri)
        mp.isLooping = true
        mp.setVolume(gain, gain)
        mp.setOnPreparedListener { t.ready = true; apply() }
        mp.setOnErrorListener { _, _, _ -> t.ready = false; true } // log, optionally fall back
        mp.prepareAsync()
        t
    } catch (e: Exception) { Log.e("AlarmAudio", "load failed: $uri", e); null }

    fun start(ringUri: Uri, calmUri: Uri?) {
        release()
        mode = Mode.RINGING
        ring = build(ringUri, 1f)             // fall back to default alarm URI if null
        calm = calmUri?.let { build(it, 0.4f) }
    }

    fun ring() { mode = Mode.RINGING; apply() }
    fun mute() { mode = Mode.MUTED;   apply() }

    private fun apply() {
        drive(ring, play = mode == Mode.RINGING)
        drive(calm, play = mode == Mode.MUTED)
    }

    private fun drive(t: Track?, play: Boolean) {
        val p = t?.takeIf { it.ready }?.player ?: return
        try {
            if (play) { if (!p.isPlaying) p.start() }
            else if (p.isPlaying) p.pause()
        } catch (e: IllegalStateException) { Log.w("AlarmAudio", "bad state", e) }
    }

    fun release() {
        mode = Mode.IDLE
        listOf(ring, calm).forEach { t ->
            t?.player?.let { runCatching { it.stop() }; it.release() }
        }
        ring = null; calm = null
    }
}