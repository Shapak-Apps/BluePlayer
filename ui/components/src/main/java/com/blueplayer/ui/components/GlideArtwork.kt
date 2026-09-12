package com.blueplayer.ui.components

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

@Composable
fun GlideArtwork(
    model: Any?,
    modifier: Modifier = Modifier,
    onError: () -> Unit
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        modifier = modifier,
        update = { view ->
            Glide.with(context)
                .load(model)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(view)
        }
    )
}