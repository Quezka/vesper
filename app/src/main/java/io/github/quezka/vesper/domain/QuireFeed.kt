package io.github.quezka.vesper.domain

/** The next thing on Quire's timeline (a class, an event or a work shift), with absolute times. */
data class Upcoming(val title: String, val kind: String, val startMs: Long, val endMs: Long)

/** Quire's focus timer while it runs; [label] is the task or project being focused on. */
data class FocusInfo(val phase: String, val endsMs: Long, val label: String?)

/** What Quire's bedside provider hands over: see Quire's `docs/bedside.md`. Never carries grades. */
data class QuireFeed(
    val generatedMs: Long,
    val next: Upcoming?,
    val tomorrow: Upcoming?,
    val tasksToday: Int,
    val tasksTomorrow: Int,
    val taskTitle: String?,
    val focus: FocusInfo?,
) {
    companion object {
        /** Builds the feed from one provider row (column name to value). */
        fun fromRow(row: Map<String, Any?>): QuireFeed {
            fun s(k: String) = (row[k] as? String)?.takeIf { it.isNotBlank() }
            fun l(k: String) = (row[k] as? Number)?.toLong()
            fun upcoming(prefix: String): Upcoming? {
                val title = s("${prefix}_title") ?: return null
                val start = l("${prefix}_start_ms") ?: return null
                return Upcoming(title, s("${prefix}_kind") ?: "event", start, l("${prefix}_end_ms") ?: start)
            }
            val ends = l("focus_ends_ms")
            return QuireFeed(
                generatedMs = l("generated_ms") ?: 0L,
                next = upcoming("next"),
                tomorrow = upcoming("tomorrow"),
                tasksToday = (l("tasks_today") ?: 0L).toInt(),
                tasksTomorrow = (l("tasks_tomorrow") ?: 0L).toInt(),
                taskTitle = s("task_title"),
                focus = if (ends != null && s("focus_phase") != null) FocusInfo(s("focus_phase")!!, ends, s("focus_label")) else null,
            )
        }
    }
}

/** What the "next" line of the clock says right now. */
sealed interface NextLine {
    /** Something is on now and ends at [endMs]. */
    data class Running(val title: String, val endMs: Long) : NextLine

    /** Something starts at [startMs], in [minutes] minutes. */
    data class Soon(val title: String, val startMs: Long, val minutes: Long) : NextLine
}

object QuireView {
    const val HORIZON_MS = 12L * 60 * 60 * 1000
    private const val MINUTE = 60_000L

    /** Works out the "next" line at [nowMs], so the countdown stays right between two reads of the provider. */
    fun nextLine(feed: QuireFeed, nowMs: Long): NextLine? {
        val n = feed.next ?: return null
        return when {
            n.endMs <= nowMs -> null
            n.startMs <= nowMs -> NextLine.Running(n.title, n.endMs)
            n.startMs - nowMs <= HORIZON_MS -> NextLine.Soon(n.title, n.startMs, ceilMinutes(n.startMs - nowMs))
            else -> null
        }
    }

    /** Tomorrow's first start, as long as it is still ahead of us. */
    fun tomorrow(feed: QuireFeed, nowMs: Long): Upcoming? = feed.tomorrow?.takeIf { it.startMs > nowMs }

    /** Milliseconds left on the running focus timer, or null when it is not running or has ended. */
    fun focusLeftMs(feed: QuireFeed, nowMs: Long): Long? = feed.focus?.let { it.endsMs - nowMs }?.takeIf { it > 0 }

    /** "2 h 5 min" parts: hours and minutes of a number of minutes. */
    fun split(minutes: Long): Pair<Long, Long> = minutes / 60 to minutes % 60

    private fun ceilMinutes(ms: Long) = (ms + MINUTE - 1) / MINUTE
}
