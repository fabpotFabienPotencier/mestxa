package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.ui.theme.*

@Composable
fun PickUsernameScreen(
    onBack: () -> Unit,
    onContinue: (String) -> Unit
) {
    var username by remember { mutableStateOf("@caleb") }

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pick Username",
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Your @username is linked to your Ed25519 public key. It can be changed at any time without resetting your encryption keys.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        if (!it.startsWith("@")) {
                            username = "@$it"
                        } else {
                            username = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = AccentWhite,
                        unfocusedBorderColor = BorderHairline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(AccentGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Available • Post-quantum ready",
                        fontSize = 12.sp,
                        color = AccentGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = { onContinue(username) },
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
                    text = "Continue",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
