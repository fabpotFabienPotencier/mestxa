package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.ui.theme.*

@Composable
fun LinkDeviceScreen(
    onBack: () -> Unit,
    onLinkComplete: () -> Unit
) {
    Scaffold(
        containerColor = OledBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Link Companion",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OledBlack
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Scan this dynamic QR from your primary phone to register a companion Sesame ratchet session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Dynamic QR Display Container
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AccentWhite)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "QR Code",
                        tint = OledBlack,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "End-to-End Encrypted Sync • No Cloud Cache",
                    fontSize = 12.sp,
                    color = AccentGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = onLinkComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentWhite,
                    contentColor = OledBlack
                )
            ) {
                Text(
                    text = "Pair Companion Device",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
