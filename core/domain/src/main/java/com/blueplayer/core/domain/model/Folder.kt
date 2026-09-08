package com.blueplayer.core.domain.model

data class Folder(
    val id: String,
    val name: String,
    val path: String,
    val trackCount: Int
)