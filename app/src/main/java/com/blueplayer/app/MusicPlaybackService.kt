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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.blueplayer.app.audio.NativeBassProcessor

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
    }

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    private val sessionListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publishAudioSessionId()
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
            .build()

        exoPlayer.addListener(sessionListener)
        publishAudioSessionId()

        if (exoPlayer.mediaItemCount == 0) {
            restoreLastSession(exoPlayer)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
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