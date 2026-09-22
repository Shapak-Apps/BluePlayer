package com.blueplayer.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.core.domain.model.Track
import com.blueplayer.ui.components.storage.TrackDeleter

/**
 * Registers the three ActivityResult launchers TrackDeleter needs and
 * binds them to the instance. Call once per screen that can delete tracks.
 */
@Composable
fun rememberTrackDeleterLaunchers(deleter: TrackDeleter) {
    val intentSenderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> deleter.handleIntentSenderResult(result.resultCode) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> deleter.handlePermissionResult(granted) }

    val allFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { deleter.handleAllFilesResult() }

    LaunchedEffect(deleter) {
        deleter.launchIntentSender = { intentSenderLauncher.launch(it) }
        deleter.launchPermission = { permissionLauncher.launch(it) }
        deleter.launchAllFiles = { allFilesLauncher.launch(it) }
    }
}

/**
 * Dismissible banner shown while the app lacks "All files access".
 * Granting removes future consent dialogs for deletions.
 */
@Composable
fun StorageAccessBanner(
    visible: Boolean,
    lang: AppLanguage,
    onGrant: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = visible) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.FolderOpen, null)
                Spacer(Modifier.width(12.dp))
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    Text(
                        Strings.storageAccessTitle(lang),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        Strings.storageAccessText(lang),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = onGrant) {
                    Text(Strings.storageAccessGrant(lang))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, null)
                }
            }
        }
    }
}

/**
 * Reusable "delete this track?" confirmation dialog.
 * Pass null track to hide it.
 */
@Composable
fun DeleteTrackConfirmDialog(
    track: Track?,
    lang: AppLanguage,
    onConfirm: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    if (track != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(Strings.deleteTrackTitle(lang)) },
            text = { Text(Strings.deleteTrackText(lang, track.title)) },
            confirmButton = {
                TextButton(onClick = { onConfirm(track) }) {
                    Text(Strings.delete(lang), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(Strings.cancel(lang)) }
            }
        )
    }
}