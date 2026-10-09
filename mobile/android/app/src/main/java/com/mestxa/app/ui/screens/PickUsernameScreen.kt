package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.network.MestxaApiClient
import com.mestxa.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PickUsernameScreen(
    onBack: () -> Unit,
    onContinue: (String) -> Unit
) {
    var rawInput by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var isAvailable by remember { mutableStateOf<Boolean?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val apiClient = remember { MestxaApiClient.getInstance() }

    LaunchedEffect(rawInput) {
        val clean = rawInput.trim().trimStart('@').lowercase()
        if (clean.length < 3) {
            isAvailable = null
            statusMessage = if (clean.isNotEmpty()) "At least 3 characters" else null
            return@LaunchedEffect
        }

        delay(350) // debounce
        isChecking = true
        val res = apiClient.checkUsername(clean)
        isChecking = false
        isAvailable = res.available
        statusMessage = if (res.available) "Username available" else (res.reason ?: "Username is taken")
    }

    val cleanUsername = rawInput.trim().trimStart('@').lowercase()
    val canProceed = isAvailable == true && cleanUsername.length >= 3

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                // 4-step progress dots (Step 2 active)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(TextPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 22.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TextPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(BorderHairline)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(BorderHairline)
                    )
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                Button(
                    onClick = {
                        if (canProceed) {
                            onContinue(cleanUsername)
                        }
                    },
                    enabled = canProceed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentWhite,
                        contentColor = OledBlack,
                        disabledContainerColor = SurfaceDark,
                        disabledContentColor = TextSecondary
                    )
                ) {
                    Text(
                        text = "Next",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Pick a username",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(28.dp))

            TextField(
                value = rawInput,
                onValueChange = { rawInput = it },
                placeholder = {
                    Text(
                        text = "username",
                        fontSize = 22.sp,
                        color = TextSecondary
                    )
                },
                trailingIcon = {
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = AccentWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (isAvailable == true) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Available",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(22.dp)
                        )
                    } else if (isAvailable == false) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Unavailable",
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = AccentWhite,
                    unfocusedIndicatorColor = BorderHairline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (statusMessage != null) {
                Text(
                    text = statusMessage ?: "",
                    fontSize = 14.sp,
                    color = when (isAvailable) {
                        true -> Color(0xFF81C784)
                        false -> Color(0xFFE57373)
                        else -> TextSecondary
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "People can search and message you with this handle.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextSecondary
            )
        }
    }
}
