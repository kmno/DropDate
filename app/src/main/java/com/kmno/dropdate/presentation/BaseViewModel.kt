package com.kmno.dropdate.presentation

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmno.dropdate.core.analytics.AnalyticsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseViewModel : ViewModel() {
    @Inject
    internal lateinit var analyticsHelper: AnalyticsHelper

    private var _events = Channel<OneTimeEvents>(BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        events.shareIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT),
        )
    }

    fun sendEvent(event: OneTimeEvents) {
        _events.trySend(event)
    }

    fun logAnalyticsEvent(
        name: String,
        params: Bundle = Bundle(),
    ) {
        analyticsHelper.logEvent(name, params)
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        const val SUBSCRIBE_TIMEOUT = 5_000L
    }
}
