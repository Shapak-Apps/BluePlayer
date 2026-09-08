package com.blueplayer.core.player

object BassDsp {
    @Volatile
    var gainDb: Float = 0f

    @Volatile
    var enabled: Boolean = true
}