package com.blueplayer.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.media3.exoplayer.ExoPlayer
import com.blueplayer.app.MainActivity
import com.blueplayer.app.R
import com.blueplayer.app.di.AppContainer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.util.concurrent.atomic.AtomicInteger

/**
 * Builds and pushes RemoteViews for the home-screen widget.
 *
 * Timing design:
 * - Current time is rendered by a system Chronometer (setChronometer),
 *   which ticks every second inside the system server. The app does not
 *   need to wake up each second, so there is no lag and no jumps.
 * - Progress bar and total time refresh on playback events, seeks
 *   (onPositionDiscontinuity) and a slow 10s ticker.
 *
 * Anti-glitch design:
 * - [generation] counter discards stale asynchronous artwork loads.
 * - [bitmapCache] stores rounded artwork per track id for instant repaints.
 */
object WidgetUpdater {

    private const val PROGRESS_MAX = 1000
    private const val CACHE_MAX = 24

    // Monotonic counter: each render invalidates all older async loads
    private val generation = AtomicInteger(0)

    // LRU cache of rounded artwork bitmaps keyed by track / media id
    private val bitmapCache = object : LinkedHashMap<String, Bitmap>(CACHE_MAX, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
            size > CACHE_MAX
    }

    /** Renders widget from the in-memory player state (used by provider). */
    fun updateFromState(context: Context, container: AppContainer) {
        val state = container.playerController.state.value
        val track = state.currentTrack
        val cover = track?.let { container.coverCache.covers.value[it.id] as? String }
        val model: Any? = cover ?: track?.artworkUri?.takeIf { it.isNotBlank() }
        render(
            context = context,
            key = track?.id,
            title = track?.title,
            artist = track?.artist,
            isPlaying = state.isPlaying,
            artworkModel = model,
            positionMs = state.positionMs,
            durationMs = state.durationMs
        )
    }

    /** Renders widget directly from the ExoPlayer instance (used by service). */
    fun updateFromService(context: Context, container: AppContainer, player: ExoPlayer?) {
        val item = player?.currentMediaItem
        val mediaId = item?.mediaId
        val cover = mediaId?.let { container.coverCache.covers.value[it] as? String }
        val model: Any? = cover
            ?: item?.mediaMetadata?.artworkUri?.toString()?.takeIf { it.isNotBlank() }
        render(
            context = context,
            key = mediaId,
            title = item?.mediaMetadata?.title?.toString(),
            artist = item?.mediaMetadata?.artist?.toString(),
            isPlaying = player?.isPlaying == true,
            artworkModel = model,
            positionMs = player?.currentPosition?.takeIf { it >= 0 } ?: 0L,
            durationMs = player?.duration?.takeIf { it > 0 } ?: 0L
        )
    }

    /** Lightweight 10s ticker: refreshes progress bar using cached bitmap. */
    fun updateProgressOnly(context: Context, container: AppContainer, player: ExoPlayer?) {
        val item = player?.currentMediaItem ?: return
        val mediaId = item.mediaId ?: return
        val ids = widgetIds(context) ?: return
        val cached = synchronized(bitmapCache) { bitmapCache[mediaId] }
        val views = buildViews(
            context = context,
            title = item.mediaMetadata.title?.toString(),
            artist = item.mediaMetadata.artist?.toString(),
            isPlaying = player.isPlaying,
            bitmap = cached,
            positionMs = player.currentPosition.takeIf { it >= 0 } ?: 0L,
            durationMs = player.duration.takeIf { it > 0 } ?: 0L
        )
        AppWidgetManager.getInstance(context).updateAppWidget(ids, views)
    }

    /** Shows an empty placeholder when the playback service stops. */
    fun showEmpty(context: Context) {
        generation.incrementAndGet()
        val ids = widgetIds(context) ?: return
        AppWidgetManager.getInstance(context)
            .updateAppWidget(ids, buildViews(context, null, null, false, null, 0L, 0L))
    }

