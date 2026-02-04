package com.tomcvt.goready.premium

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DevPremiumRepository() : PremiumRepositoryI {

    private val _state = MutableStateFlow(PremiumState(isPremium = true, source = PremiumSource.DEV))

    override val premiumState: StateFlow<PremiumState> = _state

    override fun setPremiumState(state: PremiumState) {
        _state.value = state
    }

    override fun setIsPremium(isPremium: Boolean) {
        _state.value = _state.value.copy(isPremium = isPremium)
    }

}
