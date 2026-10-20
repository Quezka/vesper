package io.github.quezka.vesper.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quezka.vesper.BuildConfig
import io.github.quezka.vesper.R
import io.github.quezka.vesper.data.Prefs
import io.github.quezka.vesper.data.QuireLink
import io.github.quezka.vesper.data.ReleaseFeed
import io.github.quezka.vesper.data.Updater
import io.github.quezka.vesper.domain.Release
import io.github.quezka.vesper.domain.newerThan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object Current : UpdateState
    data object NoRelease : UpdateState
    data object Failed : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val percent: Int) : UpdateState
}

@Composable
fun SettingsScreen(
    prefs: Prefs,
    permissions: List<PermissionState>,
    onAskPostNotifications: () -> Unit,
    onAskQuire: () -> Unit,
    onOpen: (Intent) -> Unit,
    onAutoStartChanged: (Boolean) -> Unit,
    onPreview: () -> Unit,
) {
    var autoStart by remember { mutableStateOf(prefs.autoStart) }
    var closeOnUnplug by remember { mutableStateOf(prefs.closeOnUnplug) }
    var use24h by remember { mutableStateOf(prefs.use24h) }
    var seconds by remember { mutableStateOf(prefs.seconds) }
    var nightTint by remember { mutableStateOf(prefs.nightTint) }
    var showNotices by remember { mutableStateOf(prefs.showNotices) }
    var showQuire by remember { mutableStateOf(prefs.showQuire) }
    var brightness by remember { mutableStateOf(prefs.brightness.toFloat()) }
    var hidden by remember { mutableStateOf(prefs.hiddenApps) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(stringResource(R.string.app_name), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.tagline), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Section(stringResource(R.string.sec_status)) {
                SwitchRow(stringResource(R.string.auto_start), stringResource(R.string.auto_start_hint), autoStart) {
                    autoStart = it; prefs.autoStart = it; onAutoStartChanged(it)
                }
                SwitchRow(stringResource(R.string.close_on_unplug), null, closeOnUnplug) { closeOnUnplug = it; prefs.closeOnUnplug = it }
                Button(onClick = onPreview, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.preview_now)) }
            }

            Section(stringResource(R.string.sec_access)) {
                permissions.forEach { p ->
                    val (title, hint) = permissionText(p.kind)
                    PermissionRow(title, hint, p.granted) {
                        if (p.grantIntent != null) onOpen(p.grantIntent) else onAskPostNotifications()
                    }
                }
            }

            Section(stringResource(R.string.sec_quire)) {
                SwitchRow(stringResource(R.string.quire_show), stringResource(R.string.quire_show_hint), showQuire) { showQuire = it; prefs.showQuire = it }
                val quireAllowed = QuireLink.allowed(LocalContext.current)
                val quireInstalled = QuireLink.installed(LocalContext.current)
                when {
                    !quireInstalled -> Text(stringResource(R.string.quire_missing), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    quireAllowed -> Text(stringResource(R.string.perm_on), color = Palette.Success, fontWeight = FontWeight.SemiBold)
                    else -> Button(onClick = onAskQuire, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.quire_allow)) }
                }
            }

            Section(stringResource(R.string.sec_clock)) {
                SwitchRow(stringResource(R.string.use24h), null, use24h) { use24h = it; prefs.use24h = it }
                SwitchRow(stringResource(R.string.seconds), null, seconds) { seconds = it; prefs.seconds = it }
                SwitchRow(stringResource(R.string.night_tint), stringResource(R.string.night_tint_hint), nightTint) { nightTint = it; prefs.nightTint = it }
                SwitchRow(stringResource(R.string.show_notices), null, showNotices) { showNotices = it; prefs.showNotices = it }
                Text(stringResource(R.string.brightness) + " · ${brightness.toInt()}%", fontSize = 15.sp)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    onValueChangeFinished = { prefs.brightness = brightness.toInt() },
                    valueRange = 5f..100f,
                )
                if (hidden.isNotEmpty()) {
                    OutlinedButton(onClick = { hidden = emptySet(); prefs.hiddenApps = emptySet() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.hidden_apps_reset, hidden.size))
                    }
                }
            }

            UpdatesSection(onOpen)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun permissionText(kind: PermissionKind): Pair<String, String> = when (kind) {
    PermissionKind.NOTICE_ACCESS -> stringResource(R.string.perm_notice_access) to stringResource(R.string.perm_notice_access_hint)
    PermissionKind.OVERLAY -> stringResource(R.string.perm_overlay) to stringResource(R.string.perm_overlay_hint)
    PermissionKind.POST_NOTIFICATIONS -> stringResource(R.string.perm_post) to stringResource(R.string.perm_post_hint)
    PermissionKind.FULL_SCREEN -> stringResource(R.string.perm_fullscreen) to stringResource(R.string.perm_fullscreen_hint)
    PermissionKind.BATTERY -> stringResource(R.string.perm_battery) to stringResource(R.string.perm_battery_hint)
}

@Composable
private fun UpdatesSection(onOpen: (Intent) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }

    Section(stringResource(R.string.sec_updates)) {
        Text(stringResource(R.string.version_fmt, BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (val s = state) {
            UpdateState.Idle -> {}
            UpdateState.Checking -> Text(stringResource(R.string.checking))
            UpdateState.Current -> Text(stringResource(R.string.up_to_date), color = Palette.Success)
            UpdateState.NoRelease -> Text(stringResource(R.string.no_releases))
            UpdateState.Failed -> Text(stringResource(R.string.update_failed), color = MaterialTheme.colorScheme.error)
            is UpdateState.Downloading -> Text(stringResource(R.string.downloading, s.percent))
            is UpdateState.Available -> {
                Text(stringResource(R.string.update_available, s.release.version.toString()), fontWeight = FontWeight.SemiBold)
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!Updater.canInstall(context)) {
                            onOpen(Updater.permissionIntent(context))
                            return@Button
                        }
                        state = UpdateState.Downloading(0)
                        scope.launch {
                            val apk = try {
                                withContext(Dispatchers.IO) {
                                    Updater.download(context, s.release) { state = UpdateState.Downloading((it * 100).toInt()) }
                                }
                            } catch (e: Exception) {
                                state = UpdateState.Failed
                                return@launch
                            }
                            state = UpdateState.Available(s.release)
                            Updater.install(context, apk)
                        }
                    },
                ) { Text(stringResource(if (Updater.canInstall(context)) R.string.download_install else R.string.allow_installs)) }
            }
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is UpdateState.Checking && state !is UpdateState.Downloading,
            onClick = {
                state = UpdateState.Checking
                scope.launch {
                    state = try {
                        val feed = withContext(Dispatchers.IO) { ReleaseFeed.latest(BuildConfig.UPDATE_REPO) }
                        val r = newerThan(BuildConfig.VERSION_NAME, feed)
                        when {
                            r != null -> UpdateState.Available(r)
                            feed == null -> UpdateState.NoRelease
                            else -> UpdateState.Current
                        }
                    } catch (e: Exception) {
                        UpdateState.Failed
                    }
                }
            },
        ) { Text(stringResource(R.string.check_updates)) }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp),
        )
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
        }
    }
}

@Composable
private fun SwitchRow(title: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 15.sp)
            if (hint != null) Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PermissionRow(title: String, hint: String, granted: Boolean, onGrant: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 15.sp)
            Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) {
            Text(stringResource(R.string.perm_on), color = Palette.Success, fontWeight = FontWeight.SemiBold)
        } else {
            TextButton(onClick = onGrant, colors = ButtonDefaults.textButtonColors()) { Text(stringResource(R.string.perm_allow)) }
        }
    }
}
