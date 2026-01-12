package com.tomcvt.goready.test

import android.content.Context
import android.content.Intent
import com.tomcvt.goready.activities.AlarmActivity
import com.tomcvt.goready.constants.EXTRA_ALARM_ID
import com.tomcvt.goready.constants.TaskType

fun Context.launchAlarm(alarmId: Long) {
    val intent = Intent(this, AlarmActivity::class.java).apply {
        putExtra(EXTRA_ALARM_ID, alarmId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

fun Context.launchAlarmNow(alarmId: Long, overrideTaskType: String? = null) {
    var taskCode = -1L
    if (overrideTaskType != null) {
        val taskType = TaskType.fromLabel(overrideTaskType)
        if (taskType != null) {
            taskCode = taskType.code.toLong()
        }
    }
    val intent = Intent(this, AlarmActivity::class.java).apply {
        putExtra(EXTRA_ALARM_ID, alarmId)
        putExtra("TestAlarm", true)
        putExtra("OverriddenTaskType", taskCode)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}