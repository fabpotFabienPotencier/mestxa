package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.storage.DatabaseManager
import com.mestxa.app.storage.StatusRecordEntity
import com.mestxa.app.storage.VaultManager
import com.mestxa.app.ui.theme.*
import java.util.UUID

@Composable
fun AddStatusScreen(
    onBack: () -> Unit,
    onStatusPosted: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { DatabaseManager.getInstance(context) }
    var statusText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add status",
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
        },
        bottomBar = {
            Surface(
                color = OledBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Add Image", tint = TextPrimary)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo", tint = TextPrimary)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = {
                            val cleanText = statusText.trim()
                            if (cleanText.isNotBlank()) {
                                val myName = VaultManager.getDisplayName(context).ifBlank { "Me" }
                                val myHandle = VaultManager.getUsername(context).ifBlank { "me" }
                                db.saveStatus(
                                    StatusRecordEntity(
                                        id = UUID.randomUUID().toString(),
                                        userName = myName,
                                        userHandle = myHandle,
                                        text = cleanText,
                                        colorGradientIdx = (cleanText.hashCode() and 0x7fffffff) % 5
                                    )
                                )
                                onStatusPosted()
                            }
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(AccentWhite)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Post Status", tint = OledBlack)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            TextField(
                value = statusText,
                onValueChange = { statusText = it },
                placeholder = {
                    Text(
                        text = "Type a status",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentWhite,
                    textAlign = TextAlign.Center
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
