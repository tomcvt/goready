package com.tomcvt.goready.premium

import kotlinx.coroutines.flow.StateFlow

interface PremiumRepositoryI {
    val premiumState: StateFlow<PremiumState>
    fun setPremiumState(state: PremiumState)
    fun setIsPremium(isPremium: Boolean)

}
