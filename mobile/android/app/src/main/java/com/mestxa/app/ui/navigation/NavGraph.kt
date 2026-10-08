package com.mestxa.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mestxa.app.ui.screens.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object OnboardingNumber : Screen("onboarding_number")
    object PickUsername : Screen("pick_username")
    object ProfileSetup : Screen("profile_setup")
    object LinkDevice : Screen("link_device")
    object Pin : Screen("pin")
    object Chats : Screen("chats")
    object Conversation : Screen("conversation/{contactName}") {
        fun createRoute(contactName: String) = "conversation/$contactName"
    }
    object ActiveCall : Screen("active_call/{contactName}") {
        fun createRoute(contactName: String) = "active_call/$contactName"
    }
    object Settings : Screen("settings")
    object StatusViewer : Screen("status_viewer/{contactName}") {
        fun createRoute(contactName: String) = "status_viewer/$contactName"
    }
    object AddStatus : Screen("add_status")
    object QrProfile : Screen("qr_profile")
    object ContactInfo : Screen("contact_info/{contactName}") {
        fun createRoute(contactName: String) = "contact_info/$contactName"
    }
    object AppTheme : Screen("app_theme")
}

@Composable
fun MestxaNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Welcome.route
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Temporary onboarding state
    var obNumber by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var obOfferToken by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var obCountryIso by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("US") }
    var obUsername by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var obDisplayName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        // Step 0: Welcome Screen
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(Screen.OnboardingNumber.route)
                },
                onLinkDevice = {
                    navController.navigate(Screen.LinkDevice.route)
                }
            )
        }

        // Step 1: Your number (Country-aware MX number offer)
        composable(Screen.OnboardingNumber.route) {
            OnboardingNumberScreen(
                onBack = {
                    navController.popBackStack()
                },
                onContinue = { number, offerToken, countryIso ->
                    obNumber = number
                    obOfferToken = offerToken
                    obCountryIso = countryIso
                    navController.navigate(Screen.PickUsername.route)
                }
            )
        }

        // Step 2: Pick a username
        composable(Screen.PickUsername.route) {
            PickUsernameScreen(
                onBack = {
                    navController.popBackStack()
                },
                onContinue = { username ->
                    obUsername = username
                    navController.navigate(Screen.ProfileSetup.route)
                }
            )
        }

        // Step 3: Your profile (Name & avatar)
        composable(Screen.ProfileSetup.route) {
            ProfileSetupScreen(
                username = obUsername,
                onBack = {
                    navController.popBackStack()
                },
                onContinue = { displayName ->
                    obDisplayName = displayName
                    navController.navigate(Screen.Pin.route)
                }
            )
        }

        // Link Companion Device QR
        composable(Screen.LinkDevice.route) {
            LinkDeviceScreen(
                onBack = {
                    navController.popBackStack()
                },
                onLinkComplete = {
                    navController.navigate(Screen.Chats.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // Step 4: Set a PIN & finalize registration
        composable(Screen.Pin.route) {
            PinScreen(
                onBack = {
                    navController.popBackStack()
                },
                onPinComplete = { pin ->
                    coroutineScope.launch {
                        // 1. Get or create post-quantum identity bundle
                        val bundle = com.mestxa.app.storage.VaultManager.getOrCreateIdentityBundle(context)
                        val privateKey = bundle.identityPrivateKey ?: ByteArray(32)
                        val userHex = com.mestxa.app.engine.MestxaBridge.bytesToHex(bundle.identityKey)

                        // 2. Save PIN if provided
                        if (pin.isNotBlank()) {
                            com.mestxa.app.storage.VaultManager.saveVaultPin(context, pin)
                        }

                        // 3. Register account on relay
                        val regRes = com.mestxa.app.network.MestxaApiClient.getInstance().registerAccount(
                            userHex = userHex,
                            number = obNumber,
                            offerToken = obOfferToken,
                            username = obUsername,
                            name = obDisplayName,
                            countryIso = obCountryIso,
                            privateKey = privateKey
                        )

                        // 4. Upload post-quantum prekeys to relay
                        com.mestxa.app.network.MestxaApiClient.getInstance().uploadPrekeys(
                            userHex = userHex,
                            bundle = bundle,
                            username = obUsername
                        )

                        // 5. Persist account locally
                        val finalNumber = regRes.getOrNull()?.number ?: obNumber
                        val finalUsername = regRes.getOrNull()?.username ?: obUsername
                        val finalName = regRes.getOrNull()?.name ?: obDisplayName

                        com.mestxa.app.storage.VaultManager.saveAccount(
                            context = context,
                            number = finalNumber,
                            username = finalUsername,
                            displayName = finalName,
                            countryIso = obCountryIso
                        )

                        // 6. Connect WebSocket gateway with registered identity
                        com.mestxa.app.network.MestxaNetworkService.getInstance().connect()

                        // 7. Navigate directly to Chats screen
                        navController.navigate(Screen.Chats.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 7. Core 4-Tab Screen (Chats, Updates, Communities, Calls)
        composable(Screen.Chats.route) {
            ChatsScreen(
                onOpenChat = { _, contactName ->
                    navController.navigate(Screen.Conversation.createRoute(contactName))
                },
                onStartCall = { contactName ->
                    navController.navigate(Screen.ActiveCall.createRoute(contactName))
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onViewStatus = { contactName ->
                    navController.navigate(Screen.StatusViewer.createRoute(contactName))
                },
                onAddStatus = {
                    navController.navigate(Screen.AddStatus.route)
                }
            )
        }

        // 8. Individual Conversation View
        composable(
            route = Screen.Conversation.route,
            arguments = listOf(
                navArgument("contactName") {
                    type = NavType.StringType
                    defaultValue = "Contact"
                }
            )
        ) { backStackEntry ->
            val contactName = backStackEntry.arguments?.getString("contactName") ?: "Contact"
            ConversationScreen(
                contactName = contactName,
                onBack = {
                    navController.popBackStack()
                },
                onStartCall = { name ->
                    navController.navigate(Screen.ActiveCall.createRoute(name))
                },
                onOpenContactInfo = { name ->
                    navController.navigate(Screen.ContactInfo.createRoute(name))
                }
            )
        }

        // 9. Pristine 48 kHz WebRTC Active Call Screen
        composable(
            route = Screen.ActiveCall.route,
            arguments = listOf(
                navArgument("contactName") {
                    type = NavType.StringType
                    defaultValue = "Contact"
                }
            )
        ) { backStackEntry ->
            val contactName = backStackEntry.arguments?.getString("contactName") ?: "Contact"
            ActiveCallScreen(
                contactName = contactName,
                onEndCall = {
                    navController.popBackStack()
                }
            )
        }

        // 10. Settings & Zero-Cloud Security Vault
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onOpenQr = {
                    navController.navigate(Screen.QrProfile.route)
                },
                onOpenTheme = {
                    navController.navigate(Screen.AppTheme.route)
                },
                onSignOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 11. WhatsApp/Instagram Style Segmented Status Story Viewer
        composable(
            route = Screen.StatusViewer.route,
            arguments = listOf(
                navArgument("contactName") {
                    type = NavType.StringType
                    defaultValue = "Contact"
                }
            )
        ) { backStackEntry ->
            val contactName = backStackEntry.arguments?.getString("contactName") ?: "Contact"
            StatusViewerScreen(
                contactName = contactName,
                onClose = {
                    navController.popBackStack()
                }
            )
        }

        // 12. Fullscreen Add Status Screen
        composable(Screen.AddStatus.route) {
            AddStatusScreen(
                onBack = {
                    navController.popBackStack()
                },
                onStatusPosted = {
                    navController.popBackStack()
                }
            )
        }

        // 13. Dedicated QR Profile Screen
        composable(Screen.QrProfile.route) {
            QrProfileScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 14. Contact Info Screen
        composable(
            route = Screen.ContactInfo.route,
            arguments = listOf(
                navArgument("contactName") {
                    type = NavType.StringType
                    defaultValue = "Contact"
                }
            )
        ) { backStackEntry ->
            val contactName = backStackEntry.arguments?.getString("contactName") ?: "Contact"
            ContactInfoScreen(
                contactName = contactName,
                onBack = {
                    navController.popBackStack()
                },
                onStartCall = { name ->
                    navController.navigate(Screen.ActiveCall.createRoute(name))
                },
                onStartVideoCall = { name ->
                    navController.navigate(Screen.ActiveCall.createRoute(name))
                }
            )
        }

        // 15. App Theme & 22-Accent Color Customizer
        composable(Screen.AppTheme.route) {
            AppThemeScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
