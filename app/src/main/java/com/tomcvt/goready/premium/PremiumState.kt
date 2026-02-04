package com.tomcvt.goready.premium

import java.time.Instant

data class PremiumState(
    val isPremium: Boolean = false,
    val source: PremiumSource = PremiumSource.DEV,
    val expiresAt: Instant? = null
)
