package com.blueplayer.ui.components

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest

/**
 * Prefetches artwork bitmaps into Coil's memory cache before the
 * user scrolls to them. This eliminates decode-on-demand jank on
 * slow devices when list items appear on screen.
 *
 * Usage:
 * ```
 * LaunchedEffect(listState.firstVisibleItemIndex) {
 *     val window = (firstVisible until firstVisible + WINDOW)
 *     window.forEach { idx ->
 *         ListPreloader.preload(context, items[idx].artworkUri, 96.dp.toPx)
 *     }
 * }
 * ```
 */
object ListPreloader {

    /**
     * Enqueues an image load for the given URL at the given pixel size.
     * No-op for blank URLs; Coil silently skips items already in cache.
     *
     * @param context Application or Activity context (used to resolve the
     *   singleton [coil.ImageLoader]).
     * @param url Artwork URL (http / content / file). Blank strings are ignored.
     * @param sizePx Target pixel dimension (width and height).
     *   Should match the on-screen size so Coil caches the exact bitmap
     *   the ImageView will request, avoiding a later re-request.
     */
    fun preload(context: Context, url: String?, sizePx: Int) {
        if (url.isNullOrBlank()) return
        if (sizePx <= 0) return

        val request = ImageRequest.Builder(context)
            .data(url)
            .size(sizePx, sizePx)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .build()

        // enqueue() returns immediately; the decode happens on a background
        // dispatcher inside Coil. No await, no blocking.
        runCatching {
            context.imageLoader.enqueue(request)
        }
    }
}