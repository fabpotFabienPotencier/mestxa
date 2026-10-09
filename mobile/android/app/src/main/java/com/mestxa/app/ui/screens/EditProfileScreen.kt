package com.mestxa.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.storage.VaultManager
import com.mestxa.app.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val theme = ThemeManager.colors

    val currentName = remember { VaultManager.getDisplayName(context).ifBlank { "User" } }
    val currentHandle = remember { VaultManager.getUsername(context).ifBlank { "user" } }

    var name by remember { mutableStateOf(currentName) }
    var about by remember { mutableStateOf("Available") }
    var username by remember { mutableStateOf(currentHandle) }

    Scaffold(
        containerColor = theme.bg,
        topBar = {
            TopAppBar(
                title = { Text("Edit profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.tx) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.tx)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.bg)
            )
        },
        bottomBar = {
            Surface(
                color = theme.bg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = {
                        val cleanName = name.trim()
                        val cleanUser = username.trim().removePrefix("@")
                        if (cleanName.isEmpty() || cleanUser.isEmpty()) {
                            Toast.makeText(context, "Name and username needed", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        VaultManager.saveDisplayName(context, cleanName)
                        VaultManager.saveUsername(context, cleanUser)
                        Toast.makeText(context, "Profile saved", Toast.LENGTH_SHORT).show()
                        onBack()
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
                    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Centered Avatar with Camera overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 28.dp),
                contentAlignment = Alignment.Center
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
                        text = name.take(2).uppercase().ifBlank { "ME" },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.tx
                    )
                }

                Box(
                    modifier = Modifier
                        .size(96.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(theme.ac)
                            .border(2.dp, theme.bg, CircleShape)
                            .clickable {
                                Toast.makeText(context, "Photo picker", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Change photo",
                            tint = theme.acx,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Name Field
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text("Name", fontSize = 13.sp, color = theme.s2)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = theme.ol,
                        unfocusedIndicatorColor = theme.ol,
                        focusedTextColor = theme.tx,
                        unfocusedTextColor = theme.tx
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // About Field
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text("About", fontSize = 13.sp, color = theme.s2)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = about,
                    onValueChange = { about = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = theme.ol,
                        unfocusedIndicatorColor = theme.ol,
                        focusedTextColor = theme.tx,
                        unfocusedTextColor = theme.tx
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Username Field
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text("Username", fontSize = 13.sp, color = theme.s2)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    singleLine = true,
                    prefix = { Text("@", color = theme.s2) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = theme.ol,
                        unfocusedIndicatorColor = theme.ol,
                        focusedTextColor = theme.tx,
                        unfocusedTextColor = theme.tx
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
