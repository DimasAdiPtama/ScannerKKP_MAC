package org.bkkipmsemarang.kkp_scanner

import java.util.prefs.Preferences

object SessionManager {
    private val prefs = Preferences.userNodeForPackage(SessionManager::class.java)
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    fun saveLoginSession(rememberMe: Boolean) {
        if (rememberMe) {
            prefs.putBoolean(KEY_IS_LOGGED_IN, true)
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun clearSession() {
        prefs.remove(KEY_IS_LOGGED_IN)
    }
}
