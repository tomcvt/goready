package com.tomcvt.goready.premium

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "PremiumRepository"

class ProdPremiumRepository(
    //billing
    //api
) : PremiumRepositoryI {
    //TODO implement
    override val premiumState: StateFlow<PremiumState> = MutableStateFlow(PremiumState(isPremium = true, source = PremiumSource.GOOGLE_PLAY))
    override fun setPremiumState(state: PremiumState) {
        Log.d(TAG, "Not supposed to be called")
        TODO("Not yet implemented")
    }
    override fun setIsPremium(isPremium: Boolean) {
        Log.d(TAG, "Not supposed to be called")
        TODO("Not yet implemented")
    }



    /*
    val realFlow = combine(
        billingManager.premiumState,
        apiManager.premiumState
    ) { billingState, apiState ->
        resolvesPremiumState(billingState, apiState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = resolvesPremiumState(billingManager.premiumState.value, apiManager.premiumState.value)
    )
    */

}
