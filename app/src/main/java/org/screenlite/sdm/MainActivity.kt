package org.screenlite.sdm

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.screenlite.sdm.config.KioskConfigurator
import org.screenlite.sdm.config.ManagedConfig
import org.screenlite.sdm.receivers.ScreenliteDeviceAdminReceiver

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Route to setup if not yet configured
        val prefs = getSharedPreferences("screenlite_provisioning", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("setup_complete", false)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, ScreenliteDeviceAdminReceiver::class.java)

        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                devicePolicyManager.setStatusBarDisabled(adminComponent, true)
            }
            devicePolicyManager.setLockTaskPackages(
                adminComponent,
                arrayOf("org.screenlite.webkiosk")
            )
        }

        applyConfig()

        // Start watchdog — monitors web-kiosk from the SDM process and restarts
        // it if the kiosk process dies (crash, OOM kill, etc.)
        WatchdogService.start(this)

        val updater = AutoUpdater(this)
        lifecycleScope.launch {
            updater.updateIfNeeded()
        }
    }

    private fun applyConfig() {
        val prefs = getSharedPreferences("screenlite_provisioning", Context.MODE_PRIVATE)
        val provServerUrl = prefs.getString("screenlite_server_url", null)
        val provScreenName = prefs.getString("screen_name", null)
        val provToken = prefs.getString("player_token", null)
        val provScreenId = prefs.getString("screen_id", null)

        val managedConfig = ManagedConfig(this)

        val serverUrl = if (!provServerUrl.isNullOrBlank()) {
            Log.i(TAG, "Using server URL from USB provisioning: $provServerUrl")
            provServerUrl
        } else if (managedConfig.isConfigured) {
            Log.i(TAG, "Using server URL from ManagedConfig: ${managedConfig.serverUrl}")
            managedConfig.serverUrl
        } else {
            Log.w(TAG, "No config source available — using default")
            managedConfig.serverUrl
        }

        val screenName = if (!provScreenName.isNullOrBlank()) {
            provScreenName
        } else {
            managedConfig.screenName
        }

        val screenId = if (!provScreenId.isNullOrBlank()) {
            provScreenId
        } else {
            managedConfig.screenId
        }

        Log.i(TAG, "Resolved config — serverUrl=$serverUrl, screenName=$screenName, screenId=$screenId")

        val playerUrl = if (screenId.isNotBlank()) {
            val base = "${serverUrl.trimEnd('/')}/player/$screenId"
            if (!provToken.isNullOrBlank()) "$base?token=$provToken" else base
        } else {
            serverUrl
        }

        Log.i(TAG, "Player URL: $playerUrl")

        // Launch web-kiosk with the URL embedded directly in the intent
        // This works even if web-kiosk is not already running
        val launchIntent = packageManager.getLaunchIntentForPackage("org.screenlite.webkiosk")
        if (launchIntent != null) {
            launchIntent.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("start_url", playerUrl)
                if (screenName.isNotBlank()) putExtra("screen_name", screenName)
            }
            startActivity(launchIntent)
            Log.i(TAG, "Launched web-kiosk with player URL: $playerUrl")
        } else {
            Log.e(TAG, "web-kiosk not installed or not launchable")
        }

        // Also send the broadcast in case web-kiosk was already running
        val configurator = KioskConfigurator(this)
        val urlPushed = configurator.pushServerUrl(playerUrl)
        Log.i(TAG, "pushServerUrl($playerUrl) = $urlPushed")

        if (screenName.isNotBlank()) {
            val namePushed = configurator.pushScreenName(screenName)
            Log.i(TAG, "pushScreenName($screenName) = $namePushed")
        }
    }
}