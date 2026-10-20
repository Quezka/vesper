package io.github.quezka.vesper.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import io.github.quezka.vesper.R
import io.github.quezka.vesper.data.Prefs
import io.github.quezka.vesper.ui.MainActivity

/** Sits quietly in the background and opens the clock when power is connected. */
class PowerWatcher : Service() {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_POWER_CONNECTED && Prefs(context).autoStart) ClockLauncher.open(context)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ClockLauncher.createChannels(this)
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, ClockLauncher.CHANNEL_WAIT)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(getString(R.string.waiting_title))
            .setContentText(getString(R.string.waiting_text))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        registerReceiver(receiver, IntentFilter(Intent.ACTION_POWER_CONNECTED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            context.startForegroundService(Intent(context, PowerWatcher::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PowerWatcher::class.java))
        }
    }
}
