package com.tomcvt.goready.domain

import java.time.DayOfWeek

data class SimpleAlarmDraft(
    val hour : Int,
    val minute : Int,
    val repeatDays: Set<DayOfWeek>
)
