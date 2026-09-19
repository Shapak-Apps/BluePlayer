package com.blueplayer.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.blueplayer.app.audio.NativeBassProcessor
import com.blueplayer.app.widget.WidgetUpdater
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MusicPlaybackService : MediaSessionService() {

    companion object {
        const val KEY_AUDIO_SESSION_ID = "audio_session_id"

        const val PREFS_LAST_SESSION = "blue_player_last_session"
        const val KEY_URI = "uri"
        const val KEY_POSITION = "position"
        const val KEY_PLAY_WHEN_READY = "play_when_ready"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_ALBUM = "album"
        const val KEY_ARTWORK = "artwork"

        private const val REPEAT_COMMAND = "com.blueplayer.app.REPEAT"
    }

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    // Scope for widget repaint coroutines; cancelled in onDestroy
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val container by lazy {
        (application as BluePlayerApplication).container
    }

    private val sessionListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publishAudioSessionId()
            WidgetUpdater.updateFromService(this@MusicPlaybackService, container, player)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            saveCurrentSession()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
                reason != Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
            ) {
                saveCurrentSession()
            }
            WidgetUpdater.updateFromService(this@MusicPlaybackService, container, player)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            WidgetUpdater.updateFromService(this@MusicPlaybackService, container, player)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            mediaSession?.setCustomLayout(buildRepeatLayout())
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val bassProcessor = NativeBassProcessor()

        val audioSink = DefaultAudioSink.Builder(this)
            .setAudioProcessors(arrayOf(bassProcessor))
            .build()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink = audioSink
        }

        val exoPlayer = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer.volume = 1.0f
        player = exoPlayer

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionPendingIntent = PendingIntent.getActivity(
            this, 0, sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionPendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = super.onConnect(session, controller)
                        .availableSessionCommands
                        .buildUpon()
                        .add(SessionCommand(REPEAT_COMMAND, Bundle.EMPTY))
                        .build()

                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction == REPEAT_COMMAND) {
                        player?.let { p ->
                            p.repeatMode = when (p.repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                        }
                    }
                    return Futures.immediateFuture(
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    )
                }
            })
            .build()

        exoPlayer.addListener(sessionListener)
        mediaSession?.setCustomLayout(buildRepeatLayout())
        publishAudioSessionId()

        // Repaint the widget whenever an online cover arrives for the playing track
        serviceScope.launch {
            var lastCover: String? = null
            container.coverCache.covers.collect { covers ->
                val mediaId = player?.currentMediaItem?.mediaId
                val cover = mediaId?.let { covers[it] as? String }
                if (cover != null && cover != lastCover) {
                    lastCover = cover
                    WidgetUpdater.updateFromService(this@MusicPlaybackService, container, player)
                }
            }
        }

        // Slow ticker: keeps the widget progress bar alive while playing
        serviceScope.launch {
            while (true) {
                delay(10_000)
                val p = player
                if (p != null && p.isPlaying) {
                    WidgetUpdater.updateProgressOnly(this@MusicPlaybackService, container, p)
                }
            }
        }

        if (exoPlayer.mediaItemCount == 0) {
            restoreLastSession(exoPlayer)
        }
    }

    private fun buildRepeatLayout(): List<CommandButton> {
        val iconRes = when (player?.repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
            Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat
            else -> R.drawable.ic_repeat_off
        }
        return listOf(
            CommandButton.Builder()
                .setDisplayName("Repeat")
                .setIconResId(iconRes)
                .setSessionCommand(SessionCommand(REPEAT_COMMAND, Bundle.EMPTY))
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun saveCurrentSession() {
        val p = player ?: return
        try {
            val item = p.currentMediaItem ?: return
            val uri = item.localConfiguration?.uri?.toString() ?: return
            val meta = item.mediaMetadata

            getSharedPreferences(PREFS_LAST_SESSION, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_URI, uri)
                .putLong(KEY_POSITION, p.currentPosition)
                .putBoolean(KEY_PLAY_WHEN_READY, p.playWhenReady)
                .putString(KEY_TITLE, meta.title?.toString())
                .putString(KEY_ARTIST, meta.artist?.toString())
                .putString(KEY_ALBUM, meta.albumTitle?.toString())
                .putString(KEY_ARTWORK, meta.artworkUri?.toString())
                .apply()
        } catch (_: Exception) {
        }
    }

    private fun restoreLastSession(p: Player): Boolean {
        return try {
            val prefs = getSharedPreferences(PREFS_LAST_SESSION, Context.MODE_PRIVATE)
            val uri = prefs.getString(KEY_URI, null) ?: return false
            val position = prefs.getLong(KEY_POSITION, 0L)
            val title = prefs.getString(KEY_TITLE, null)
            val artist = prefs.getString(KEY_ARTIST, null)
            val album = prefs.getString(KEY_ALBUM, null)
            val artwork = prefs.getString(KEY_ARTWORK, null)

            val metadata = MediaMetadata.Builder()
                .setTitle(title ?: "")
                .setArtist(artist ?: "")
                .setAlbumTitle(album ?: "")
                .apply {
                    if (!artwork.isNullOrEmpty()) {
                        setArtworkUri(Uri.parse(artwork))
                    }
                }
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(uri))
                .setMediaMetadata(metadata)
                .build()

            p.setMediaItem(mediaItem)
            p.seekTo(position)
            p.prepare()
            p.playWhenReady = false
            true
        } catch (_: Exception) {
            false
        }
    }

    @OptIn(UnstableApi::class)
    private fun publishAudioSessionId() {
        val p = player ?: return
        val session = mediaSession ?: return
        val sessionId = p.audioSessionId
        if (sessionId != 0) {
            session.setSessionExtras(
                Bundle().apply { putInt(KEY_AUDIO_SESSION_ID, sessionId) }
            )
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player ?: return
        saveCurrentSession()
        if (!p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveCurrentSession()
        serviceScope.cancel()
        WidgetUpdater.showEmpty(this)
        player?.let { p ->
            p.removeListener(sessionListener)
            p.release()
        }
        player = null

        mediaSession?.release()
        mediaSession = null

        super.onDestroy()
    }
}