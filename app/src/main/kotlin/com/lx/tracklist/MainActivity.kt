package com.lx.tracklist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.lx.tracklist.ui.navigation.AppNavigation
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXTrackListTheme

import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.lx.tracklist.data.remote.mal.MalAuthManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleMalAuthIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            LXTrackListTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LXBackgroundBlack
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMalAuthIntent(intent)
    }

    private fun handleMalAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "lxtracklist" && uri.host == "mal-oauth") {
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                lifecycleScope.launch {
                    val result = MalAuthManager.handleAuthCallback(this@MainActivity, code)
                    result.onSuccess { username ->
                        Toast.makeText(
                            this@MainActivity,
                            "Connected to MyAnimeList as $username!",
                            Toast.LENGTH_LONG
                        ).show()
                    }.onFailure { err ->
                        Toast.makeText(
                            this@MainActivity,
                            "MyAnimeList connection failed: ${err.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}
