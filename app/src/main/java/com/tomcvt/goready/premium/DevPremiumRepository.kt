package com.tomcvt.goready.premium

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform

class DevPremiumRepository(
    private val appScope: CoroutineScope
) : PremiumRepositoryI {

    private val _state = MutableStateFlow(PremiumState(isPremium = true, source = PremiumSource.DEV))

    override val premiumState: StateFlow<PremiumState> = _state
    /*
    override val addsDisabled: StateFlow<Boolean> = premiumState.transform { state ->
        emit(state.addsPaidOff || state.isPremium)
    }
        .stateIn(appScope, SharingStarted.Eagerly, false)
     */
    override val addsDisabled: StateFlow<Boolean> = premiumState.map { state ->
        state.addsPaidOff || state.isPremium
    }.stateIn(appScope, SharingStarted.Eagerly, false)



    override fun setPremiumState(state: PremiumState) {
        _state.value = state
    }

    override fun setIsPremium(isPremium: Boolean) {
        _state.value = _state.value.copy(isPremium = isPremium)
    }

}