    private fun widgetIds(context: Context): IntArray? {
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, BluePlayerWidgetProvider::class.java))
        return if (ids.isEmpty()) null else ids
    }

    /** Formats milliseconds as m:ss (or h:mm:ss for very long tracks). */
    private fun formatMs(ms: Long): String {
        val totalSeconds = maxOf(0L, ms) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    private fun progressOf(positionMs: Long, durationMs: Long): Int {
        if (durationMs <= 0) return 0
        return ((positionMs.toFloat() / durationMs.toFloat()) * PROGRESS_MAX)
            .toInt()
            .coerceIn(0, PROGRESS_MAX)
    }

    private fun render(
        context: Context,
        key: String?,
        title: String?,
        artist: String?,
        isPlaying: Boolean,
        artworkModel: Any?,
        positionMs: Long,
        durationMs: Long
    ) {
        val ids = widgetIds(context) ?: return
        val manager = AppWidgetManager.getInstance(context)
        val gen = generation.incrementAndGet()

        // Instant pass: reuse cached rounded artwork when available (no flicker)
        val cached = if (key != null) synchronized(bitmapCache) { bitmapCache[key] } else null
        manager.updateAppWidget(
            ids,
            buildViews(context, title, artist, isPlaying, cached, positionMs, durationMs)
        )

        // Async pass only when this track has no cached bitmap yet
        if (cached == null && key != null && artworkModel != null) {
            Glide.with(context.applicationContext)
                .asBitmap()
                .load(artworkModel)
                .into(object : CustomTarget<Bitmap>(256, 256) {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val rounded = roundCenterCrop(context, resource)
                        synchronized(bitmapCache) { bitmapCache[key] = rounded }
                        // Discard stale loads: a previous track must never
                        // overwrite the artwork of the current one
                        if (gen == generation.get()) {
                            manager.updateAppWidget(
                                ids,
                                buildViews(
                                    context, title, artist, isPlaying,
                                    rounded, positionMs, durationMs
                                )
                            )
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                })
        }
    }

    /** Center-crops the source into a square with rounded corners. */
    private fun roundCenterCrop(context: Context, source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val bitmapShader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val scale = size.toFloat() / minOf(source.width, source.height)
        val matrix = Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(
            -(source.width * scale - size) / 2f,
            -(source.height * scale - size) / 2f
        )
        bitmapShader.setLocalMatrix(matrix)

        val paint = Paint().apply {
            isAntiAlias = true
            shader = bitmapShader
        }

        val radius = 18f * context.resources.displayMetrics.density
        canvas.drawRoundRect(
            RectF(0f, 0f, size.toFloat(), size.toFloat()),
            radius, radius, paint
        )
        return output
    }

    private fun buildViews(
        context: Context,
        title: String?,
        artist: String?,
        isPlaying: Boolean,
        bitmap: Bitmap?,
        positionMs: Long,
        durationMs: Long
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_blue_player)

        views.setTextViewText(
            R.id.widget_title,
            title ?: context.getString(R.string.app_name)
        )
        views.setTextViewText(R.id.widget_artist, artist ?: "")

        // Current time: system-driven Chronometer.
        // base = elapsedRealtime() - position, so it shows the real
        // position and ticks every second without any app involvement.
        // When paused (started = false) it freezes at the exact position.
        val chronometerBase = SystemClock.elapsedRealtime() - positionMs
        views.setChronometer(
            R.id.widget_time_current,
            chronometerBase,
            null,
            isPlaying
        )

        // Total duration is static per track
        views.setTextViewText(R.id.widget_time_total, formatMs(durationMs))

        views.setProgressBar(
            R.id.widget_progress,
            PROGRESS_MAX,
            progressOf(positionMs, durationMs),
            false
        )

        views.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )

        if (bitmap != null) {
            views.setImageViewBitmap(R.id.widget_artwork, bitmap)
        } else {
            views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_widget_music)
        }

        // Tapping anywhere on the widget opens the Now Playing screen
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("section", "nowplaying")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        // Previous track button
        val prevIntent = Intent(context, BluePlayerWidgetProvider::class.java)
            .setAction(BluePlayerWidgetProvider.ACTION_PREV)
        views.setOnClickPendingIntent(
            R.id.widget_prev,
            PendingIntent.getBroadcast(
                context, 1, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        // Play / pause button
        val toggleIntent = Intent(context, BluePlayerWidgetProvider::class.java)
            .setAction(BluePlayerWidgetProvider.ACTION_TOGGLE)
        views.setOnClickPendingIntent(
            R.id.widget_play_pause,
            PendingIntent.getBroadcast(
                context, 2, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        // Next track button
        val nextIntent = Intent(context, BluePlayerWidgetProvider::class.java)
            .setAction(BluePlayerWidgetProvider.ACTION_NEXT)
        views.setOnClickPendingIntent(
            R.id.widget_next,
            PendingIntent.getBroadcast(
                context, 3, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        return views
    }
}