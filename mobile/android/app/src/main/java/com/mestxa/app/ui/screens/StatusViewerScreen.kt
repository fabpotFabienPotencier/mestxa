package com.mestxa.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.ui.theme.*

@Composable
fun StatusViewerScreen(
    contactName: String,
    onClose: () -> Unit
) {
    var currentSegment by remember { mutableIntStateOf(0) }
    val totalSegments = 3
    var replyText by remember { mutableStateOf("") }

    val gradient = remember(contactName) {
        if (contactName == "Bella") {
            Brush.verticalGradient(listOf(Color(0xFFF4A261), Color(0xFF5A2D0C), Color(0xFF000000)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFF2A9D8F), Color(0xFF0B3D3A), Color(0xFF000000)))
        }
    }

    val captions = listOf(
        "Big things start small. Shipping the new cryptographic engine.",
        "Team sync was high signal today.",
        "Zero cloud leaks. Everything verified."
    )

    // Segment timer progress animation (5 seconds per segment)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(currentSegment) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
        )
        if (currentSegment < totalSegments - 1) {
            currentSegment++
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Tap Zones for navigating stories (Left: Prev, Right: Next)
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentSegment > 0) currentSegment-- else onClose()
                    }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentSegment < totalSegments - 1) currentSegment++ else onClose()
                    }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Segmented Story Progress Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0 until totalSegments) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        ) {
                            val barProgress = when {
                                i < currentSegment -> 1f
                                i == currentSegment -> progress.value
                                else -> 0f
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barProgress)
                                    .fillMaxHeight()
                                    .background(Color.White)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Profile Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contactName.take(1),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contactName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentWhite
                        )
                        Text(
                            text = "Today, 9:40 AM",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AccentWhite
                        )
                    }
                }
            }

            // Center Caption
            Text(
                text = captions[currentSegment.coerceIn(0, captions.size - 1)],
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentWhite,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            // Bottom Reply Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = {
                        Text(text = "Reply", color = Color.White.copy(alpha = 0.6f))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.15f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                        focusedBorderColor = Color.White.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            replyText = ""
                            onClose()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentWhite)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Reply",
                        tint = OledBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
