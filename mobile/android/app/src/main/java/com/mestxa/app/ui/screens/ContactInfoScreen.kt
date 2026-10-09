package com.mestxa.app.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactInfoScreen(
    contactName: String,
    onBack: () -> Unit,
    onStartCall: (String) -> Unit = {},
    onStartVideoCall: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val theme = ThemeManager.colors

    var notificationsEnabled by remember { mutableStateOf(true) }
    var disappearingEnabled by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = theme.bg,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.tx
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.bg)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Big Avatar & Name Header (.big in prototype)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(theme.sf)
                            .border(1.dp, theme.ol, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contactName.take(2).uppercase(),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.tx
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = contactName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.tx
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "online",
                        fontSize = 14.sp,
                        color = theme.acl,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Actions: Voice, Video, Chat (.qa)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        ContactQuickActionPill(
                            icon = Icons.Default.Phone,
                            label = "Voice",
                            onClick = { onStartCall(contactName) }
                        )
                        ContactQuickActionPill(
                            icon = Icons.Default.Videocam,
                            label = "Video",
                            onClick = { onStartVideoCall(contactName) }
                        )
                        ContactQuickActionPill(
                            icon = Icons.Default.ChatBubble,
                            label = "Chat",
                            onClick = onBack
                        )
                    }
                }
            }

            // Notifications Toggle Row
            item {
                ContactToggleRow(
                    title = "Notifications",
                    checked = notificationsEnabled,
                    onToggle = { notificationsEnabled = !notificationsEnabled }
                )
            }

            // Disappearing Messages Toggle Row
            item {
                ContactToggleRow(
                    title = "Disappearing messages",
                    checked = disappearingEnabled,
                    onToggle = { disappearingEnabled = !disappearingEnabled }
                )
            }

            // Media and Files Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable { Toast.makeText(context, "Media and files", Toast.LENGTH_SHORT).show() }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Media and files",
                        fontSize = 16.sp,
                        color = theme.tx,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = theme.s2,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Block Contact Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable {
                            Toast.makeText(context, "$contactName blocked", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Block",
                        fontSize = 16.sp,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
fun ContactQuickActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val theme = ThemeManager.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(theme.sfa)
                .border(1.dp, theme.ol, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = theme.acl,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = theme.s2
        )
    }
}

@Composable
fun ContactToggleRow(
    title: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val theme = ThemeManager.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onToggle() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = theme.tx,
            modifier = Modifier.weight(1f)
        )
        // Custom Prototype Toggle (.tg: 48x28dp, 14dp radius)
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (checked) theme.ac else theme.ol)
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (checked) theme.acx else theme.tx)
            )
        }
    }
}
