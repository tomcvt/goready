package com.tomcvt.goready.time

class RealTimeProvider() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}