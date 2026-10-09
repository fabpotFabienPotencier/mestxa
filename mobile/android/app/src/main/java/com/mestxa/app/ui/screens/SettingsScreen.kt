package com.mestxa.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.storage.VaultManager
import com.mestxa.app.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenQr: () -> Unit = {},
    onOpenTheme: () -> Unit = {},
    onOpenEditProfile: () -> Unit = {},
    onReplayIntro: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val theme = ThemeManager.colors

    val displayName = remember { VaultManager.getDisplayName(context).ifBlank { "David Caleb" } }
    val userHandle = remember { VaultManager.getUsername(context).ifBlank { "davidcaleb" } }
    val userNumber = remember { VaultManager.getUserNumber(context) }

    var readReceiptsEnabled by remember { mutableStateOf(true) }
    var selectedSubSetting by remember { mutableStateOf<String?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val subSettings = listOf(
        "Account",
        "Privacy",
        "Chats",
        "Notifications",
        "Storage and data",
        "Linked devices",
        "Language",
        "Help"
    )

    Scaffold(
        containerColor = theme.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.tx
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.tx)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenQr) {
                        Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = theme.tx)
                    }
                    IconButton(
                        onClick = onOpenEditProfile,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(theme.sfa)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = theme.acl, modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.bg)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Big Avatar Profile Header (.big in prototype)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(theme.sf)
                            .border(1.dp, theme.ol, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(2).uppercase().ifBlank { userHandle.take(2).uppercase() },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.tx
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = displayName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.tx
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "@$userHandle • ${userNumber.ifBlank { "MX Number" }}",
                        fontSize = 14.sp,
                        color = theme.s2
                    )
                }
            }

            // Theme Row
            item {
                SettingRowItem(
                    title = "Theme",
                    subtitle = when (ThemeManager.currentMode) {
                        com.mestxa.app.ui.theme.ThemeMode.DARK -> "Dark"
                        com.mestxa.app.ui.theme.ThemeMode.LIGHT -> "Light"
                        com.mestxa.app.ui.theme.ThemeMode.SYSTEM -> "System default"
                    },
                    onClick = onOpenTheme
                )
            }

            // Read receipts Toggle Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable { readReceiptsEnabled = !readReceiptsEnabled }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Read receipts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = theme.tx,
                        modifier = Modifier.weight(1f)
                    )
                    // Custom Prototype Toggle (.tg: 48x28dp, 14dp radius)
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (readReceiptsEnabled) theme.ac else theme.ol)
                            .padding(3.dp),
                        contentAlignment = if (readReceiptsEnabled) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (readReceiptsEnabled) theme.acx else theme.tx)
                        )
                    }
                }
            }

            // 8 Sub-Settings Rows
            items(subSettings) { title ->
                SettingRowItem(
                    title = title,
                    hasArrow = true,
                    onClick = {
                        selectedSubSetting = title
                    }
                )
            }

            // Replay intro
            item {
                SettingRowItem(
                    title = "Replay intro",
                    onClick = onReplayIntro
                )
            }

            // Sign Out / Reset
            item {
                SettingRowItem(
                    title = "Log out",
                    titleColor = Color(0xFFEF4444),
                    onClick = { showSignOutDialog = true }
                )
            }
        }
    }

    // Sub-Setting Modal Sheet
    selectedSubSetting?.let { subTitle ->
        ModalBottomSheet(
            onDismissRequest = { selectedSubSetting = null },
            containerColor = theme.sh,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(theme.s2, RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = subTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.tx,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                when (subTitle) {
                    "Account" -> {
                        Text("Handle: @$userHandle", color = theme.tx, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Number: ${userNumber.ifBlank { "Not set" }}", color = theme.s2, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                Toast.makeText(context, "Account active", Toast.LENGTH_SHORT).show()
                                selectedSubSetting = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.ac, contentColor = theme.acx)
                        ) {
                            Text("Done")
                        }
                    }
                    "Privacy" -> {
                        Text("All chats and calls are secured peer-to-peer. Zero server logs are retained.", color = theme.s2, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { selectedSubSetting = null },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.ac, contentColor = theme.acx)
                        ) {
                            Text("Done")
                        }
                    }
                    "Storage and data" -> {
                        Text("Local storage: SQLite Encrypted Vault", color = theme.tx, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("All media and database entries remain on your device only.", color = theme.s2, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { selectedSubSetting = null },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.ac, contentColor = theme.acx)
                        ) {
                            Text("Done")
                        }
                    }
                    else -> {
                        Text("$subTitle preferences are configured automatically.", color = theme.s2, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { selectedSubSetting = null },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.ac, contentColor = theme.acx)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Log out?", color = theme.tx) },
            text = { Text("This will clear your local keys on this device. Make sure you have your backup.", color = theme.s2) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    }
                ) {
                    Text("Log out", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = theme.tx)
                }
            },
            containerColor = theme.sh
        )
    }
}

@Composable
fun SettingRowItem(
    title: String,
    subtitle: String? = null,
    hasArrow: Boolean = false,
    titleColor: Color? = null,
    onClick: () -> Unit
) {
    val theme = ThemeManager.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = titleColor ?: theme.tx,
            modifier = Modifier.weight(1f)
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = theme.s2
            )
        }
        if (hasArrow) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = theme.s2,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
