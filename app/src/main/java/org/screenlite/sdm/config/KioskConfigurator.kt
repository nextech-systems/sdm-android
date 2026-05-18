package org.screenlite.sdm.config

import android.content.Context
import android.util.Log

private const val TAG = "KioskConfigurator"
private const val KIOSK_PACKAGE = "org.screenlite.webkiosk"

class KioskConfigurator(private val context: Context) {

    fun pushServerUrl(serverUrl: String): Boolean {
        return try {
            val kioskContext = context.createPackageContext(
                KIOSK_PACKAGE,
                Context.CONTEXT_IGNORE_SECURITY
            )
            val prefs = kioskContext.getSharedPreferences(
                "kiosk_settings",
                Context.MODE_PRIVATE
            )
            prefs.edit().putString("start_url", serverUrl).apply()
            Log.i(TAG, "Pushed server URL to web-kiosk: $serverUrl")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push config to web-kiosk: ${e.message}", e)
            false
        }
    }

    fun pushScreenName(screenName: String): Boolean {
        return try {
            val kioskContext = context.createPackageContext(
                KIOSK_PACKAGE,
                Context.CONTEXT_IGNORE_SECURITY
            )
            val prefs = kioskContext.getSharedPreferences(
                "kiosk_settings",
                Context.MODE_PRIVATE
            )
            prefs.edit().putString("screen_name", screenName).apply()
            Log.i(TAG, "Pushed screen name to web-kiosk: $screenName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push screen name to web-kiosk: ${e.message}", e)
            false
        }
    }
}