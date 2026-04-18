package com.kmno.dropdate.presentation.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    data object Schedule : Screen()
}
