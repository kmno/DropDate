package com.kmno.dropdate.presentation

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmno.dropdate.core.analytics.AnalyticsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseViewModel : ViewModel() {
    @Inject
    internal lateinit var analyticsHelper: AnalyticsHelper

    fun logAnalyticsEvent(
        name: String,
        params: Bundle = Bundle(),
    ) {
        analyticsHelper.logEvent(name, params)
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch { block() }
    }
}
