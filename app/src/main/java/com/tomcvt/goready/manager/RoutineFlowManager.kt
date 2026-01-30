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
import com.tomcvt.goready.constants.ACTION_RF_UI_STEP_TIMEOUT
import com.tomcvt.goready.constants.ACTION_RF_UI_SHOW
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
private const val STATUS_CHANNEL = "routine_status_channel"
//TODO refactor to routine alarm channel
private const val FLOW_CHANNEL = "routine_flow_channel"


class RoutineFlowManager(
    private val routineRepository: RoutineRepository,
    private val routineStepRepository: RoutineStepRepository,
    private val stepDefinitionRepository: StepDefinitionRepository,
    private val routineSessionRepository: RoutineSessionRepository,
    private val routineScheduler: RoutineScheduler,
    private val context: Context
) {

    fun getRoutineSessionByIdFlow(sessionId: Long) = routineSessionRepository.getRoutineSessionByIdFlow(sessionId)

    fun getRoutineByIdFlow(routineId: Long) = routineRepository.getRoutineByIdFlow(routineId)

    fun getRoutineStepsWithDefinitionFlow(routineId: Long) = routineStepRepository.getRoutineStepsWithDefinitionFlow(routineId)

    fun getRoutineStepByNumberFlow(routineId: Long, stepNumber: Int) = routineStepRepository.getRoutineStepByNumberFlow(routineId, stepNumber)


    suspend fun stepStartedPersistentNotify(sessionId: Long, routineId: Long, stepNumber: Int) {
        val session = routineSessionRepository.getRoutineSessionByIdFlow(sessionId).first()
        if (session == null) {
            Log.d(TAG, "Session not found")
            return
        }
        val routine = routineRepository.getRoutineById(routineId)
        if (routine == null) {
            Log.d(TAG, "Routine not found")
            return
        }
        val steps = routineStepRepository.getRoutineStepsWithDefinitionFlow(routineId).first()
        val isLastStep = stepNumber == steps.size - 1
        Log.d(TAG, "isLastStep: $isLastStep")

        val currentStep = steps[stepNumber]
        Log.d(TAG, "currentStep: $currentStep")


        val timeoutMinutes = currentStep.length

        getRoutineStatusChannel()
        //TODO how this notif channel things work, initialize somewhere else

        val uiIntent = routineActivityIntentShowUiPersistent(sessionId, stepNumber, "RUNNING")
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
        //val chronometerTime = SystemClock.elapsedRealtime() + timeoutMinutes * MINUTE
        val chronometerTime = System.currentTimeMillis() + timeoutMinutes * MINUTE


        val notification = NotificationCompat.Builder(context, STATUS_CHANNEL)
            .setSmallIcon(R.drawable.ic_gicon)
            .setContentTitle("Routine ${routine.name} running")
            .setContentText("Step ${stepNumber + 1} of ${steps.size} running")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntentUi)
            .setWhen(chronometerTime)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            //.addAction(notifActionUi)
            .build()
        Log.d(TAG, "Notification built $notification")
        //launch
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID, notification)
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

        val uiIntent = routineActivityIntentShowUiStepTimeout(sessionId, stepNumber)
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

        val notification = NotificationCompat.Builder(context, FLOW_CHANNEL)
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

    suspend fun advanceToNextStep(sessionId: Long) {
        val session = routineSessionRepository.getRoutineSessionByIdFlow(sessionId).first()
        if (session == null) {
            Log.e(TAG, "Session not found")
            return
        }
        val nextStep = routineStepRepository.getRoutineStepByNumberFlow(session.routineId, session.stepNumber + 1).first()

        if (session.stepNumber == session.maxSteps - 1) {
            Log.d(TAG, "Routine finished")
            routineSessionRepository.updateRoutineSession(session.copy(status = RoutineStatus.COMPLETED))
        } else {
            val nextNumber = session.stepNumber + 1
            val nextStartTime = System.currentTimeMillis()
            routineSessionRepository.updateRoutineSession(
                session.copy(stepNumber = nextNumber, stepStartTime = nextStartTime))
        }


    }

    suspend fun startRoutine(routineId: Long) : Long {
        clearRunningRoutines()
        val routine = routineRepository.getRoutineById(routineId)
        val steps = routineStepRepository.getRoutineStepsWithDefinitionFlow(routineId).first()
        val firstEndTimeMinutes = steps.firstOrNull()?.length ?: 0L
        //TODO later change, for now test
        //val firstEndTimeMinutes = 5L
        if (firstEndTimeMinutes == 0L) {
            Log.e(TAG, "Routine has no steps")
            return -1
        }
        val session = RoutineSession(
            routineId = routineId,
            stepNumber = 0,
            stepStatus = StepStatus.RUNNING,
            stepStartTime = System.currentTimeMillis(),
            maxSteps = steps.size,
            status = RoutineStatus.RUNNING,
            startTime = System.currentTimeMillis(),
            endTime = null //TODO calculate end time
        )
        val sessionId = routineSessionRepository.insertRoutineSession(session)
        routineScheduler.scheduleStepTimeout(sessionId, routineId, 0, firstEndTimeMinutes.toInt())
        stepStartedPersistentNotify(sessionId, routineId, 0)
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
                .getNotificationChannel(FLOW_CHANNEL)
            if (existingChannel != null) {
                return
            }
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val channel = NotificationChannel(
                FLOW_CHANNEL,
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

    private fun getRoutineStatusChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel("routine_status_channel")
            if (existingChannel != null) {
                return
            }
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val channel = NotificationChannel(
                "routine_status_channel",
                "Routine Status",
                NotificationManager.IMPORTANCE_LOW
                ).apply {
                setSound(defaultSoundUri, null)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun routineActivityIntentShowUiPersistent(sessionId: Long, stepNumber: Int, info: String? = null) : Intent {
        val intent = Intent(context, RoutineFlowActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTINE_SESSION_ID, sessionId)
            putExtra(EXTRA_ROUTINE_STEP, stepNumber)
            putExtra(EXTRA_ROUTINE_INFO, info)
            setAction(ACTION_RF_UI_SHOW)
        }
        return intent
    }

    private fun routineActivityIntentShowUiStepTimeout(sessionId: Long, stepNumber: Int, info: String? = null) : Intent {
        val intent = Intent(context, RoutineFlowActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTINE_SESSION_ID, sessionId)
            putExtra(EXTRA_ROUTINE_STEP, stepNumber)
            putExtra(EXTRA_ROUTINE_INFO, info)
            setAction(ACTION_RF_UI_STEP_TIMEOUT)
        }
        return intent
    }
}