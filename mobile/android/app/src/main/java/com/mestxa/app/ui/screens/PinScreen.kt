package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.ui.theme.*

@Composable
fun PinScreen(
    onBack: () -> Unit,
    onPinComplete: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Set Vault PIN",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "This PIN derives your local SQLCipher master key via Argon2id. It is never sent to any server.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 6-digit PIN indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val isFilled = index < pin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = if (isFilled) TextPrimary else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = if (isFilled) TextPrimary else BorderHairline,
                                shape = CircleShape
                            )
                    )
                }
            }
        }

        // Tactile Numeric Keypad
        Column(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫")
            )

            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (digit in row) {
                        if (digit.isEmpty()) {
                            Spacer(modifier = Modifier.size(72.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceDark)
                                    .clickable {
                                        if (digit == "⌫") {
                                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        } else {
                                            if (pin.length < 6) {
                                                pin += digit
                                                if (pin.length == 6) {
                                                    onPinComplete(pin)
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
