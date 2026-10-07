package com.mestxa.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mestxa.app.ui.theme.*
import com.mestxa.app.ui.viewmodels.CallViewModel

@Composable
fun ActiveCallScreen(
    contactName: String,
    onEndCall: () -> Unit,
    viewModel: CallViewModel = viewModel()
) {
    val callSeconds by viewModel.callSeconds.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val waveformBars by viewModel.waveformBars.collectAsState()

    LaunchedEffect(contactName) {
        viewModel.startCall(contactName)
    }

    val minutes = String.format("%02d", callSeconds / 60)
    val seconds = String.format("%02d", callSeconds % 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080808))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Contact Profile & Timer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .background(SurfaceDark, CircleShape)
                    .border(2.dp, AccentWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contactName.take(1),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentWhite
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = contactName,
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "48 kHz Full-Band Opus • SFrame E2EE",
                style = MaterialTheme.typography.labelSmall,
                color = AccentGreen,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "$minutes:$seconds",
                style = MaterialTheme.typography.headlineSmall,
                color = TextSecondary,
                fontSize = 18.sp
            )
        }

        // Live Acoustic Voice Spectrum Visualizer (Driven by 48 kHz WebRTC stream)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(48.dp)
        ) {
            waveformBars.forEach { height ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(height.coerceIn(8f, 48f).dp)
                        .background(AccentWhite, CircleShape)
                )
            }
        }

        // In-Call Action Buttons (Mute, Red End Call, Speaker)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute Button
            IconButton(
                onClick = { viewModel.toggleMute() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isMuted) AccentWhite else SurfaceDark)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute",
                    tint = if (isMuted) OledBlack else AccentWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Red End Call Button
            IconButton(
                onClick = { viewModel.endCall(onEndCall) },
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(DestructiveRed)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = AccentWhite,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Speaker Button
            IconButton(
                onClick = { viewModel.toggleSpeaker() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isSpeakerOn) SurfaceElevated else SurfaceDark)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Speaker",
                    tint = AccentWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
