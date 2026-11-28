package com.example.mobilefinalproject.strength.domain

class RestTimer {
    var seconds: Int = 60
        private set
    private var deadline: Long? = null
    private var paused: Long? = null

    fun select(seconds: Int) {
        require(seconds == 60 || seconds == 90)
        this.seconds = seconds
        reset()
    }

    fun start(now: Long) {
        deadline = now + seconds * 1000L
        paused = null
    }

    fun remaining(now: Long): Long =
        deadline?.let { (it - now).coerceAtLeast(0L) } ?: paused ?: (seconds * 1000L)

    fun isRunning(now: Long) = deadline != null && remaining(now) > 0
    fun isPaused() = paused != null
    fun isFinished(now: Long) = deadline != null && remaining(now) == 0L

    fun pause(now: Long) {
        if (!isRunning(now)) return
        paused = remaining(now)
        deadline = null
    }

    fun resume(now: Long) {
        val remaining = paused ?: return
        deadline = now + remaining
        paused = null
    }

    fun reset() {
        deadline = null
        paused = null
    }
}
