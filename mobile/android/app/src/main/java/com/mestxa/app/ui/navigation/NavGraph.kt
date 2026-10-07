package com.mestxa.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mestxa.app.ui.screens.*

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object ChooseId : Screen("choose_id")
    object PickUsername : Screen("pick_username")
    object VerifyPhone : Screen("verify_phone")
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
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        // 1. Welcome Screen
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(Screen.ChooseId.route)
                },
                onLinkDevice = {
                    navController.navigate(Screen.LinkDevice.route)
                }
            )
        }

        // 2. Choose Identity Mode
        composable(Screen.ChooseId.route) {
            ChooseIdScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSelectUsername = {
                    navController.navigate(Screen.PickUsername.route)
                },
                onSelectPhone = {
                    navController.navigate(Screen.VerifyPhone.route)
                }
            )
        }

        // 3. Pick Anonymous @Username
        composable(Screen.PickUsername.route) {
            PickUsernameScreen(
                onBack = {
                    navController.popBackStack()
                },
                onContinue = { _ ->
                    navController.navigate(Screen.Pin.route)
                }
            )
        }

        // 4. Verify Phone via Silent Cryptographic SMS Challenge
        composable(Screen.VerifyPhone.route) {
            VerifyPhoneScreen(
                onBack = {
                    navController.popBackStack()
                },
                onContinue = { _ ->
                    navController.navigate(Screen.Pin.route)
                }
            )
        }

        // 5. Link Companion Device QR
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

        // 6. Set Vault Master PIN
        composable(Screen.Pin.route) {
            PinScreen(
                onBack = {
                    navController.popBackStack()
                },
                onPinComplete = { _ ->
                    navController.navigate(Screen.Chats.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
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
