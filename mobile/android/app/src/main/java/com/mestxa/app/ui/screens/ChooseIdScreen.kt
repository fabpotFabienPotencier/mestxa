package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.ui.theme.*

@Composable
fun ChooseIdScreen(
    onBack: () -> Unit,
    onSelectUsername: () -> Unit,
    onSelectPhone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
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
                text = "Choose Identity",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Select how you want to be discovered on the network:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Option A: Anonymous @Username
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderHairline, RoundedCornerShape(20.dp))
                .clickable { onSelectUsername() }
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(AccentBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "@",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Anonymous @Username",
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Keep your phone number completely private",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Option B: Phone Number
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderHairline, RoundedCornerShape(20.dp))
                .clickable { onSelectPhone() }
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(AccentGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Phone Number",
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Discover friends from your address book",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
