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
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.tomcvt.goready.R
import com.tomcvt.goready.activities.AlarmActivity
import com.tomcvt.goready.constants.ACTION_UI_HIDDEN
import com.tomcvt.goready.constants.ACTION_USER_INTERACTION
import com.tomcvt.goready.constants.EXTRA_ALARM_ID
import com.tomcvt.goready.constants.EXTRA_REMAINING_SNOOZE
import com.tomcvt.goready.data.AlarmDatabase
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.repository.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmForegroundService : Service() {

    //Notes: TODO
    // Only one alarm may be active at a time
    // Additional alarms are postponed
    // Service owns MediaPlayer check
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: AlarmRepository

    private lateinit var alarmContext: Context

    private var mediaPlayer: MediaPlayer? = null
    var isTemporarilyMuted = false
    var muteUntil: Long = 0L
    var isActive: Boolean = false

    var currentAlarm: AlarmEntity? = null
    var currentSnooze: Int = 0

    override fun onCreate() {
        super.onCreate()
        val db = AlarmDatabase.getDatabase(this)
        repository = AlarmRepository(db.alarmDao())
        alarmContext =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                createAttributionContext("alarm")
            } else {
                this
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1
        val remainingSnooze = intent?.getIntExtra(EXTRA_REMAINING_SNOOZE, -1) ?: -1
        Log.d("AlarmForegroundService", "onStartCommand with alarm ID: $alarmId and snooze: $remainingSnooze")

        if (alarmId == -1L && remainingSnooze == -1) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_UI_HIDDEN) {
            serviceScope.launch {
                delay(5000)
                if (!isActive) {
                    stopSelf()
                    return@launch
                }
                startAsForeground(currentAlarm!!, currentSnooze)
            }
        }

        if (intent?.action == ACTION_USER_INTERACTION) {
            muteUntil = System.currentTimeMillis() + 5000
            isTemporarilyMuted = true
            pauseAlarm()
            Log.d(TAG, "Alarm muted for 5 seconds")
            Log.d(TAG, "Mute until: $muteUntil")
            Log.d(TAG, "Is active: $isActive")
            return START_STICKY
        }

        if (intent?.action == "STOP_ALARM") {
            stopAlarmSound()
            isActive = false
            stopSelf()
            return START_NOT_STICKY
        }

        if (alarmId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        isActive = true

        serviceScope.launch {
            val alarm = withContext(Dispatchers.IO) {repository.getAlarmById(alarmId)}
            if (alarm == null) {
                stopSelf()
                return@launch
            }
            Log.d("AlarmForegroundService", "Alarm found: $alarm")
            //If receiver let it run, it should, there is disabling if not recurrent alarm
            /*
            if (!alarm.isEnabled) {
                Log.d("AlarmForegroundService", "Alarm is disabled")
                stopSelf()
                return@launch
            }
            */
            isActive = true
            currentAlarm = alarm
            currentSnooze = remainingSnooze

            val audioManager = alarmContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
            startAsForeground(alarm, remainingSnooze)
        }

        serviceScope.launch {
            Log.d(TAG, "---Checking active: $isActive")
            while (isActive) {
                delay(2000)
                Log.d(TAG, "Checking muted: $isTemporarilyMuted")
                if (isTemporarilyMuted && System.currentTimeMillis() >= muteUntil) {
                    Log.d(TAG, "Unmuting alarm")
                    try {
                        resumeSound()
                        isTemporarilyMuted = false
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resuming sound", e)
                    }
                }
            }
        }

        return START_STICKY
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
            val existingChannel = getSystemService(NotificationManager::class.java)
                .getNotificationChannel("alarm_channel")
            if (existingChannel != null) {
                return
            }
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

    private fun alarmActivityPendingIntent(alarmId: Long, remainingSnooze: Int): PendingIntent {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_REMAINING_SNOOZE, remainingSnooze)
        }

        return PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startAsForeground(alarm: AlarmEntity, remainingSnooze: Int) {
        createAlarmChannel()

        val fullScreenIntent = alarmActivityPendingIntent(alarm.id, remainingSnooze)

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
    }

    private fun stopAlarmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        val audioManager = alarmContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(null)
    }

    private fun pauseAlarm() {
        Log.d(TAG, "Pausing alarm")
        mediaPlayer?.pause()
    }

    private fun resumeSound() {
        mediaPlayer?.start()
    }

    companion object {
        private const val TAG = "AlarmForegroundService"
    }
}