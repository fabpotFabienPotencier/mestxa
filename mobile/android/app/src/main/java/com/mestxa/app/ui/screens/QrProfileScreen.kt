package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
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
fun QrProfileScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = OledBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Contact QR Code",
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Main QR Badge Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(AccentWhite)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar Overlap
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Alex Rivera",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OledBlack
                        )

                        Text(
                            text = "@alex_r",
                            fontSize = 14.sp,
                            color = Color(0xFF555555)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // High Contrast QR Vector
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF0F0F0))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Code",
                                tint = OledBlack,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Your QR code is private. If you share it with others, they can scan it with their camera to add you as a contact.",
                            fontSize = 11.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Bottom Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentWhite,
                        contentColor = OledBlack
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Share QR Code", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BorderHairline)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Scan QR Code", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
