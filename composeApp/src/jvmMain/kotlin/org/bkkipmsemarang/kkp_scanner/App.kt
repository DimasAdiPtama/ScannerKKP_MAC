package org.bkkipmsemarang.kkp_scanner

import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class Screen {
    LOGIN,
    DASHBOARD,
    SCAN_QR
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentScreen by remember { 
            mutableStateOf(if (SessionManager.isLoggedIn()) Screen.DASHBOARD else Screen.LOGIN) 
        }

        Crossfade(targetState = currentScreen) { screen ->
            when (screen) {
                Screen.LOGIN -> LoginScreen(onLoginSuccess = { currentScreen = Screen.DASHBOARD })
                Screen.DASHBOARD -> DashboardScreen(
                    onScanClick = { currentScreen = Screen.SCAN_QR },
                    onLogout = { 
                        SessionManager.clearSession()
                        currentScreen = Screen.LOGIN 
                    }
                )
                Screen.SCAN_QR -> QRScanScreen(onBack = { currentScreen = Screen.DASHBOARD })
            }
        }
    }
}
