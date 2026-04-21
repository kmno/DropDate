package com.kmno.dropdate.core

sealed interface AppError {
    data class Network(
        val message: String,
    ) : AppError

    data class Database(
        val message: String,
    ) : AppError

    data object Unknown : AppError
}
