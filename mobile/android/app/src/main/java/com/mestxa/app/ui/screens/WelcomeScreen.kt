package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.ui.theme.*

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLinkDevice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Hero Brand Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            // M Geometric Icon Container
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(SurfaceDark, RoundedCornerShape(26.dp))
                    .border(1.dp, BorderHairline, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentWhite
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Mestxa",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Simple. Fast. Yours.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "• Zero-Knowledge Store & Forward Engine",
                style = MaterialTheme.typography.labelSmall,
                color = AccentGreen,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentWhite,
                    contentColor = OledBlack
                )
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onLinkDevice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderHairline)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    text = "Link a Companion Device",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Post-Quantum PQXDH & SFrame E2EE",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
    }
}
