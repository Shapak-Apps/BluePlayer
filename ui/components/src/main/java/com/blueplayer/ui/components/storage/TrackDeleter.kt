package com.blueplayer.ui.components.storage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.result.IntentSenderRequest
import androidx.documentfile.provider.DocumentFile
import com.blueplayer.core.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Single entry point for deleting audio files on every Android version.
 *
 * Owns the whole permission state machine (MediaStore delete request,
 * RecoverableSecurityException, WRITE_EXTERNAL_STORAGE, all-files access)
 * so screens only call [requestDelete] and receive results via [Callbacks].
 */
class TrackDeleter(private val context: Context) {

    interface Callbacks {
        fun onDeleted(track: Track)
        fun onFailed(track: Track)
        fun onDenied(track: Track)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var callbacks: Callbacks? = null

    // Wired once per composition by rememberTrackDeleterLaunchers()
    internal lateinit var launchIntentSender: (IntentSenderRequest) -> Unit
    internal lateinit var launchPermission: (String) -> Unit
    internal lateinit var launchAllFiles: (Intent) -> Unit

    private var pendingUri: Uri? = null
    private var pendingTrack: Track? = null

    private val _showBanner = MutableStateFlow(false)
    val showBanner: StateFlow<Boolean> = _showBanner.asStateFlow()
    private var bannerDismissed = false

    fun bind(callbacks: Callbacks) {
        this.callbacks = callbacks
    }

    fun isAllFilesAccessGranted(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Environment.isExternalStorageManager()

    fun needsAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isAllFilesAccessGranted()

    fun refreshBanner() {
        _showBanner.value = needsAllFilesAccess() && !bannerDismissed
    }

    fun dismissBanner() {
        bannerDismissed = true
        _showBanner.value = false
    }

    /** Opens the system "All files access" settings screen (no result needed). */
    fun openAllFilesSettings() {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    // ---------------------------------------------------------------------
    // Public delete entry point
    // ---------------------------------------------------------------------

    fun requestDelete(track: Track) {
        val originalUri = runCatching { Uri.parse(track.uri) }.getOrNull()
        if (originalUri == null) {
            callbacks?.onFailed(track)
            return
        }

        // Plain file URIs never need MediaStore consent
        if (originalUri.scheme == "file") {
            scope.launch {
                val deleted = performDelete(originalUri)
                if (deleted) callbacks?.onDeleted(track)
                else if (needsAllFilesAccess()) {
                    pendingUri = originalUri
                    pendingTrack = track
                    launchAllFiles(allFilesIntent())
                } else callbacks?.onFailed(track)
            }
            return
        }

        // Normalize non-MediaStore content URIs on Android 10+
        val mediaUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            originalUri.scheme == "content" &&
            originalUri.authority != MediaStore.AUTHORITY
        ) {
            runCatching { MediaStore.getMediaUri(context, originalUri) }.getOrNull()
                ?: originalUri
        } else {
            originalUri
        }

        when {
            // Android 11+: silent delete when all-files granted, else system dialog
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (isAllFilesAccessGranted()) {
                    scope.launch {
                        val deleted = performDelete(mediaUri)
                        if (deleted) callbacks?.onDeleted(track)
                        else launchMediaStoreDeleteRequest(mediaUri, track)
                    }
                } else {
                    launchMediaStoreDeleteRequest(mediaUri, track)
                }
            }

            // Android 10: scoped storage with recoverable exceptions
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                scope.launch {
                    try {
                        val deleted = withContext(Dispatchers.IO) {
                            context.contentResolver.delete(mediaUri, null, null) > 0
                        }
                        if (deleted) {
                            callbacks?.onDeleted(track)
                        } else {
                            val docDeleted = withContext(Dispatchers.IO) {
                                runCatching {
                                    DocumentFile.fromSingleUri(context, mediaUri)?.delete() == true
                                }.getOrDefault(false)
                            }
                            if (docDeleted) callbacks?.onDeleted(track)
                            else callbacks?.onFailed(track)
                        }
                    } catch (e: android.app.RecoverableSecurityException) {
                        pendingUri = mediaUri
                        pendingTrack = track
                        // userAction / actionIntent exist only from API 26/29,
                        // so access them behind an explicit SDK guard
                        val sender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            runCatching {
                                IntentSenderRequest.Builder(
                                    e.userAction.actionIntent.intentSender
                                ).build()
                            }.getOrNull()
                        } else {
                            null
                        }
                        if (sender != null) {
                            runCatching { launchIntentSender(sender) }.onFailure {
                                pendingUri = null
                                pendingTrack = null
                                callbacks?.onFailed(track)
                            }
                        } else {
                            pendingUri = null
                            pendingTrack = null
                            callbacks?.onFailed(track)
                        }
                    } catch (e: SecurityException) {
                        val docDeleted = withContext(Dispatchers.IO) {
                            runCatching {
                                DocumentFile.fromSingleUri(context, mediaUri)?.delete() == true
                            }.getOrDefault(false)
                        }
                        if (docDeleted) callbacks?.onDeleted(track)
                        else callbacks?.onDenied(track)
                    } catch (e: Exception) {
                        callbacks?.onFailed(track)
                    }
                }
            }

