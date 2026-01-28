package com.tomcvt.goready.manager

import android.content.Context

class RoutineScheduler(private val context: Context) {

    val alarmManager = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager



}