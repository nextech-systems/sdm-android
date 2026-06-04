package org.screenlite.sdm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.screenlite.sdm.WatchdogService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Boot completed — launching SDM MainActivity and watchdog")
            val i = Intent(context, Class.forName("org.screenlite.sdm.MainActivity"))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
            WatchdogService.start(context)
        }
    }
}