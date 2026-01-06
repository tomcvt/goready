package com.tomcvt.goready.domain

import com.tomcvt.goready.DayOfWeek

data class SimpleAlarmDraft(
    val hour : Int,
    val minute : Int,
    val repeatDays: Set<DayOfWeek>
)
