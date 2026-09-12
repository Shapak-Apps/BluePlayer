package com.blueplayer.core.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CoverCache {
    private val _covers = MutableStateFlow<Map<String, Any>>(emptyMap())
    val covers: StateFlow<Map<String, Any>> = _covers.asStateFlow()

    fun put(trackId: String, model: Any) {
        _covers.update { it + (trackId to model) }
    }

    fun remove(trackId: String) {
        _covers.update { it - trackId }
    }

    fun get(trackId: String): Any? = _covers.value[trackId]

    fun clear() {
        _covers.value = emptyMap()
    }
}