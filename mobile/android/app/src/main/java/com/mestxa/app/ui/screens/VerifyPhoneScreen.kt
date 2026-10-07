package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.ui.theme.*

@Composable
fun VerifyPhoneScreen(
    onBack: () -> Unit,
    onContinue: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("+1 (555) 019-2834") }

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Verify Number",
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
                    text = "We verify your phone number through a silent cryptographic SMS challenge to discover contacts while preserving full metadata secrecy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
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

                Text(
                    text = "Zero phone numbers are stored in plaintext on the relay.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Button(
                onClick = { onContinue(phoneNumber) },
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
                    text = "Send Code",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
