package io.github.quezka.vesper.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** "07:05" or "7:05" in 24-hour style, "7:05" with [use24h] false (the AM/PM marker is returned apart). */
fun clockText(now: LocalDateTime, use24h: Boolean, seconds: Boolean): String {
    val hour = if (use24h) now.hour else (now.hour % 12).let { if (it == 0) 12 else it }
    val hh = if (use24h) "%02d".format(hour) else hour.toString()
    val base = "$hh:%02d".format(now.minute)
    return if (seconds) base + ":%02d".format(now.second) else base
}

fun meridiem(now: LocalDateTime, use24h: Boolean): String? =
    if (use24h) null else if (now.hour < 12) "AM" else "PM"

/** "Tuesday, 20 October" in the user's language; [pattern] comes from the platform's best pattern for the locale. */
fun dateText(now: LocalDateTime, locale: Locale, pattern: String): String =
    DateTimeFormatter.ofPattern(pattern, locale).format(now).replaceFirstChar { it.titlecase(locale) }

/** The same moment as a short time for a notification row. */
fun shortTime(epochMs: Long, zone: java.time.ZoneId, locale: Locale): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        .format(java.time.Instant.ofEpochMilli(epochMs).atZone(zone))
