package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactInfoScreen(
    contactName: String,
    onBack: () -> Unit,
    onStartCall: (String) -> Unit = {},
    onStartVideoCall: (String) -> Unit = {}
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var disappearingTimer by remember { mutableStateOf("7 Days") }
    var showSafetyNumberDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* more options */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OledBlack
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Big Contact Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E3A5F))
                            .border(2.dp, BorderHairline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contactName.take(1).uppercase(),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = contactName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Online • NIST ML-KEM-768 Verified",
                        fontSize = 13.sp,
                        color = AccentGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Actions: Voice, Video, Chat
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Call,
                        label = "Voice",
                        onClick = { onStartCall(contactName) }
                    )
                    QuickActionButton(
                        icon = Icons.Default.Videocam,
                        label = "Video",
                        onClick = { onStartVideoCall(contactName) }
                    )
                    QuickActionButton(
                        icon = Icons.Default.ChatBubble,
                        label = "Chat",
                        onClick = onBack
                    )
                }
            }

            // Section: Cryptographic Verification
            item {
                Text(
                    text = "SECURITY & ENCRYPTION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                ) {
                    ContactSettingRow(
                        icon = Icons.Default.Lock,
                        title = "Encryption",
                        subtitle = "Messages and calls are end-to-end encrypted with PQXDH & Kyber-768. Tap to verify.",
                        onClick = { showSafetyNumberDialog = true }
                    )
                    Divider(color = BorderHairline, thickness = 0.5.dp)
                    ContactSettingRow(
                        icon = Icons.Default.QrCode,
                        title = "Verify Safety Number",
                        subtitle = "Scan QR code or compare 60-digit fingerprint",
                        onClick = { showSafetyNumberDialog = true }
                    )
                }
            }

            // Section: Privacy & Timers
            item {
                Text(
                    text = "CHAT SETTINGS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notifications",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentWhite,
                                checkedTrackColor = AccentGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }

                    Divider(color = BorderHairline, thickness = 0.5.dp)

                    ContactSettingRow(
                        icon = Icons.Default.Timer,
                        title = "Disappearing Messages",
                        subtitle = disappearingTimer,
                        onClick = {
                            disappearingTimer = when (disappearingTimer) {
                                "24 Hours" -> "7 Days"
                                "7 Days" -> "90 Days"
                                "90 Days" -> "Off"
                                else -> "24 Hours"
                            }
                        }
                    )

                    Divider(color = BorderHairline, thickness = 0.5.dp)

                    ContactSettingRow(
                        icon = Icons.Default.PermMedia,
                        title = "Media, Links and Docs",
                        subtitle = "18 files shared",
                        onClick = { /* Media browser */ }
                    )
                }
            }

            // Section: Danger Zone (Block / Clear)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Block */ }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Block $contactName",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentRed
                        )
                    }

                    Divider(color = BorderHairline, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Report */ }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Report,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Report $contactName",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Safety Number Comparison Dialog
    if (showSafetyNumberDialog) {
        AlertDialog(
            onDismissRequest = { showSafetyNumberDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Verify Safety Number",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "To verify that messages and calls with $contactName are end-to-end encrypted, compare the number below with their device.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(OledBlack)
                            .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "28401 98402 11093 48201\n55829 44810 39201 84920\n10394 59201 84920 18392",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = AccentWhite,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSafetyNumberDialog = false }) {
                    Text("Done", color = AccentGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(1.dp, BorderHairline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

@Composable
private fun ContactSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
