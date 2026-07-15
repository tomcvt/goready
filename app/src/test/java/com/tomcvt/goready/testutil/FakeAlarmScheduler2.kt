package com.tomcvt.goready.testutil

import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.manager.AlarmScheduler

class FakeAlarmScheduler2 : AlarmScheduler {
    val scheduledOneTime = mutableListOf<Long>()
    val scheduledNext = mutableListOf<Pair<Long, Long>>()
    val cancelled = mutableListOf<Long>()

    override fun scheduleOneTimeAlarm(alarm: AlarmEntity, alarmId: Long, remainingSnooze: Int) {
        scheduledOneTime += alarmId
    }
    override fun scheduleNextAlarm(alarm: AlarmEntity, alarmId: Long, remainingSnooze: Int, triggerTime: Long) {
        scheduledNext += alarmId to triggerTime
    }
    override fun scheduleSnooze(alarmId: Long, remainingSnooze: Int, snoozeTimeMinutes: Int) {}
    override fun cancelAlarm(alarm: AlarmEntity) { cancelled += alarm.id }
}