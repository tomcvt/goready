package com.tomcvt.goready.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tomcvt.goready.R
import com.tomcvt.goready.activities.AlarmActivity
import com.tomcvt.goready.constants.EXTRA_ALARM_ID
import com.tomcvt.goready.data.AlarmDatabase
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.repository.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: AlarmRepository

    private var mediaPlayer: MediaPlayer? = null

    var isRinging = false
    var isTemporarilyMuted = false
    var muteUntil: Long = 0L

    var isActive: Boolean = false


    override fun onCreate() {
        super.onCreate()
        val db = AlarmDatabase.getDatabase(this)
        repository = AlarmRepository(db.alarmDao())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1) ?: -1
        Log.d("AlarmForegroundService", "onStartCommand with alarm ID: $alarmId")

        if (alarmId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == "USER_INTERACTION") {
            muteUntil = System.currentTimeMillis() + 5000
            isTemporarilyMuted = true
            pauseAlarm()
            return START_STICKY
        }

        if (intent?.action == "STOP_ALARM") {
            stopAlarmSound()
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            val alarm = repository.getAlarmById(alarmId)
            if (alarm == null) {
                stopSelf()
                return@launch
            }
            Log.d("AlarmForegroundService", "Alarm found: $alarm")
            if (!alarm.isEnabled) {
                Log.d("AlarmForegroundService", "Alarm is disabled")
                stopSelf()
                return@launch
            }
            isActive = true
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val result = audioManager.requestAudioFocus(
                { /* optional: handle focus changes */ },
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN
            )

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // 2️⃣ Start the alarm sound
                Log.d("AlarmService", "Audio focus granted, starting alarm sound")
            } else {
                Log.w("AlarmService", "Audio focus denied, starting alarm sound anyway")
            }

            startAlarmSound(alarm)
            startAsForeground(alarm)
        }

        serviceScope.launch {
            while (isActive) {
                delay(500)

                if (isTemporarilyMuted && System.currentTimeMillis() >= muteUntil) {
                    isTemporarilyMuted = false
                    resumeSound()
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        serviceScope.cancel()
        isActive = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createAlarmChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alarm_channel",
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null) // sound handled by MediaPlayer
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun alarmActivityPendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALARM_ID, alarmId)
        }

        return PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startAsForeground(alarm: AlarmEntity) {
        createAlarmChannel()

        val fullScreenIntent = alarmActivityPendingIntent(alarm.id)

        val notification = NotificationCompat.Builder(this, "alarm_channel")
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Alarm")
            .setContentText("Wake up")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenIntent, true)
            .build()

        startForeground(1, notification)
    }

    private fun startAlarmSound(alarm: AlarmEntity) {
        var soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alarm.soundUri != null) {
            soundUri = Uri.parse(alarm.soundUri)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(audioAttributes)
            setDataSource(this@AlarmForegroundService, soundUri)
            isLooping = true
            setVolume(100f, 100f)
            prepare()
            start()
        }
        //mediaPlayer = MediaPlayer.create(this, soundUri)
        //mediaPlayer?.isLooping = true
        //mediaPlayer?.setVolume(100f, 100f)
        //mediaPlayer?.start()
    }

    private fun stopAlarmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(null)
    }

    private fun pauseAlarm() {
        mediaPlayer?.pause()
    }

    private fun resumeSound() {
        mediaPlayer?.start()
    }
}