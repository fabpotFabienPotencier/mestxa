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

/**
 * 22 curated accent colors matching the Mestxa device prototype
 */
val ACCENT_PALETTE: List<Color?> = listOf(
    null,                   // Default White / Monochrome
    Color(0xFF6B6B6B),      // Graphite
    Color(0xFF0E7490),      // Cyan
    Color(0xFF7E3AA0),      // Violet
    Color(0xFFC2255C),      // Pink Rose
    Color(0xFF1D4ED8),      // Blue
    Color(0xFF0B5394),      // Cobalt
    Color(0xFF5B3FD1),      // Electric Indigo
    Color(0xFF5B8C26),      // Lime
    Color(0xFF0F8F6F),      // Emerald Green
    Color(0xFF0B6B63),      // Teal
    Color(0xFF4B5D44),      // Forest Sage
    Color(0xFFE0B400),      // Gold
    Color(0xFF8B6F4E),      // Sand Bronze
    Color(0xFF6B4226),      // Mocha
    Color(0xFF9B1C31),      // Crimson
    Color(0xFFE58A00),      // Amber
    Color(0xFFC2410C),      // Orange
    Color(0xFFE11D48),      // Ruby
    Color(0xFF0EA5E9),      // Sky Blue
    Color(0xFF14B8A6),      // Aqua
    Color(0xFFA855F7)       // Purple
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeScreen(
    onBack: () -> Unit
) {
    var selectedThemeMode by remember { mutableStateOf("Dark") }
    var selectedAccentIndex by remember { mutableIntStateOf(0) }
    var showThemeSheet by remember { mutableStateOf(false) }

    val activeAccentColor = ACCENT_PALETTE[selectedAccentIndex] ?: TextPrimary

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App theme",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Dark Mode Selector Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                    .clickable { showThemeSheet = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = activeAccentColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dark mode",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = selectedThemeMode,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "Change",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = activeAccentColor
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Palette Header
            Text(
                text = "ACCENT COLOR (22 PRESETS)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 22-Color Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(ACCENT_PALETTE) { index, color ->
                    val isSelected = index == selectedAccentIndex
                    val displayColor = color ?: TextPrimary

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(displayColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) AccentWhite else BorderHairline,
                                shape = CircleShape
                            )
                            .clickable { selectedAccentIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (color == null || displayColor == TextPrimary) OledBlack else AccentWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Live Preview Card
            Text(
                text = "PREVIEW",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(activeAccentColor)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Mestxa PQXDH end-to-end encrypted.",
                                color = if (activeAccentColor == TextPrimary) OledBlack else AccentWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Theme Mode
    if (showThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            containerColor = SurfaceDark,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BorderHairline)
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
                    text = "Choose Theme",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                listOf("Dark", "Light", "System default").forEach { mode ->
                    val isModeSelected = mode == selectedThemeMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedThemeMode = mode
                                showThemeSheet = false
                            }
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mode,
                            fontSize = 16.sp,
                            fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isModeSelected) activeAccentColor else TextPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        if (isModeSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = activeAccentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
