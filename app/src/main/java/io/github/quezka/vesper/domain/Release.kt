package io.github.quezka.vesper.domain

/** A published build: its version and where its APK can be downloaded. */
data class Release(val version: Version, val notes: String, val apkUrl: String, val apkSize: Long)

fun newerThan(current: String, latest: Release?): Release? {
    val cur = Version.parse(current) ?: return null
    return latest?.takeIf { it.version > cur }
}
