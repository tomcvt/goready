package com.tomcvt.goready.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tomcvt.goready.premium.PremiumRepositoryI

class SettingsViewModelFactory(
    private val premiumRepository: PremiumRepositoryI
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(premiumRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")

    }
}