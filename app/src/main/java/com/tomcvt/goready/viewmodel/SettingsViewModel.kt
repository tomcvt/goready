package com.tomcvt.goready.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomcvt.goready.premium.PremiumRepositoryI
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val premiumRepository: PremiumRepositoryI
) : ViewModel() {
    val premiumState = premiumRepository.premiumState

    fun devTogglePremium() {
        val isPremium = premiumState.value.isPremium
        viewModelScope.launch {
            premiumRepository.setIsPremium(!isPremium)
        }
    }
}
