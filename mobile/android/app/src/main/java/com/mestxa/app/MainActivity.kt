package com.mestxa.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mestxa.app.storage.VaultManager
import com.mestxa.app.ui.navigation.MestxaNavGraph
import com.mestxa.app.ui.navigation.Screen
import com.mestxa.app.ui.theme.MestxaTheme
import com.mestxa.app.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Audio and Camera permissions handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce anti-screen capture / task switcher leak prevention
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request audio and camera runtime permissions for WebRTC calling
        requestPermissionsLauncher.launch(
            arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CAMERA
            )
        )

        val isRegistered = VaultManager.isRegistered(this)
        val startDestination = if (isRegistered) {
            Screen.Chats.route
        } else {
            Screen.Welcome.route
        }

        setContent {
            MestxaTheme {
                val theme = ThemeManager.colors
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.bg),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                    ) {
                        MestxaNavGraph(startDestination = startDestination)
                    }
                }
            }
        }
    }
}
