package com.yamp.player

import com.yamp.domain.model.Track

class QueueManager {
    private var _queue: MutableList<Track> = mutableListOf()
    private var _currentIndex: Int = -1
    private var _shuffleEnabled: Boolean = false
    private var _originalQueue: List<Track> = emptyList()

    val queue: List<Track> get() = _queue.toList()
    val currentIndex: Int get() = _currentIndex
    val currentTrack: Track? get() = _queue.getOrNull(_currentIndex)
    val shuffleEnabled: Boolean get() = _shuffleEnabled
    val hasNext: Boolean get() = _currentIndex < _queue.size - 1
    val hasPrevious: Boolean get() = _currentIndex > 0

    fun setQueue(tracks: List<Track>, startIndex: Int = 0) {
        _originalQueue = tracks.toList()
        _queue = tracks.toMutableList()
        _currentIndex = startIndex.coerceIn(0, (_queue.size - 1).coerceAtLeast(0))
        if (_shuffleEnabled) applyShuffle()
    }

    fun skipToNext(): Track? {
        if (_queue.isEmpty()) return null
        _currentIndex = (_currentIndex + 1) % _queue.size
        return currentTrack
    }

    fun skipToPrevious(): Track? {
        if (_queue.isEmpty()) return null
        _currentIndex = if (_currentIndex > 0) _currentIndex - 1 else _queue.size - 1
        return currentTrack
    }

    fun skipToIndex(index: Int): Track? {
        if (index !in _queue.indices) return null
        _currentIndex = index
        return currentTrack
    }

    fun toggleShuffle(): Boolean {
        _shuffleEnabled = !_shuffleEnabled
        if (_shuffleEnabled) {
            applyShuffle()
        } else {
            restoreOrder()
        }
        return _shuffleEnabled
    }

    private fun applyShuffle() {
        val current = currentTrack ?: return
        val others = _queue.filterIndexed { i, _ -> i != _currentIndex }.shuffled()
        _queue = (listOf(current) + others).toMutableList()
        _currentIndex = 0
    }

    private fun restoreOrder() {
        val current = currentTrack ?: return
        _queue = _originalQueue.toMutableList()
        _currentIndex = _queue.indexOf(current).coerceAtLeast(0)
    }

    fun addToQueue(track: Track) {
        _queue.add(track)
    }

    fun removeFromQueue(index: Int) {
        if (index !in _queue.indices) return
        _queue.removeAt(index)
        if (index < _currentIndex) _currentIndex--
        if (_currentIndex >= _queue.size) _currentIndex = _queue.size - 1
    }

    fun clear() {
        _queue.clear()
        _originalQueue = emptyList()
        _currentIndex = -1
    }
}
