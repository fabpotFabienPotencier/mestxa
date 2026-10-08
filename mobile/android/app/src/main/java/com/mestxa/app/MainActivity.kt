package com.mestxa.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mestxa.app.ui.navigation.MestxaNavGraph
import com.mestxa.app.ui.theme.MestxaTheme
import com.mestxa.app.ui.theme.OledBlack

import androidx.activity.result.contract.ActivityResultContracts

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

        val isRegistered = com.mestxa.app.storage.VaultManager.isRegistered(this)
        val startDestination = if (isRegistered) {
            com.mestxa.app.ui.navigation.Screen.Chats.route
        } else {
            com.mestxa.app.ui.navigation.Screen.Welcome.route
        }

        setContent {
            MestxaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = OledBlack
                ) {
                    MestxaNavGraph(startDestination = startDestination)
                }
            }
        }
    }
}
