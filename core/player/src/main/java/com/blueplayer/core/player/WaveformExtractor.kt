package com.blueplayer.core.player

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object WaveformExtractor {

    init {
        System.loadLibrary("waveformextractor")
    }

    private external fun extractNative(
        fd: Int,
        offset: Long,
        length: Long,
        durationUs: Long
    ): FloatArray

    private val cache = ConcurrentHashMap<String, FloatArray>()

    suspend fun extract(context: Context, uri: Uri): FloatArray = withContext(Dispatchers.IO) {
        val key = uri.toString()
        cache[key]?.let { return@withContext it }

        val result = try {
            val durationUs = getDurationUs(context, uri)
            if (durationUs <= 0) {
                FloatArray(0)
            } else {
                val fd = context.contentResolver.openFileDescriptor(uri, "r")
                if (fd == null) {
                    FloatArray(0)
                } else {
                    fd.use { descriptor ->
                        extractNative(
                            descriptor.fd,
                            0L,
                            descriptor.statSize,
                            durationUs
                        )
                    }
                }
            }
        } catch (e: Exception) {
            FloatArray(0)
        } catch (e: UnsatisfiedLinkError) {
            FloatArray(0)
        }

        if (result.isNotEmpty()) cache[key] = result
        result
    }

    private fun getDurationUs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.times(1000) ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun clearCache() = cache.clear()
}