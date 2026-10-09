package com.mestxa.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.storage.VaultManager
import com.mestxa.app.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrProfileScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val theme = ThemeManager.colors

    val displayName = remember { VaultManager.getDisplayName(context).ifBlank { "David Caleb" } }
    val userHandle = remember { VaultManager.getUsername(context).ifBlank { "davidcaleb" } }

    var selectedBackgroundIdx by remember { mutableIntStateOf(0) }

    val backgrounds = listOf(
        Brush.linearGradient(listOf(Color(0xFF000000), Color(0xFF000000))),
        Brush.linearGradient(listOf(Color(0xFF2C2C2C), Color(0xFF000000))),
        Brush.linearGradient(listOf(theme.ac.copy(alpha = 0.7f), Color(0xFF000000))),
        Brush.radialGradient(listOf(Color(0xFF1E293B), Color(0xFF050505)))
    )

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgrounds[selectedBackgroundIdx])
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Centered QR Badge with -36dp overlapping avatar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(260.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(top = 46.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // QR Code Vector
                            Box(
                                modifier = Modifier
                                    .size(192.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "QR Code",
                                    tint = Color.Black,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Centered Chat Badge on QR code
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                        .border(3.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "@${userHandle.uppercase()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Overlapping Avatar (-36dp offset on top)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-82).dp)
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(theme.sf)
                                .border(4.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.take(2).uppercase().ifBlank { userHandle.take(2).uppercase() },
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Bottom Panel matching prototype
                Surface(
                    color = theme.sh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .border(1.dp, theme.ol, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "QR Code",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.tx,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 4 Style cards strip
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(backgrounds.size) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 70.dp, height = 88.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(backgrounds[idx])
                                        .border(
                                            width = if (selectedBackgroundIdx == idx) 2.dp else 1.dp,
                                            color = if (selectedBackgroundIdx == idx) theme.acl else theme.ol,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedBackgroundIdx = idx },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Share QR Code Button
                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Connect with me on Mestxa: @$userHandle")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share QR Code"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.ac,
                                contentColor = theme.acx
                            )
                        ) {
                            Text("Share QR Code", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Scan QR Code Button
                        TextButton(
                            onClick = {
                                Toast.makeText(context, "Camera scanner ready", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = theme.acl,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan QR Code", color = theme.acl, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
