package com.blueplayer.core.domain.repository

import com.blueplayer.core.domain.model.Folder
import com.blueplayer.core.domain.model.Track

interface FolderRepository {
    suspend fun getFolders(): List<Folder>
    suspend fun getTracksForFolder(folderPath: String): List<Track>
}