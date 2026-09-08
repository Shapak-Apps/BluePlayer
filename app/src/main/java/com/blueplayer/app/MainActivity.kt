package com.blueplayer.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.blueplayer.app.ui.BluePlayerRoot
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