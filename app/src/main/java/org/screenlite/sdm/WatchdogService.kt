package org.screenlite.sdm

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

/**
 * Runs in the SDM process (separate from web-kiosk) and restarts the kiosk
 * app if its process dies. Checks every 15 seconds.
 */
class WatchdogService : Service() {
    companion object {
        private const val TAG = "WatchdogService"
        private const val CHANNEL_ID = "sdm_watchdog_channel"
        private const val NOTIFICATION_ID = 42
        private const val CHECK_INTERVAL_MS = 30_000L
        private const val LAUNCH_COOLDOWN_MS = 90_000L  // don't re-check for 90s after a launch
        private const val KIOSK_PACKAGE = "org.screenlite.webkiosk"
        private const val KIOSK_ACTIVITY = "org.screenlite.webkiosk.MainActivity"

        fun start(context: Context) {
            val intent = Intent(context, WatchdogService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastLaunchTimeMs: Long = 0L

    private val checkTask = object : Runnable {
        override fun run() {
            checkAndRestartKiosk()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground()
        handler.post(checkTask)
        Log.i(TAG, "Watchdog started — monitoring $KIOSK_PACKAGE every ${CHECK_INTERVAL_MS / 1000}s")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkTask)
        Log.i(TAG, "Watchdog stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY // restart the watchdog itself if it's killed

    private fun checkAndRestartKiosk() {
        val now = System.currentTimeMillis()
        if (now - lastLaunchTimeMs < LAUNCH_COOLDOWN_MS) {
            Log.d(TAG, "Skipping check — within cooldown window after last launch")
            return
        }
        if (!isKioskRunning()) {
            Log.w(TAG, "web-kiosk not detected in foreground — restarting")
            launchKiosk()
        }
    }

    private fun isKioskRunning(): Boolean {
        // Use package manager to check if the kiosk process has a running activity.
        // runningAppProcesses() is unreliable on Android 11+ (can't see other apps).
        // Instead, check if the kiosk's main activity is in the recent tasks list.
        return try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val tasks = am.getRunningTasks(10)
            tasks?.any { task ->
                task.baseActivity?.packageName == KIOSK_PACKAGE ||
                task.topActivity?.packageName == KIOSK_PACKAGE
            } == true
        } catch (e: Exception) {
            Log.w(TAG, "isKioskRunning check failed: ${e.message} — assuming running")
            true // fail safe: don't restart if we can't tell
        }
    }

    private fun launchKiosk() {
        try {
            val prefs = getSharedPreferences("screenlite_provisioning", Context.MODE_PRIVATE)
            val serverUrl = prefs.getString("screenlite_server_url", null)
            val screenId = prefs.getString("screen_id", null)
            val token = prefs.getString("player_token", null)

            val playerUrl = if (!serverUrl.isNullOrBlank() && !screenId.isNullOrBlank()) {
                val base = "${serverUrl.trimEnd('/')}/player/$screenId"
                if (!token.isNullOrBlank()) "$base?token=$token" else base
            } else null

            val intent = packageManager.getLaunchIntentForPackage(KIOSK_PACKAGE)
                ?: Intent().apply {
                    setClassName(KIOSK_PACKAGE, KIOSK_ACTIVITY)
                }

            intent.apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP  // routes to onNewIntent, never recreates
                )
                if (playerUrl != null) putExtra("start_url", playerUrl)
            }

            startActivity(intent)
            lastLaunchTimeMs = System.currentTimeMillis()
            Log.i(TAG, "Restarted web-kiosk${if (playerUrl != null) " with URL: $playerUrl" else ""}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart web-kiosk: ${e.message}")
        }
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screenlite Watchdog",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Screenlite SDM")
                .setContentText("Monitoring kiosk")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Screenlite SDM")
                .setContentText("Monitoring kiosk")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        }

        startForeground(NOTIFICATION_ID, notification)
    }
}
