package com.tomcvt.goready.testutil

import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.manager.AlarmScheduler

class FakeAlarmScheduler : AlarmScheduler {

    var scheduledOneTime: Triple<AlarmEntity, Long, Int>? = null
    var scheduledNext: Quadruple<AlarmEntity, Long, Int, Long>? = null
    var cancelled: AlarmEntity? = null

    override fun scheduleOneTimeAlarm(
        alarm: AlarmEntity,
        alarmId: Long,
        remainingSnooze: Int
    ) {
        scheduledOneTime = Triple(alarm, alarmId, remainingSnooze)
    }

    override fun scheduleNextAlarm(
        alarm: AlarmEntity,
        alarmId: Long,
        remainingSnooze: Int,
        triggerTime: Long
    ) {
        scheduledNext = Quadruple(alarm, alarmId, remainingSnooze, triggerTime)
    }

    override fun scheduleSnooze(alarmId: Long, remainingSnooze: Int, snoozeTimeMinutes: Int) {}

    override fun cancelAlarm(alarm: AlarmEntity) {
        cancelled = alarm
    }
}