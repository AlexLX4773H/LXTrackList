package com.lx.tracklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lx.tracklist.ui.navigation.AppNavigation
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXTrackListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
}
