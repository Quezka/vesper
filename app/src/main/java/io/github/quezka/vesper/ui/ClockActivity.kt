package io.github.quezka.vesper.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.quezka.vesper.data.Prefs
import io.github.quezka.vesper.service.ClockLauncher
import io.github.quezka.vesper.service.NoticeFeed

/** The bedside clock: black screen, big time, battery, and the notifications that came in. */
class ClockActivity : ComponentActivity() {
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        ClockLauncher.dismissPrompt(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = prefs.brightness / 100f }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            var battery by remember { mutableStateOf(readBattery(this@ClockActivity, null)) }
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action == Intent.ACTION_POWER_DISCONNECTED && prefs.closeOnUnplug) {
                            finish()
                            return
                        }
                        battery = readBattery(context, intent)
                    }
                }
                registerReceiver(receiver, IntentFilter().apply {
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                    addAction(Intent.ACTION_POWER_DISCONNECTED)
                })
                onDispose { unregisterReceiver(receiver) }
            }
            ClockScreen(
                prefs = prefs,
                battery = battery,
                notices = NoticeFeed.items,
                onClose = { finish() },
                onSettings = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
            )
        }
    }

    private fun readBattery(context: Context, intent: Intent?): BatteryState {
        val i = intent?.takeIf { it.action == Intent.ACTION_BATTERY_CHANGED }
            ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val plugged = (i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        return BatteryState(if (level < 0) 0 else level * 100 / scale, plugged)
    }
}

data class BatteryState(val percent: Int, val charging: Boolean)
