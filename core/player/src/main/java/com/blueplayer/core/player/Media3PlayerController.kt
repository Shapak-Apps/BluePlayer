package com.blueplayer.core.player

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.player.PlaybackOptions
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.player.PlayerState
import com.blueplayer.core.domain.player.RepeatModeUi
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class Media3PlayerController(
    private val context: Context,
    private val serviceComponent: ComponentName
) : PlayerController {

    companion object {
        private const val KEY_AUDIO_SESSION_ID = "audio_session_id"
    }

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _options = MutableStateFlow(PlaybackOptions())
    override val options: StateFlow<PlaybackOptions> = _options.asStateFlow()

    private val _audioSessionId = MutableStateFlow<Int?>(null)
    override val audioSessionId: StateFlow<Int?> = _audioSessionId.asStateFlow()

    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null

    private var pendingTracks: List<Track>? = null
    private var pendingIndex = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = Executor { command -> mainHandler.post(command) }

    private var sleepRunnable: Runnable? = null
    private var sleepWaitTrackFinish = false
    private var sleepPending = false
    private var sleepAtTrackEnd = false
    private var sleepAtQueueEnd = false

    private var abA: Long? = null
    private var abB: Long? = null
    private val abRunnable = object : Runnable {
        override fun run() {
            val c = controller
            val a = abA
            val b = abB
            if (c != null && a != null && b != null && b > a) {
                if (c.currentPosition >= b) c.seekTo(a)
            }
            mainHandler.postDelayed(this, 200)
        }
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            mainHandler.postDelayed(this, 500L)
        }
    }

    private val listener = object : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (sleepPending || sleepAtTrackEnd) {
                controller?.pause()
                cancelSleepTimer()
            }
            updateState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState()
            if (isPlaying) {
                mainHandler.removeCallbacks(progressRunnable)
                mainHandler.post(progressRunnable)
            } else {
                mainHandler.removeCallbacks(progressRunnable)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && sleepAtQueueEnd) {
                cancelSleepTimer()
            }
            _state.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
            updateState()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _options.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _options.update {
                it.copy(
                    repeatMode = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> RepeatModeUi.ONE
                        Player.REPEAT_MODE_ALL -> RepeatModeUi.ALL
                        else -> RepeatModeUi.OFF
                    }
                )
            }
        }
    }

    override suspend fun connect() {
        try {
            withContext(Dispatchers.Main) {
                if (controller != null || future != null) return@withContext

                val sessionToken = SessionToken(context, serviceComponent)
                val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
                future = controllerFuture

                controllerFuture.addListener(
                    {
                        try {
                            val mediaController = controllerFuture.get()
                            controller = mediaController
                            mediaController.addListener(listener)

                            _options.update {
                                it.copy(
                                    shuffleEnabled = mediaController.shuffleModeEnabled,
                                    repeatMode = when (mediaController.repeatMode) {
                                        Player.REPEAT_MODE_ONE -> RepeatModeUi.ONE
                                        Player.REPEAT_MODE_ALL -> RepeatModeUi.ALL
                                        else -> RepeatModeUi.OFF
                                    }
                                )
                            }

                            val extras = mediaController.sessionExtras
                            val sessionId = extras?.getInt(KEY_AUDIO_SESSION_ID, 0) ?: 0
                            if (sessionId != 0) _audioSessionId.value = sessionId

                            processPendingQueue()
                            updateState()
                            mainHandler.post(progressRunnable)
                        } catch (e: Exception) {
                            future = null
                        }
                    },
                    mainExecutor
                )
            }
        } catch (e: Exception) {
            future = null
        }
    }

    override suspend fun disconnect() {
        try {
            withContext(Dispatchers.Main) {
                mainHandler.removeCallbacks(progressRunnable)
                mainHandler.removeCallbacks(abRunnable)
                controller?.removeListener(listener)
                runCatching { future?.let { MediaController.releaseFuture(it) } }
                controller = null
                future = null
            }
        } catch (e: Exception) {
        }
    }

    override fun playTracks(tracks: List<Track>, startIndex: Int) {
        val mediaController = controller

        if (mediaController == null) {
            pendingTracks = tracks
            pendingIndex = startIndex
            _state.update {
                it.copy(
                    queue = tracks, currentIndex = startIndex,
                    currentTrack = tracks.getOrNull(startIndex),
                    durationMs = tracks.getOrNull(startIndex)?.durationMs ?: 0L,
                    positionMs = 0L
                )
            }
            return
        }

        try {
            mediaController.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
            mediaController.prepare()
            mediaController.play()

            _state.update {
                it.copy(
                    queue = tracks, currentIndex = startIndex,
                    currentTrack = tracks.getOrNull(startIndex),
                    durationMs = tracks.getOrNull(startIndex)?.durationMs ?: 0L,
                    positionMs = 0L
                )
            }
        } catch (e: Exception) {
            pendingTracks = tracks
            pendingIndex = startIndex
        }
    }

    override fun togglePlayPause() {
        try {
            val c = controller ?: return
            if (c.isPlaying) c.pause() else c.play()
        } catch (e: Exception) { /* безопасно */ }
    }

    override fun next() {
        try {
            controller?.seekToNextMediaItem()
            controller?.play()
        } catch (e: Exception) { /* безопасно */ }
    }

    override fun previous() {
        try {
            controller?.seekToPreviousMediaItem()
            controller?.play()
        } catch (e: Exception) { /* безопасно */ }
    }

    override fun seekTo(positionMs: Long) {
        try {
            controller?.seekTo(positionMs)
        } catch (e: Exception) { /* безопасно */ }
    }

    override fun toggleShuffle() {
        try {
            val c = controller ?: return
            c.shuffleModeEnabled = !c.shuffleModeEnabled
        } catch (e: Exception) { /* безопасно */ }
    }

    override fun cycleRepeatMode() {
        try {
            val c = controller ?: return
            c.repeatMode = when (c.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        } catch (e: Exception) { /* безопасно */ }
    }

    override fun setPlaybackSpeed(speed: Float) {
        try {
            controller?.setPlaybackParameters(PlaybackParameters(speed))
            _state.update { it.copy(playbackSpeed = speed) }
        } catch (e: Exception) { /* безопасно */ }
    }

    override fun setABPointA() {
        abA = runCatching { controller?.currentPosition ?: 0L }.getOrDefault(0L)
        abB = null
        _options.update { it.copy(abState = 1) }
    }

    override fun setABPointB() {
        abB = runCatching { controller?.currentPosition ?: 0L }.getOrDefault(0L)
        _options.update { it.copy(abState = 2) }
        mainHandler.removeCallbacks(abRunnable)
        mainHandler.post(abRunnable)
    }

    override fun clearAB() {
        abA = null
        abB = null
        mainHandler.removeCallbacks(abRunnable)
        _options.update { it.copy(abState = 0) }
    }

    override fun startSleepTimer(minutes: Int, waitTrackFinish: Boolean) {
        cancelSleepTimer()
        sleepWaitTrackFinish = waitTrackFinish
        val r = Runnable {
            if (sleepWaitTrackFinish) sleepPending = true
            else {
                try { controller?.pause() } catch (_: Exception) {}
                cancelSleepTimer()
            }
        }
        sleepRunnable = r
        mainHandler.postDelayed(r, minutes * 60_000L)
        _options.update { it.copy(sleepTimerActive = true) }
    }

    override fun startSleepAtTrackEnd() {
        cancelSleepTimer()
        sleepAtTrackEnd = true
        _options.update { it.copy(sleepTimerActive = true) }
    }

    override fun startSleepAtQueueEnd() {
        cancelSleepTimer()
        sleepAtQueueEnd = true
        _options.update { it.copy(sleepTimerActive = true) }
    }

    override fun cancelSleepTimer() {
        sleepRunnable?.let { mainHandler.removeCallbacks(it) }
        sleepRunnable = null
        sleepWaitTrackFinish = false
        sleepPending = false
        sleepAtTrackEnd = false
        sleepAtQueueEnd = false
        _options.update { it.copy(sleepTimerActive = false) }
    }

    override fun setVolume(volume: Float) {
        try {
            controller?.setVolume(volume.coerceIn(0f, 1f))
        } catch (e: Exception) { /* безопасно */ }
    }

    private fun processPendingQueue() {
        val tracks = pendingTracks ?: return
        val index = pendingIndex
        pendingTracks = null
        playTracks(tracks, index)
    }

    private fun updateState() {
        val mediaController = controller ?: return
        val queue = _state.value.queue

        val currentMediaId = mediaController.currentMediaItem?.mediaId
        val index = queue.indexOfFirst { it.id == currentMediaId }

        val currentTrack = if (index >= 0) queue.getOrNull(index) else _state.value.currentTrack
        val durationMs = currentTrack?.durationMs
            ?: mediaController.duration.takeIf { it > 0 }
            ?: 0L

        val extras = mediaController.sessionExtras
        val sessionId = extras?.getInt(KEY_AUDIO_SESSION_ID, 0) ?: 0
        if (sessionId != 0) _audioSessionId.value = sessionId

        _state.update {
            it.copy(
                isPlaying = mediaController.isPlaying,
                currentIndex = if (index >= 0) index else it.currentIndex,
                currentTrack = currentTrack,
                durationMs = durationMs,
                positionMs = mediaController.currentPosition.takeIf { it >= 0 } ?: 0L,
                playbackSpeed = mediaController.playbackParameters.speed
            )
        }
    }

    private fun updateProgress() {
        val mediaController = controller ?: return
        _state.update {
            it.copy(
                positionMs = mediaController.currentPosition.takeIf { it >= 0 } ?: 0L,
                durationMs = mediaController.duration.takeIf { it > 0 } ?: it.durationMs,
                isPlaying = mediaController.isPlaying,
                playbackSpeed = mediaController.playbackParameters.speed
            )
        }
    }
}