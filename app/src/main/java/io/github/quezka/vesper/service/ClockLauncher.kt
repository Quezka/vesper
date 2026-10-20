package io.github.quezka.vesper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import io.github.quezka.vesper.R
import io.github.quezka.vesper.ui.ClockActivity

/**
 * Brings the clock up when the charger is plugged in. With "display over other apps" the activity starts directly;
 * without it a full-screen notification does the job (the system shows it over the lock screen).
 */
object ClockLauncher {
    const val CHANNEL_WAIT = "wait"
    const val CHANNEL_CHARGING = "charging"
    private const val ID_CHARGING = 2

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_WAIT, context.getString(R.string.channel_wait), NotificationManager.IMPORTANCE_MIN)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_CHARGING, context.getString(R.string.channel_charging), NotificationManager.IMPORTANCE_HIGH)
                .apply { setSound(null, null); enableVibration(false) }
        )
    }

    fun open(context: Context) {
        val intent = Intent(context, ClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Settings.canDrawOverlays(context)) {
            context.startActivity(intent)
            return
        }
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(context, CHANNEL_CHARGING)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(context.getString(R.string.charging_title))
            .setContentText(context.getString(R.string.charging_text))
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(ID_CHARGING, n)
    }

    fun dismissPrompt(context: Context) = context.getSystemService(NotificationManager::class.java).cancel(ID_CHARGING)
}
