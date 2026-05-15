package org.screenlite.sdm.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

private const val TAG = "DeviceAdminReceiver"

class ScreenliteDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Screenlite Device Admin Enabled", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "Device admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Screenlite Device Admin Disabled", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "Device admin disabled")
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        Log.i(TAG, "Provisioning complete")

        val extras = intent.getBundleExtra(
            "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"
        )

        if (extras != null) {
            saveProvisioningExtras(context, extras)
            Log.i(TAG, "Provisioning extras saved")
        } else {
            Log.w(TAG, "No provisioning extras found")
        }

        val launchIntent = Intent(context,
            Class.forName("org.screenlite.sdm.MainActivity"))
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }

    private fun saveProvisioningExtras(context: Context, extras: Bundle) {
        val prefs = context.getSharedPreferences(
            "screenlite_provisioning",
            Context.MODE_PRIVATE
        )
        prefs.edit().apply {
            extras.getString("screenlite_server_url")?.let {
                putString("screenlite_server_url", it)
                Log.i(TAG, "Saved server URL: $it")
            }
            extras.getString("screen_name")?.let {
                putString("screen_name", it)
                Log.i(TAG, "Saved screen name: $it")
            }
            extras.getString("screen_id")?.let {
                putString("screen_id", it)
            }
            apply()
        }
    }
}