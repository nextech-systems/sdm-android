package org.screenlite.sdm.config

import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "KioskConfigurator"
private const val KIOSK_PACKAGE = "org.screenlite.webkiosk"
private const val ACTION_SET_CONFIG = "org.screenlite.webkiosk.SET_CONFIG"

class KioskConfigurator(private val context: Context) {
    fun pushServerUrl(serverUrl: String): Boolean {
        return try {
            val intent = Intent(ACTION_SET_CONFIG).apply {
                setPackage(KIOSK_PACKAGE)
                putExtra("start_url", serverUrl)
            }
            context.sendBroadcast(intent)
            Log.i(TAG, "Broadcast sent with start_url: $serverUrl")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send config broadcast: ${e.message}", e)
            false
        }
    }

    fun pushScreenName(screenName: String): Boolean {
        return try {
            val intent = Intent(ACTION_SET_CONFIG).apply {
                setPackage(KIOSK_PACKAGE)
                putExtra("screen_name", screenName)
            }
            context.sendBroadcast(intent)
            Log.i(TAG, "Broadcast sent with screen_name: $screenName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send screen name broadcast: ${e.message}", e)
            false
        }
    }
}