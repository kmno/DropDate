package com.kmno.dropdate.presentation

sealed interface OneTimeEvents {
    data class SnackAlert(
        val message: String,
    ) : OneTimeEvents

    data class DialogAlert(
        val title: String,
        val message: String,
    ) : OneTimeEvents
}
