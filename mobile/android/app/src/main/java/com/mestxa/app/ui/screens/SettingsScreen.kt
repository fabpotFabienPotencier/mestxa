package com.mestxa.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.storage.DatabaseManager
import com.mestxa.app.storage.VaultManager
import com.mestxa.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenQr: () -> Unit = {},
    onOpenTheme: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { DatabaseManager.getInstance(context) }

    val displayName = remember { VaultManager.getDisplayName(context) }
    val userHandle = remember { VaultManager.getUserHandle(context) }
    val userNumber = remember { VaultManager.getUserNumber(context) }
    val userAbout = remember { VaultManager.getUserAbout(context) }

    var biometricLockEnabled by remember { mutableStateOf(true) }
    var screenSecurityEnabled by remember { mutableStateOf(true) }
    var studioAudioEnabled by remember { mutableStateOf(true) }
    var readReceiptsEnabled by remember { mutableStateOf(true) }
    var disappearingDays by remember { mutableStateOf("7 Days") }
    var showResetDialog by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Real User Profile Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(2).uppercase().ifBlank { userHandle.take(2).uppercase() },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "@$userHandle",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.clickable { copyToClipboard("Username", "@$userHandle") }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { copyToClipboard("MX Number", userNumber) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = userNumber.ifBlank { "MX Number" },
                                fontSize = 12.sp,
                                color = AccentGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(onClick = onOpenQr) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "My QR Code",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Post-Quantum Security Shield Banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F1E13))
                        .border(1.dp, Color(0xFF1B4D28), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security",
                        tint = AccentGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Post-Quantum Cryptography Active",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentGreen
                        )
                        Text(
                            text = "Kyber-768 ML-KEM + Double Ratchet forward secrecy",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Category: Appearance & Theme
            item {
                SettingsSectionHeader(title = "Appearance & Theme")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                ) {
                    SettingsClickableItem(
                        icon = Icons.Default.Palette,
                        title = "App Theme & Accent Colors",
                        value = "22 Presets",
                        onClick = onOpenTheme
                    )
                    Divider(color = BorderHairline, thickness = 0.5.dp)
                    SettingsClickableItem(
                        icon = Icons.Default.Wallpaper,
                        title = "Chat Wallpaper",
                        value = "OLED Pure Black",
                        onClick = {}
                    )
                }
            }

            // Category: Privacy & Security
            item {
                SettingsSectionHeader(title = "Privacy & Security")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                ) {
                    SettingsToggleItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Vault Lock",
                        subtitle = "Require fingerprint / face to unlock on app launch",
                        checked = biometricLockEnabled,
                        onCheckedChange = { biometricLockEnabled = it }
                    )
                    Divider(color = BorderHairline, thickness = 0.5.dp)
                    SettingsToggleItem(
                        icon = Icons.Default.Security,
                        title = "Screen Security (FLAG_SECURE)",
                        subtitle = "Block screenshots and task-switcher previews",
                        checked = screenSecurityEnabled,
                        onCheckedChange = { screenSecurityEnabled = it }
                    )
                    Divider(color = BorderHairline, thickness = 0.5.dp)
                    SettingsClickableItem(
                        icon = Icons.Default.Timer,
                        title = "Disappearing Messages",
                        value = disappearingDays,
                        onClick = {
                            disappearingDays = when (disappearingDays) {
                                "24 Hours" -> "7 Days"
                                "7 Days" -> "90 Days"
                                "90 Days" -> "Off"
                                else -> "24 Hours"
                            }
                        }
                    )
                    Divider(color = BorderHairline, thickness = 0.5.dp)
                    SettingsToggleItem(
                        icon = Icons.Default.DoneAll,
                        title = "Read Receipts",
                        subtitle = "Allow contacts to see when you've opened messages",
                        checked = readReceiptsEnabled,
                        onCheckedChange = { readReceiptsEnabled = it }
                    )
                }
            }

            // Category: Audio & Calls
            item {
                SettingsSectionHeader(title = "Audio & Calling")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                ) {
                    SettingsToggleItem(
                        icon = Icons.Default.GraphicEq,
                        title = "Studio 48kHz Audio (Opus Full-Band)",
                        subtitle = "Crystal pure high-fidelity calling with zero server relay lag",
                        checked = studioAudioEnabled,
                        onCheckedChange = { studioAudioEnabled = it }
                    )
                    Divider(color = BorderHairline, thickness = 0.5.dp)
                    SettingsClickableItem(
                        icon = Icons.Default.Lock,
                        title = "SFrame End-to-End Encryption",
                        value = "RFC 9605 Verified",
                        onClick = {}
                    )
                }
            }

            // Category: Zero-Cloud Vault & Identity
            item {
                SettingsSectionHeader(title = "Relay & Cryptographic Keys")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                ) {
                    SettingsClickableItem(
                        icon = Icons.Default.Dns,
                        title = "Relay Server",
                        value = "api.mestxa.com",
                        onClick = {}
                    )
                    Divider(color = BorderHairline, thickness = 0.5.dp)
                    SettingsClickableItem(
                        icon = Icons.Default.VpnKey,
                        title = "Identity Key Hex",
                        value = "Ed25519 Verified",
                        onClick = {
                            val ik = VaultManager.getIdentityPublicKey(context)
                            val hex = com.mestxa.app.engine.MestxaBridge.bytesToHex(ik)
                            copyToClipboard("Identity Key", hex)
                        }
                    )
                    Divider(color = BorderHairline, thickness = 0.5.dp)
                    SettingsClickableItem(
                        icon = Icons.Default.Devices,
                        title = "Linked Devices",
                        value = "Companion QR Ready",
                        onClick = {}
                    )
                }
            }

            // Category: Account & Reset
            item {
                SettingsSectionHeader(title = "Account Management")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                ) {
                    SettingsClickableItem(
                        icon = Icons.Default.Logout,
                        title = "Reset Account & Clear Data",
                        value = "Purge local keys",
                        onClick = { showResetDialog = true }
                    )
                }
            }

            // Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Mestxa v1.0.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Zero Cloud Storage • Zero Metadata • Pure Native",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Account?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will erase your local cryptographic identity keys, clear message history, and unbind your MX number from this device.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        VaultManager.clearAccount(context)
                        // Clear database
                        try {
                            db.writableDatabase.execSQL("DELETE FROM messages")
                            db.writableDatabase.execSQL("DELETE FROM conversations")
                            db.writableDatabase.execSQL("DELETE FROM contacts")
                            db.writableDatabase.execSQL("DELETE FROM calls")
                        } catch (_: Exception) {}
                        onSignOut()
                    }
                ) {
                    Text("Reset", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentWhite,
                checkedTrackColor = AccentGreen,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = SurfaceElevated
            )
        )
    }
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}
