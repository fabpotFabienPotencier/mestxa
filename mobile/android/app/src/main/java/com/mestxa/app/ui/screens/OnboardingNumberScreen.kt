package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.network.CountryDetector
import com.mestxa.app.network.CountryDto
import com.mestxa.app.network.MestxaApiClient
import com.mestxa.app.network.NumberOfferDto
import com.mestxa.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingNumberScreen(
    onBack: () -> Unit,
    onContinue: (number: String, offerToken: String, countryIso: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { MestxaApiClient.getInstance() }

    var selectedCountryIso by remember { mutableStateOf("US") }
    var currentOffer by remember { mutableStateOf<NumberOfferDto?>(null) }
    var allCountries by remember { mutableStateOf<List<CountryDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var countrySearchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadOffer(iso: String) {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val offer = apiClient.offerNumber(iso)
            if (offer != null) {
                currentOffer = offer
                selectedCountryIso = iso
            } else {
                errorMessage = "Failed to allocate number. Please check connection."
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val countries = apiClient.getCountries()
        if (countries.isNotEmpty()) {
            allCountries = countries
        }
        val detectedIso = CountryDetector.detectCountry(context)
        selectedCountryIso = detectedIso
        loadOffer(detectedIso)
    }

    val currentCountryInfo = allCountries.find { it.iso.equals(selectedCountryIso, ignoreCase = true) }
        ?: currentOffer?.country
        ?: CountryDto(selectedCountryIso, "Selected Country", "1", 10)

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

                // 4-step progress dots matching prototype
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step 1: active (22dp wide)
                    Box(
                        modifier = Modifier
                            .size(width = 22.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TextPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Step 2
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(BorderHairline)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Step 3
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(BorderHairline)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Step 4
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
                        val offer = currentOffer
                        if (offer != null) {
                            onContinue(offer.number, offer.offerToken, selectedCountryIso)
                        }
                    },
                    enabled = currentOffer != null && !isLoading,
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
                text = "Your number",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Country chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderHairline, RoundedCornerShape(20.dp))
                    .clickable { showCountryPicker = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentCountryInfo.flagEmoji}  ${currentCountryInfo.name} (+${currentCountryInfo.callingCode})",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Change country",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AccentWhite,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFE57373),
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { loadOffer(selectedCountryIso) },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("Retry")
                }
            } else {
                Text(
                    text = currentOffer?.number ?: "",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // "New number" pill button matching prototype line 1659
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, BorderHairline, RoundedCornerShape(20.dp))
                        .clickable { loadOffer(selectedCountryIso) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "New number",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New number",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "This is your number in the app. Share it with friends so they can find you anywhere in the world.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextSecondary
            )
        }
    }

    // Country selection modal sheet
    if (showCountryPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCountryPicker = false },
            containerColor = SurfaceSheet,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextSecondary)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Select country",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                TextField(
                    value = countrySearchQuery,
                    onValueChange = { countrySearchQuery = it },
                    placeholder = { Text("Search country...", color = TextSecondary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filtered = allCountries.filter {
                    it.name.contains(countrySearchQuery, ignoreCase = true) ||
                            it.iso.contains(countrySearchQuery, ignoreCase = true) ||
                            it.callingCode.contains(countrySearchQuery)
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCountryPicker = false
                                    selectedCountryIso = country.iso
                                    loadOffer(country.iso)
                                }
                                .padding(vertical = 14.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = country.flagEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = country.name,
                                    fontSize = 16.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "+${country.callingCode}",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                            if (country.iso.equals(selectedCountryIso, ignoreCase = true)) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = AccentWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = BorderHairline, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
