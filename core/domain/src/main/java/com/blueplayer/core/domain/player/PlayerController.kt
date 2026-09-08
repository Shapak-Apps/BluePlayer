package com.blueplayer.core.domain.player

import com.blueplayer.core.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface PlayerController {
    val state: StateFlow<PlayerState>
    val options: StateFlow<PlaybackOptions>
    val audioSessionId: StateFlow<Int?>

    suspend fun connect()
    suspend fun disconnect()

    fun playTracks(tracks: List<Track>, startIndex: Int)
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)

    fun toggleShuffle()
    fun cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float)

    fun setABPointA()
    fun setABPointB()
    fun clearAB()

    fun startSleepTimer(minutes: Int, waitTrackFinish: Boolean)
    fun startSleepAtTrackEnd()
    fun startSleepAtQueueEnd()
    fun cancelSleepTimer()
    fun setVolume(volume: Float)
}