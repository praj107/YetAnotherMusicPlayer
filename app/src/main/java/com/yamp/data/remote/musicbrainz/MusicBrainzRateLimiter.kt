package com.yamp.data.remote.musicbrainz

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MusicBrainzRateLimiter {
    private val mutex = Mutex()
    private var lastRequestTime = 0L
    private val minIntervalMs = 1100L // MusicBrainz requires max 1 req/sec

    suspend fun <T> throttle(block: suspend () -> T): T {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < minIntervalMs) {
                delay(minIntervalMs - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()
        }
        return block()
    }
}
