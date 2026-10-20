package io.github.quezka.vesper.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.quezka.vesper.data.Prefs
import io.github.quezka.vesper.service.ClockLauncher
import io.github.quezka.vesper.service.NoticeFeed
import io.github.quezka.vesper.service.PowerWatcher

/** Settings, permissions and updates. The clock itself lives in [ClockActivity]. */
class MainActivity : ComponentActivity() {
    private val askPost = registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh.intValue++ }
    private val refresh = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        ClockLauncher.createChannels(this)
        if (prefs.autoStart) PowerWatcher.start(this)

        lifecycle.addObserver(LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh.intValue++ })

        setContent {
            VesperTheme {
                SettingsScreen(
                    prefs = prefs,
                    permissions = remember(refresh.intValue) { permissionStates(this) },
                    onAskPostNotifications = { askPost.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    onOpen = { startActivity(it) },
                    onAutoStartChanged = { on -> if (on) PowerWatcher.start(this) else PowerWatcher.stop(this) },
                    onPreview = { ClockLauncher.open(this) },
                )
            }
        }
    }

    private fun permissionStates(context: Context): List<PermissionState> {
        val nm = context.getSystemService(NotificationManager::class.java)
        val pm = context.getSystemService(PowerManager::class.java)
        val pkg = Uri.parse("package:${context.packageName}")
        return listOf(
            PermissionState(
                PermissionKind.NOTICE_ACCESS, NoticeFeed.isEnabled(context),
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            ),
            PermissionState(
                PermissionKind.OVERLAY, Settings.canDrawOverlays(context),
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg),
            ),
            PermissionState(
                PermissionKind.POST_NOTIFICATIONS,
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
                null,
            ),
            PermissionState(
                PermissionKind.FULL_SCREEN,
                Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent(),
                if (Build.VERSION.SDK_INT >= 34) Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg) else null,
            ),
            PermissionState(
                PermissionKind.BATTERY, pm.isIgnoringBatteryOptimizations(context.packageName),
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg),
            ),
        )
    }
}

enum class PermissionKind { NOTICE_ACCESS, OVERLAY, POST_NOTIFICATIONS, FULL_SCREEN, BATTERY }

/** One permission row: whether it is granted and the system screen that grants it (null = ask in-app). */
data class PermissionState(val kind: PermissionKind, val granted: Boolean, val grantIntent: Intent?)
