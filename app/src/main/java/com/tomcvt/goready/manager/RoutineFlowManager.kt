package com.tomcvt.goready.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tomcvt.goready.R
import com.tomcvt.goready.activities.RoutineFlowActivity
import com.tomcvt.goready.constants.EXTRA_ROUTINE_INFO
import com.tomcvt.goready.constants.EXTRA_ROUTINE_SESSION_ID
import com.tomcvt.goready.constants.EXTRA_ROUTINE_STEP
import com.tomcvt.goready.data.RoutineSession
import com.tomcvt.goready.data.RoutineStatus
import com.tomcvt.goready.data.StepStatus
import com.tomcvt.goready.repository.RoutineRepository
import com.tomcvt.goready.repository.RoutineSessionRepository
import com.tomcvt.goready.repository.RoutineStepRepository
import com.tomcvt.goready.repository.StepDefinitionRepository
import kotlinx.coroutines.flow.first

private const val TAG = "RoutineFlowManager"
private const val MINUTE = 60000L
private const val NOTIF_ID = 1119
private const val SHOW_UI_REQUEST_CODE = 13

class RoutineFlowManager(
    private val routineRepository: RoutineRepository,
    private val routineStepRepository: RoutineStepRepository,
    private val stepDefinitionRepository: StepDefinitionRepository,
    private val routineSessionRepository: RoutineSessionRepository,
    private val routineScheduler: RoutineScheduler,
    private val context: Context
) {

    suspend fun stepStartedPersistentNotify(sessionId: Long, routineId: Long, stepNumber: Int) {
        val session = routineSessionRepository.getRoutineSessionByIdFlow(sessionId).first()
        if (session == null) {
            Log.e(TAG, "Session not found")
            return
        }
        val routine = routineRepository.getRoutineById(routineId)
        if (routine == null) {
            Log.e(TAG, "Routine not found")
            return
        }
        val steps = routineStepRepository.getRoutineStepsWithDefinitionFlow(routineId).first()
        val isLastStep = stepNumber == steps.size - 1
        val timeoutMinutes = steps[stepNumber].length

        getRoutineFlowChannel()
        //TODO how this notif channel things work

        val uiIntent = routineActivityIntent(sessionId, stepNumber)
        val pendingIntentUi = PendingIntent.getActivity(
            context,
            SHOW_UI_REQUEST_CODE,
            uiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifActionUi = NotificationCompat.Action.Builder(
            R.drawable.ic_gicon,
            "Show Ui",
            pendingIntentUi
        ).build()
        val chronometerTime = SystemClock.elapsedRealtime() + timeoutMinutes * MINUTE

        val notification = NotificationCompat.Builder(context, "routine_flow_channel")
            .setSmallIcon(R.drawable.ic_gicon)
            .setContentTitle("Routine ${routine.name} running")
            .setContentText("Step ${stepNumber + 1} of ${steps.size} running")
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setWhen(chronometerTime)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .addAction(notifActionUi)
            .build()


    }

    suspend fun stepFinishedTimeout(sessionId: Long, routineId: Long, stepNumber: Int) {
        val session = routineSessionRepository.getRoutineSessionByIdFlow(sessionId).first()
        if (session == null) {
            Log.e(TAG, "Session not found")
            return
        }
        val routine = routineRepository.getRoutineById(routineId)
        if (routine == null) {
            Log.e(TAG, "Routine not found")
            return
        }

        val steps = routineStepRepository.getRoutineStepsWithDefinitionFlow(routineId).first()
        val isLastStep = stepNumber == steps.size - 1

        //TODO is last step = skip routine step -> finish routine
        getRoutineFlowChannel()
        //TODO how this notif channel things work

        val uiIntent = routineActivityIntent(sessionId, stepNumber)
        val pendingIntentUi = PendingIntent.getActivity(
            context,
            SHOW_UI_REQUEST_CODE,
            uiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifAction = NotificationCompat.Action.Builder(
            R.drawable.ic_gicon,
            "Show Ui",
            pendingIntentUi
        ).build()

        val notification = NotificationCompat.Builder(context, "routine_flow_channel")
            .setSmallIcon(R.drawable.ic_gicon)
            .setContentTitle("Routine ${routine.name}")
            .setContentText("Step ${stepNumber + 1} of ${steps.size} time is up")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(pendingIntentUi, true)
            .build()

        //launch
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID, notification)
    }

    suspend fun startRoutine(routineId: Long) : Long {
        clearRunningRoutines()
        val routine = routineRepository.getRoutineById(routineId)
        val steps = routineStepRepository.getRoutineStepsWithDefinitionFlow(routineId).first()
        //val firstEndTimeMinutes = steps.firstOrNull()?.length ?: 5L
        //TODO later change, for now test
        val firstEndTimeMinutes = 5L
        val session = RoutineSession(
            routineId = routineId,
            stepNumber = 0,
            stepStatus = StepStatus.RUNNING,
            stepStartTime = System.currentTimeMillis(),
            status = RoutineStatus.RUNNING,
            startTime = System.currentTimeMillis(),
            endTime = null //TODO calculate end time
        )
        val sessionId = routineSessionRepository.insertRoutineSession(session)
        routineScheduler.scheduleStepTimeout(sessionId, routineId, 0, firstEndTimeMinutes.toInt())
        return sessionId
    }

    suspend fun clearRunningRoutines() {
        val sessions = routineSessionRepository.getRoutineSessionsByStatusFlow(RoutineStatus.RUNNING).first()
        sessions.forEach { session ->
            routineSessionRepository.updateRoutineSession(session.copy(status = RoutineStatus.CANCELED))
        }
    }

    private fun getRoutineFlowChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel("routine_flow_channel")
            if (existingChannel != null) {
                return
            }
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val channel = NotificationChannel(
                "routine_flow_channel",
                "Routine Flow",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(defaultSoundUri, null)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun routineActivityIntent(sessionId: Long, stepNumber: Int, info: String? = null) : Intent {
        val intent = Intent(context, RoutineFlowActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTINE_SESSION_ID, sessionId)
            putExtra(EXTRA_ROUTINE_STEP, stepNumber)
            putExtra(EXTRA_ROUTINE_INFO, info)
        }
        return intent
    }
}