package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeScreen(
    onBack: () -> Unit
) {
    val theme = LocalAppTheme.current
    val currentThemeMode by ThemeManager.themeMode.collectAsState()
    val currentAccentIndex by ThemeManager.accentIndex.collectAsState()
    var showThemeSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = theme.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App theme",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = theme.tx
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.tx
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.bg
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Dark Mode Selector Card (prototype .row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.sf)
                    .border(1.dp, theme.ol, RoundedCornerShape(16.dp))
                    .clickable { showThemeSheet = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = theme.acl,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dark mode",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.tx
                    )
                    Text(
                        text = when (currentThemeMode) {
                            "dark" -> "Dark"
                            "light" -> "Light"
                            else -> "System default"
                        },
                        fontSize = 13.sp,
                        color = theme.s2
                    )
                }

                Text(
                    text = "Change",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.acl
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Palette Header (.lab in prototype)
            Text(
                text = "Color",
                fontSize = 13.sp,
                color = theme.s2,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 22-Color Swatch Grid (.cg in prototype)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(ACCENT_SWATCHES) { index, swatchColor ->
                    val isSelected = currentAccentIndex == index
                    val displayColor = swatchColor ?: theme.tx

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(displayColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) theme.tx else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                ThemeManager.setAccentIndex(index)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(theme.bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = theme.tx,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Live Bubble Preview
            Text(
                text = "Preview",
                fontSize = 13.sp,
                color = theme.s2,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.sf)
                    .border(1.dp, theme.ol, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Incoming Bubble
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.sfa)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Good morning! The prototypes look very clean.",
                            color = theme.tx,
                            fontSize = 14.sp
                        )
                    }

                    // Outgoing Bubble
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.ac)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Everything looks solid. Sending feedback now.",
                            color = theme.acx,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Theme Mode Selection (.sheet in prototype)
    if (showThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            containerColor = theme.sh,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(theme.s2, RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Dark mode",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.tx,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val modes = listOf(
                    Triple("dark", "Dark", "Permanent dark mode for OLED screens"),
                    Triple("light", "Light", "Clean white minimalist aesthetic"),
                    Triple("system", "System default", "Matches Android device setting")
                )

                modes.forEach { (modeKey, title, description) ->
                    val isCurrent = currentThemeMode == modeKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) theme.sfa else Color.Transparent)
                            .clickable {
                                ThemeManager.setThemeMode(modeKey)
                                showThemeSheet = false
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = theme.tx
                            )
                            Text(
                                text = description,
                                fontSize = 12.sp,
                                color = theme.s2
                            )
                        }

                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = theme.acl,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
