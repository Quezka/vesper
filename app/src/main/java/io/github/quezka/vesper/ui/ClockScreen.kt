package io.github.quezka.vesper.ui

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.quezka.vesper.R
import androidx.compose.ui.graphics.ImageBitmap
import io.github.quezka.vesper.data.AppIcons
import io.github.quezka.vesper.data.Prefs
import io.github.quezka.vesper.data.QuireLink
import io.github.quezka.vesper.domain.NextLine
import io.github.quezka.vesper.domain.QuireFeed
import io.github.quezka.vesper.domain.QuireView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.quezka.vesper.domain.AppDigest
import io.github.quezka.vesper.domain.NoticeItem
import io.github.quezka.vesper.domain.clockText
import io.github.quezka.vesper.domain.dateText
import io.github.quezka.vesper.domain.digest
import io.github.quezka.vesper.domain.meridiem
import io.github.quezka.vesper.domain.shortTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

private val CardBg = Color(0xFF18191C)
private val CardBorder = Color(0xFF2A2C31)
private val Muted = Color(0xFF9AA0AA)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClockScreen(
    prefs: Prefs,
    battery: BatteryState,
    notices: StateFlow<List<NoticeItem>>,
    onClose: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    val datePattern = remember { DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM") }
    val use24h = prefs.use24h
    val seconds = prefs.seconds

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(seconds) {
        while (true) {
            now = LocalDateTime.now()
            val intoStep = if (seconds) now.nano / 1_000_000L else now.second * 1000L + now.nano / 1_000_000L
            delay(((if (seconds) 1000L else 60_000L) - intoStep).coerceAtLeast(50L))
        }
    }

    // Quire's schedule, read from its provider every half minute while the clock is up.
    val quire by produceState<QuireFeed?>(null) {
        if (!prefs.showQuire) return@produceState
        while (true) {
            val ok = withContext(Dispatchers.IO) { QuireLink.read(context) } as? QuireLink.Result.Ok
            value = ok?.feed
            delay(if (ok != null) 30_000 else 5_000) // Quire may still be starting: ask again soon
        }
    }
    // The focus timer needs a tick every second while it runs.
    var focusNow by remember { mutableStateOf(System.currentTimeMillis()) }
    val focusRunning = quire?.focus != null
    LaunchedEffect(focusRunning) {
        while (focusRunning) {
            focusNow = System.currentTimeMillis()
            delay(1000)
        }
    }

    var hidden by remember { mutableStateOf(prefs.hiddenApps) }
    val items by notices.collectAsState()
    val rows = remember(items, hidden) { digest(items, hidden) }

    var controls by remember { mutableStateOf(false) }
    LaunchedEffect(controls) {
        if (controls) {
            delay(4000)
            controls = false
        }
    }

    val ink = if (prefs.nightTint) Color(0xFFB8602A) else Color(0xFFECEDEF)
    val soft = if (prefs.nightTint) Color(0xFF7A4020) else Muted
    // Shift the whole picture a few dp every few minutes so a static layout never burns into an OLED panel.
    val shift = ((now.minute / 3) % 5 - 2) * 3

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { controls = !controls } }
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        val screenHeight = maxHeight
        val landscape = maxWidth > screenHeight
        val clockWidth = if (landscape && prefs.showNotices) maxWidth * 0.55f else maxWidth
        val em = if (seconds) 4.4f else 2.8f
        val timeSize = (clockWidth.value / em).coerceAtMost(220f).sp

        val clock: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BatteryLine(battery, soft)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        clockText(now, use24h, seconds),
                        color = ink,
                        fontSize = timeSize,
                        fontWeight = FontWeight.Thin,
                        maxLines = 1,
                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                    )
                    meridiem(now, use24h)?.let {
                        Text(it, color = soft, fontSize = (timeSize.value / 6).sp, modifier = Modifier.padding(start = 6.dp, bottom = (timeSize.value / 8).dp))
                    }
                }
                Text(
                    dateText(now, locale, datePattern),
                    color = soft,
                    fontSize = (timeSize.value / 8).coerceIn(14f, 28f).sp,
                    textAlign = TextAlign.Center,
                )
                quire?.let { QuireLines(it, now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), focusNow, ink, soft, locale) }
            }
        }

        val list: @Composable (Modifier) -> Unit = { mod ->
            Column(mod.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rows.isEmpty()) {
                    Text(stringResource(R.string.no_notices), color = Color(0xFF50555E), fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                rows.forEach { row ->
                    NoticeCard(row, locale, onHide = {
                        hidden = hidden + row.packageName
                        prefs.hiddenApps = hidden
                    })
                }
            }
        }

        Box(Modifier.fillMaxSize().offset(shift.dp, (-shift).dp)) {
            if (landscape) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(if (prefs.showNotices) 1.15f else 1f), contentAlignment = Alignment.Center) { clock() }
                    if (prefs.showNotices) {
                        Spacer(Modifier.width(24.dp))
                        list(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { clock() }
                    if (prefs.showNotices) {
                        list(Modifier.fillMaxWidth().heightIn(max = screenHeight * 0.5f).padding(bottom = 8.dp))
                        if (rows.isNotEmpty()) {
                            Text(stringResource(R.string.hold_to_hide), color = Color(0xFF50555E), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }

        if (controls) {
            Row(Modifier.align(Alignment.TopEnd), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(stringResource(R.string.clock_settings), onSettings)
                Pill(stringResource(R.string.clock_close), onClose)
            }
        }
    }
}

/** Quire's part of the clock: the focus timer, what is on now or next, tomorrow's start and the open tasks. */
@Composable
private fun QuireLines(feed: QuireFeed, nowMs: Long, focusNowMs: Long, ink: Color, soft: Color, locale: Locale) {
    val zone = ZoneId.systemDefault()
    @Composable
    fun duration(minutes: Long): String {
        val (h, m) = QuireView.split(minutes)
        return when {
            h == 0L -> stringResource(R.string.dur_m, m)
            m == 0L -> stringResource(R.string.dur_h, h)
            else -> stringResource(R.string.dur_hm, h, m)
        }
    }
    val lines = mutableListOf<@Composable () -> Unit>()

    QuireView.focusLeftMs(feed, focusNowMs)?.let { left ->
        val secs = left / 1000
        val clock = "%d:%02d".format(secs / 60, secs % 60)
        val label = stringResource(if (feed.focus?.phase == "work") R.string.quire_focus else R.string.quire_break)
        lines += { Text("$label · $clock", color = ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
        feed.focus?.label?.let { l -> lines += { Text(l, color = soft, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
    }

    when (val n = QuireView.nextLine(feed, nowMs)) {
        is NextLine.Running -> {
            val until = shortTime(n.endMs, zone, locale)
            lines += { Text(stringResource(R.string.quire_now, n.title, until), color = ink, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        is NextLine.Soon -> {
            val at = shortTime(n.startMs, zone, locale)
            lines += { Text("${n.title} · $at", color = ink, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            lines += { Text(stringResource(R.string.quire_in, duration(n.minutes)), color = soft, fontSize = 13.sp) }
        }
        null -> {}
    }

    QuireView.tomorrow(feed, nowMs)?.let { t ->
        val at = shortTime(t.startMs, zone, locale)
        lines += { Text(stringResource(R.string.quire_tomorrow, at, t.title), color = soft, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }

    if (feed.tasksToday + feed.tasksTomorrow > 0) {
        lines += { Text(stringResource(R.string.quire_tasks, feed.tasksToday, feed.tasksTomorrow), color = soft, fontSize = 15.sp) }
        feed.taskTitle?.let { t -> lines += { Text(t, color = soft, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
    }

    if (lines.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            lines.forEach { it() }
        }
    }
}

@Composable
private fun BatteryLine(battery: BatteryState, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(8.dp).clip(CircleShape)
                .background(if (battery.charging) Palette.Success else Palette.Amber)
        )
        Spacer(Modifier.width(8.dp))
        val label = stringResource(if (battery.charging) R.string.battery_charging else R.string.battery_not_charging)
        Text("$label · ${battery.percent}%", color = color, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoticeCard(row: AppDigest, locale: Locale, onHide: () -> Unit) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, row.packageName) {
        value = withContext(Dispatchers.IO) { AppIcons.get(context, row.packageName) }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(14.dp))
            .combinedClickable(onClick = {}, onLongClick = onHide)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val bmp = icon
        if (bmp != null) {
            Image(bmp, contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)))
        } else {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF202226)))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.appName.uppercase(locale) + if (row.count > 1) "  ·  ${row.count}" else "",
                    color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text(shortTime(row.postedAt, ZoneId.systemDefault(), locale), color = Muted, fontSize = 11.sp)
            }
            if (row.title.isNotBlank()) {
                Text(row.title, color = Color(0xFFECEDEF), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            if (row.text.isNotBlank()) {
                Text(row.text, color = Color(0xFFB6BAC2), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Pill(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Color(0xFFECEDEF),
        fontSize = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF202226))
            .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
