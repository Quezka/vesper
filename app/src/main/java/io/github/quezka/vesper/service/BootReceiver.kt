package io.github.quezka.vesper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.quezka.vesper.data.Prefs

/** Restarts the watcher after a reboot or an update of the app. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs(context).autoStart) PowerWatcher.start(context)
    }
}
