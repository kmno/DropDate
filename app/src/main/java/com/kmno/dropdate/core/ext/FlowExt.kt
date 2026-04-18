package com.kmno.dropdate.core.ext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

sealed interface FlowResult<out T> {
    data class Success<T>(val data: T) : FlowResult<T>
    data class Error(val throwable: Throwable) : FlowResult<Nothing>
}

fun <T> Flow<T>.asResult(): Flow<FlowResult<T>> =
    map<T, FlowResult<T>> { FlowResult.Success(it) }
        .catch { emit(FlowResult.Error(it)) }
