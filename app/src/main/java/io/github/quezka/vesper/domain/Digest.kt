package io.github.quezka.vesper.domain

/** One posted notification, reduced to what the charging screen shows. */
data class NoticeItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val ongoing: Boolean = false,
    val groupSummary: Boolean = false,
)

/** What one app contributes to the screen: how many notices and the newest one. */
data class AppDigest(
    val packageName: String,
    val appName: String,
    val count: Int,
    val title: String,
    val text: String,
    val postedAt: Long,
)

/**
 * Notices worth showing: no ongoing ones (media players, downloads), no group summaries, nothing from hidden apps,
 * nothing without text. One row per app, newest first, at most [limit] rows.
 */
fun digest(items: List<NoticeItem>, hidden: Set<String>, limit: Int = 5): List<AppDigest> =
    items.asSequence()
        .filter { !it.ongoing && !it.groupSummary && it.packageName !in hidden && (it.title.isNotBlank() || it.text.isNotBlank()) }
        .groupBy { it.packageName }
        .map { (pkg, list) ->
            val newest = list.maxBy { it.postedAt }
            AppDigest(pkg, newest.appName, list.size, newest.title, newest.text, newest.postedAt)
        }
        .sortedByDescending { it.postedAt }
        .take(limit)
