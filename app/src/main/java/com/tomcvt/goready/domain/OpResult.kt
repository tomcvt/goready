package com.tomcvt.goready.domain

sealed class OpResult<out T> {
    data class Success<T>(val value: T) : OpResult<T>()
    data class Error(val error: Throwable?) : OpResult<Nothing>()
}