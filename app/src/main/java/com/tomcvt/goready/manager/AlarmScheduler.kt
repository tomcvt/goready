package com.tomcvt.goready.manager

import com.tomcvt.goready.data.AlarmEntity

interface AlarmScheduler {
    fun scheduleOneTimeAlarm(alarm: AlarmEntity, alarmId: Long, remainingSnooze: Int = 0)
    fun scheduleNextAlarm(alarm: AlarmEntity, alarmId: Long, remainingSnooze: Int = 0, triggerTime: Long)
    fun cancelAlarm(alarm: AlarmEntity)
    fun scheduleSnooze(alarmId: Long, remainingSnooze: Int, snoozeTimeMinutes: Int)
}
