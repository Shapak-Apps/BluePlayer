package com.blueplayer.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.blueplayer.app.ui.BluePlayerRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container by lazy {
        (application as BluePlayerApplication).container
    }

    private val _sectionFlow = MutableStateFlow<String?>(null)
    private val sectionFlow: StateFlow<String?> = _sectionFlow.asStateFlow()

    private var lastAutoRescanMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        _sectionFlow.value = extractSection(intent)

        setContent {
            val section by sectionFlow.collectAsState()
            BluePlayerRoot(
                container = container,
                startSection = section
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val section = extractSection(intent)
        if (section != null) {
            _sectionFlow.value = null
            lifecycleScope.launch {
                _sectionFlow.value = section
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            try {
                container.playerController.connect()
            } catch (_: Exception) {
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            autoRescanIfPermissionJustGranted()
        }
    }

    private suspend fun autoRescanIfPermissionJustGranted() {
        if (!hasAudioPermission()) return

        val isEmpty = container.trackRepository.tracks.value.isEmpty()
        if (!isEmpty) return

        val now = System.currentTimeMillis()
        if (now - lastAutoRescanMs < 30_000L) return
        lastAutoRescanMs = now

        try {
            container.trackRepository.rescan()
        } catch (_: Exception) {
        }
    }

    private fun hasAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onStop() {
        lifecycleScope.launch {
            try {
                container.playerController.disconnect()
            } catch (_: Exception) {
            }
        }
        super.onStop()
    }

    private fun extractSection(intent: Intent?): String? {
        return intent?.getStringExtra("section")
    }
}