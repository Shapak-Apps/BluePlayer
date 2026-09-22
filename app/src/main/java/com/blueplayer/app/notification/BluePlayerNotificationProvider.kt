package com.blueplayer.app.notification

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.blueplayer.app.R
import com.google.common.collect.ImmutableList

/**
 * Delegation-based notification provider.
 *
 * Everything (artwork, media style, channels, actions) is still produced
 * by the stock [DefaultMediaNotificationProvider]; this wrapper only:
 *
 * 1. Recomputes the Repeat button icon from the LIVE player state at
 *    notification build time, so the icon can never be stale
 *    (off / all / one -> three distinct drawables).
 * 2. Replaces any stale repeat button coming from the session custom
 *    layout instead of duplicating it.
 * 3. Brands the status-bar small icon and pins a stable channel.
 *
 * Because we delegate, no default behaviour is lost or changed.
 */
@OptIn(UnstableApi::class)
class BluePlayerNotificationProvider(
    private val context: Context
) : MediaNotification.Provider {

    companion object {
        // Must match the command accepted in MusicPlaybackService.onConnect
        const val REPEAT_COMMAND = "com.blueplayer.app.REPEAT"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "blue_player_channel"
    }

    private val delegate: DefaultMediaNotificationProvider =
        DefaultMediaNotificationProvider.Builder(context)
            .setNotificationId(NOTIFICATION_ID)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_notification_music)
            }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val player = mediaSession.player

        // Fresh repeat button computed from the current repeat mode
        val freshRepeat = CommandButton.Builder()
            .setDisplayName("Repeat")
            .setIconResId(iconForRepeatMode(player.repeatMode))
            .setSessionCommand(SessionCommand(REPEAT_COMMAND, Bundle.EMPTY))
            .build()

        // Drop any stale repeat button from the session custom layout,
        // keep every other custom button untouched
        val others = customLayout.filterNot {
            it.sessionCommand?.customAction == REPEAT_COMMAND
        }

        val layout = ImmutableList.builder<CommandButton>()
            .add(freshRepeat)
            .addAll(others)
            .build()

        return delegate.createNotification(
            mediaSession,
            layout,
            actionFactory,
            onNotificationChangedCallback
        )
    }

    // media3 1.4.x signature: (MediaSession, String, Bundle) -> Boolean.
    // We never intercept notification actions ourselves; the session
    // handles REPEAT_COMMAND via onCustomCommand, so return false.
    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean = false

    private fun iconForRepeatMode(mode: Int): Int = when (mode) {
        Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
        Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat
        else -> R.drawable.ic_repeat_off
    }
}