            // Android 9 and below: classic storage permission
            else -> {
                val granted = runCatching {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }.getOrDefault(false)

                if (granted) {
                    scope.launch {
                        val deleted = performDelete(originalUri)
                        if (deleted) callbacks?.onDeleted(track)
                        else callbacks?.onFailed(track)
                    }
                } else {
                    pendingUri = originalUri
                    pendingTrack = track
                    launchPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Launcher result handlers (called by rememberTrackDeleterLaunchers)
    // ---------------------------------------------------------------------

    fun handleIntentSenderResult(resultCode: Int) {
        val uri = pendingUri
        val track = pendingTrack
        pendingUri = null
        pendingTrack = null
        if (uri == null || track == null) return

        if (resultCode == Activity.RESULT_OK) {
            scope.launch {
                val deleted = withContext(Dispatchers.IO) {
                    var stillExists = runCatching {
                        context.contentResolver.query(
                            uri, arrayOf(MediaStore.MediaColumns._ID),
                            null, null, null
                        )?.use { it.moveToFirst() } ?: false
                    }.getOrDefault(false)

                    if (stillExists) stillExists = !performDelete(uri)
                    if (stillExists) {
                        val path = if (uri.scheme == "file") uri.path else getRealPath(uri)
                        stillExists = path != null && File(path).exists()
                    }
                    !stillExists
                }
                if (deleted) callbacks?.onDeleted(track) else callbacks?.onFailed(track)
            }
        } else {
            callbacks?.onDenied(track)
        }
    }

    fun handlePermissionResult(granted: Boolean) {
        val uri = pendingUri
        val track = pendingTrack
        pendingUri = null
        pendingTrack = null
        if (uri == null || track == null) return

        if (!granted) {
            callbacks?.onDenied(track)
            return
        }
        scope.launch {
            val deleted = performDelete(uri)
            if (deleted) callbacks?.onDeleted(track) else callbacks?.onFailed(track)
        }
    }

    fun handleAllFilesResult() {
        val uri = pendingUri
        val track = pendingTrack
        pendingUri = null
        pendingTrack = null
        if (uri == null || track == null) return

        if (isAllFilesAccessGranted()) {
            scope.launch {
                val deleted = performDelete(uri)
                if (deleted) callbacks?.onDeleted(track) else callbacks?.onFailed(track)
            }
        } else {
            callbacks?.onDenied(track)
        }
        refreshBanner()
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private fun allFilesIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )

    private fun launchMediaStoreDeleteRequest(uri: Uri, track: Track) {
        pendingUri = uri
        pendingTrack = track

        val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
            }.getOrNull()
        } else {
            null
        }

        if (request == null) {
            pendingUri = null
            pendingTrack = null
            scope.launch {
                val deleted = performDelete(uri)
                if (deleted) callbacks?.onDeleted(track)
                else if (needsAllFilesAccess()) {
                    pendingUri = uri
                    pendingTrack = track
                    launchAllFiles(allFilesIntent())
                } else callbacks?.onFailed(track)
            }
            return
        }

        runCatching { launchIntentSender(IntentSenderRequest.Builder(request.intentSender).build()) }
            .onFailure {
                pendingUri = null
                pendingTrack = null
                scope.launch {
                    val deleted = performDelete(uri)
                    if (deleted) callbacks?.onDeleted(track) else callbacks?.onFailed(track)
                }
            }
    }

    private suspend fun performDelete(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val crDeleted = runCatching {
            context.contentResolver.delete(uri, null, null) > 0
        }.getOrDefault(false)
        if (crDeleted) return@withContext true

        val docDeleted = runCatching {
            DocumentFile.fromSingleUri(context, uri)?.delete() == true
        }.getOrDefault(false)
        if (docDeleted) return@withContext true

        val path = if (uri.scheme == "file") uri.path else getRealPath(uri)
        if (path != null) {
            val file = File(path)
            if (file.exists() && file.delete()) return@withContext true
        }
        false
    }

    private fun getRealPath(uri: Uri): String? {
        return try {
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val column = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                if (cursor.moveToFirst()) cursor.getString(column) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}