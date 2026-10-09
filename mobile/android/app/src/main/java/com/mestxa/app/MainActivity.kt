package com.mestxa.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.rememberNavController
import com.mestxa.app.engine.MestxaBridge
import com.mestxa.app.storage.VaultManager
import com.mestxa.app.ui.navigation.MestxaNavGraph
import com.mestxa.app.ui.navigation.Screen
import com.mestxa.app.ui.theme.MestxaTheme
import com.mestxa.app.ui.theme.ThemeManager
import com.mestxa.app.webrtc.WebRtcCallManager

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

        val callManager = WebRtcCallManager.getInstance(this)

        setContent {
            MestxaTheme {
                val theme = ThemeManager.colors
                val incomingCall by callManager.incomingCall.collectAsState()
                val navController = rememberNavController()

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
                        MestxaNavGraph(
                            navController = navController,
                            startDestination = startDestination
                        )

                        // Global Incoming Call Alert Banner
                        incomingCall?.let { offer ->
                            val callerHex = MestxaBridge.bytesToHex(offer.callerId).lowercase()
                            val callerDisplay = "MX-" + callerHex.take(8).uppercase()

                            Dialog(onDismissRequest = { /* Require action */ }) {
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = theme.sf),
                                    border = BorderStroke(1.dp, theme.ol),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = callerDisplay,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.tx
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Incoming audio call…",
                                            fontSize = 15.sp,
                                            color = theme.s2
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    callManager.hangup(offer.callId)
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp),
                                                shape = RoundedCornerShape(24.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFEF4444),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Text("Decline", fontWeight = FontWeight.SemiBold)
                                            }
                                            Button(
                                                onClick = {
                                                    callManager.answerCall(offer)
                                                    navController.navigate(Screen.ActiveCall.createRoute(callerDisplay))
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp),
                                                shape = RoundedCornerShape(24.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF22C55E),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Text("Answer", fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
