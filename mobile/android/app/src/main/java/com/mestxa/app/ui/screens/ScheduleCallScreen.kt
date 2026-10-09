package com.mestxa.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
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
import com.mestxa.app.storage.DatabaseManager
import com.mestxa.app.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCallScreen(
    onBack: () -> Unit,
    onScheduled: () -> Unit = {}
) {
    val context = LocalContext.current
    val theme = ThemeManager.colors
    val db = remember { DatabaseManager.getInstance(context) }
    val contacts = remember { db.getContacts() }

    var title by remember { mutableStateOf("Product Sync") }
    var selectedContact by remember { mutableStateOf(contacts.firstOrNull()?.name ?: "") }
    var isVideo by remember { mutableStateOf(false) }
    var scheduledDate by remember { mutableStateOf("Tomorrow, 10:00 AM") }

    Scaffold(
        containerColor = theme.bg,
        topBar = {
            TopAppBar(
                title = { Text("Schedule call", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.tx) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.tx)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.bg)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Title field
                Column {
                    Text("Call Title", fontSize = 13.sp, color = theme.s2, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.acl,
                            unfocusedBorderColor = theme.ol,
                            focusedTextColor = theme.tx,
                            unfocusedTextColor = theme.tx
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Participant Contact Field
                Column {
                    Text("Participant", fontSize = 13.sp, color = theme.s2, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = selectedContact,
                        onValueChange = { selectedContact = it },
                        placeholder = { Text("Contact name or @username", color = theme.s2) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.acl,
                            unfocusedBorderColor = theme.ol,
                            focusedTextColor = theme.tx,
                            unfocusedTextColor = theme.tx
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Date & Time
                Column {
                    Text("When", fontSize = 13.sp, color = theme.s2, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.sf)
                            .border(1.dp, theme.ol, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = theme.acl)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(scheduledDate, fontSize = 15.sp, color = theme.tx, modifier = Modifier.weight(1f))
                    }
                }

                // Call Type (Audio vs Video)
                Column {
                    Text("Type", fontSize = 13.sp, color = theme.s2, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Voice
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (!isVideo) theme.sfa else theme.sf)
                                .border(1.dp, if (!isVideo) theme.acl else theme.ol, RoundedCornerShape(14.dp))
                                .clickable { isVideo = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = if (!isVideo) theme.acl else theme.s2, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Voice Call", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (!isVideo) theme.acl else theme.s2)
                            }
                        }

                        // Video
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isVideo) theme.sfa else theme.sf)
                                .border(1.dp, if (isVideo) theme.acl else theme.ol, RoundedCornerShape(14.dp))
                                .clickable { isVideo = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = if (isVideo) theme.acl else theme.s2, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Video Call", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (isVideo) theme.acl else theme.s2)
                            }
                        }
                    }
                }
            }

            // Schedule Button
            Button(
                onClick = {
                    if (title.isBlank() || selectedContact.isBlank()) {
                        Toast.makeText(context, "Fill in all fields", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Call scheduled for $selectedContact", Toast.LENGTH_SHORT).show()
                        onScheduled()
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.ac,
                    contentColor = theme.acx
                )
            ) {
                Text("Schedule Call", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
