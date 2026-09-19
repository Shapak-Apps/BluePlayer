package com.blueplayer.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.blueplayer.app.BluePlayerApplication
import com.blueplayer.core.player.Media3PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Home-screen widget receiver.
 *
 * Button clicks arrive as explicit broadcasts (PREV / TOGGLE / NEXT).
 * The provider connects to the playback service if needed, performs
 * the action and refreshes the widget UI.
 */
class BluePlayerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.blueplayer.app.widget.TOGGLE"
        const val ACTION_NEXT = "com.blueplayer.app.widget.NEXT"
        const val ACTION_PREV = "com.blueplayer.app.widget.PREV"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val container = (context.applicationContext as? BluePlayerApplication)?.container
            ?: return
        WidgetUpdater.updateFromState(context, container)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE, ACTION_NEXT, ACTION_PREV -> handleCommand(context, intent.action)
        }
    }

    private fun handleCommand(context: Context, action: String?) {
        val container = (context.applicationContext as? BluePlayerApplication)?.container
            ?: return
        val controller = container.playerController as? Media3PlayerController
            ?: return

        CoroutineScope(Dispatchers.Main).launch {
            // Ensure the MediaController is bound even after a cold start
            controller.connectAndEnsure()
            when (action) {
                ACTION_TOGGLE -> controller.togglePlayPause()
                ACTION_NEXT -> controller.next()
                ACTION_PREV -> controller.previous()
            }
            // Give the player a moment to change state, then repaint
            delay(400)
            WidgetUpdater.updateFromState(context, container)
        }
    }
}