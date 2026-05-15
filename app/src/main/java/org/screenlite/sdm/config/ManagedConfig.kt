package org.screenlite.sdm.config

import android.content.Context
import android.content.RestrictionsManager
import android.util.Log

private const val TAG = "ManagedConfig"
private const val DEFAULT_SERVER_URL = "https://screenlite.org"

class ManagedConfig(private val context: Context) {

    private val restrictionsManager =
        context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager

    val serverUrl: String
        get() {
            val restrictions = restrictionsManager.applicationRestrictions
            val url = restrictions.getString("screenlite_server_url", DEFAULT_SERVER_URL)
            Log.d(TAG, "Server URL from managed config: $url")
            return url ?: DEFAULT_SERVER_URL
        }

    val screenName: String
        get() {
            val restrictions = restrictionsManager.applicationRestrictions
            return restrictions.getString("screen_name", "") ?: ""
        }

    val screenId: String
        get() {
            val restrictions = restrictionsManager.applicationRestrictions
            return restrictions.getString("screen_id", "") ?: ""
        }

    val isConfigured: Boolean
        get() = serverUrl != DEFAULT_SERVER_URL
}