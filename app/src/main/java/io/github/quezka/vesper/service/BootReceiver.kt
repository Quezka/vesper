package io.github.quezka.vesper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.quezka.vesper.data.Prefs

/**
 * Restarts the watcher after a reboot or an update of the app. After a reboot the phone may already be
 * on the charger (switched off, plugged in, then switched back on): the "power connected" event came
 * before Vesper was running, so the clock is opened now instead of waiting for a plug-in that won't come.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs(context).autoStart) return
        PowerWatcher.start(context)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && ClockLauncher.isPluggedIn(context)) ClockLauncher.open(context)
    }
}
