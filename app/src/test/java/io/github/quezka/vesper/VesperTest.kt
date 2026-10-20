package io.github.quezka.vesper

import io.github.quezka.vesper.data.ReleaseFeed
import io.github.quezka.vesper.domain.NextLine
import io.github.quezka.vesper.domain.NoticeItem
import io.github.quezka.vesper.domain.QuireFeed
import io.github.quezka.vesper.domain.QuireView
import io.github.quezka.vesper.domain.Release
import io.github.quezka.vesper.domain.Version
import io.github.quezka.vesper.domain.clockText
import io.github.quezka.vesper.domain.dateText
import io.github.quezka.vesper.domain.digest
import io.github.quezka.vesper.domain.fallbackLabel
import io.github.quezka.vesper.domain.meridiem
import io.github.quezka.vesper.domain.newerThan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.util.Locale

class VesperTest {
    private fun item(pkg: String, at: Long, title: String = "t", text: String = "x", ongoing: Boolean = false, summary: Boolean = false) =
        NoticeItem("$pkg$at", pkg, pkg.uppercase(), title, text, at, ongoing, summary)

    @Test fun versionsCompareNumerically() {
        assertTrue(Version.parse("v0.10.0")!! > Version.parse("0.9.9")!!)
        assertEquals(0, Version.parse("1.0")!!.compareTo(Version.parse("1.0.0")!!))
        assertNull(Version.parse("nightly"))
        assertEquals("0.2.1", Version.parse("v0.2.1-beta")!!.toString())
    }

    @Test fun onlyNewerReleasesOffered() {
        val r = Release(Version.parse("0.2.0")!!, "", "u", 1)
        assertNotNull(newerThan("0.1.0", r))
        assertNull(newerThan("0.2.0", r))
        assertNull(newerThan("0.1.0", null))
    }

    @Test fun releaseJsonPicksTheApk() {
        val json = """{"tag_name":"v0.3.0","body":"notes","assets":[
            {"name":"vesper.sha256","browser_download_url":"http://x/s","size":1},
            {"name":"Vesper-0.3.0.apk","browser_download_url":"http://x/a.apk","size":42}]}"""
        val r = ReleaseFeed.parse(json)!!
        assertEquals("0.3.0", r.version.toString())
        assertEquals("http://x/a.apk", r.apkUrl)
        assertEquals(42L, r.apkSize)
        assertNull(ReleaseFeed.parse("""{"tag_name":"v0.3.0","assets":[]}"""))
    }

    @Test fun digestKeepsOneRowPerAppNewestFirst() {
        val rows = digest(listOf(item("a", 1), item("a", 5), item("b", 3)), emptySet())
        assertEquals(listOf("a", "b"), rows.map { it.packageName })
        assertEquals(2, rows[0].count)
        assertEquals(5L, rows[0].postedAt)
    }

    @Test fun digestSkipsOngoingSummariesHiddenAndEmpty() {
        val rows = digest(
            listOf(
                item("music", 9, ongoing = true), item("chat", 8, summary = true), item("spam", 7),
                item("blank", 6, title = "", text = ""), item("mail", 1),
            ),
            hidden = setOf("spam"),
        )
        assertEquals(listOf("mail"), rows.map { it.packageName })
    }

    @Test fun digestIsLimited() {
        val many = (1..40).map { item("app$it", it.toLong()) }
        assertEquals(30, digest(many, emptySet()).size)
        assertEquals(3, digest(many, emptySet(), limit = 3).size)
        assertEquals("app40", digest(many, emptySet()).first().packageName)
    }

    @Test fun unnamedPackagesGetReadableLabels() {
        assertEquals("Youtube", fallbackLabel("com.google.android.youtube"))
        assertEquals("Chat app", fallbackLabel("org.example.chat_app"))
        assertEquals("single", fallbackLabel("single").lowercase())
    }

    private val minute = 60_000L
    private val row = mapOf<String, Any?>(
        "generated_ms" to 1_000L, "next_title" to "Matematica", "next_kind" to "class",
        "next_start_ms" to 1_000L + 90 * minute, "next_end_ms" to 1_000L + 150 * minute,
        "tomorrow_title" to "Fisica", "tomorrow_kind" to "class", "tomorrow_start_ms" to 20_000_000L,
        "tasks_today" to 2L, "tasks_tomorrow" to 1L, "task_title" to "Essay", "task_due_epoch_day" to 20_000L,
        "focus_phase" to "work", "focus_ends_ms" to 5_000_000L, "focus_label" to "Study",
    )

    @Test fun quireRowIsParsed() {
        val f = QuireFeed.fromRow(row)
        assertEquals("Matematica", f.next?.title)
        assertEquals(2, f.tasksToday)
        assertEquals("Study", f.focus?.label)
        assertEquals("Fisica", f.tomorrow?.title)
    }

    @Test fun anEmptyQuireRowGivesAnEmptyFeed() {
        val f = QuireFeed.fromRow(mapOf("generated_ms" to 5L, "next_title" to null, "focus_phase" to null, "focus_ends_ms" to null))
        assertNull(f.next)
        assertNull(f.tomorrow)
        assertNull(f.focus)
        assertEquals(0, f.tasksToday)
    }

    @Test fun nextLineCountsDownAndRunsOut() {
        val f = QuireFeed.fromRow(row)
        val soon = QuireView.nextLine(f, 1_000L) as NextLine.Soon
        assertEquals(90L, soon.minutes)
        assertEquals(30L, (QuireView.nextLine(f, 1_000L + 60 * minute) as NextLine.Soon).minutes)
        assertTrue(QuireView.nextLine(f, 1_000L + 100 * minute) is NextLine.Running)
        assertNull(QuireView.nextLine(f, 1_000L + 151 * minute))
    }

    @Test fun nextLineWaitsUntilItIsWithinTwelveHours() {
        val far = QuireFeed.fromRow(row + mapOf("next_start_ms" to 1_000L + 13 * 60 * minute, "next_end_ms" to 1_000L + 14 * 60 * minute))
        assertNull(QuireView.nextLine(far, 1_000L))
    }

    @Test fun focusLeftOnlyWhileItRuns() {
        val f = QuireFeed.fromRow(row)
        assertEquals(4_000_000L, QuireView.focusLeftMs(f, 1_000_000L))
        assertNull(QuireView.focusLeftMs(f, 5_000_001L))
    }

    @Test fun tomorrowIsShownWhileAhead() {
        val f = QuireFeed.fromRow(row)
        assertEquals("Fisica", QuireView.tomorrow(f, 1_000L)?.title)
        assertNull(QuireView.tomorrow(f, 20_000_001L))
        assertEquals(2L to 5L, QuireView.split(125))
    }

    @Test fun clockTextFormats() {
        val t = LocalDateTime.of(2026, 10, 20, 7, 5, 9)
        assertEquals("07:05", clockText(t, true, false))
        assertEquals("07:05:09", clockText(t, true, true))
        assertEquals("7:05", clockText(t, false, false))
        assertEquals("12:30", clockText(LocalDateTime.of(2026, 1, 1, 0, 30), false, false))
        assertEquals("AM", meridiem(t, false))
        assertEquals("PM", meridiem(t.withHour(15), false))
        assertNull(meridiem(t, true))
    }

    @Test fun dateIsCapitalisedInTheLocale() {
        val t = LocalDateTime.of(2026, 10, 20, 7, 5)
        assertEquals("Tuesday, 20 October", dateText(t, Locale.ENGLISH, "EEEE, d MMMM"))
    }

    /** Clean layering: the domain package knows nothing about Android. */
    @Test fun domainHasNoAndroidImports() {
        val dir = File("src/main/java/io/github/quezka/vesper/domain")
        val files = dir.listFiles { f -> f.extension == "kt" }.orEmpty()
        assertTrue(files.isNotEmpty())
        for (f in files) {
            assertFalse("${f.name} imports android", f.readText().lineSequence().any { it.startsWith("import android") || it.startsWith("import androidx") })
        }
    }

    @Test fun everyEnglishStringIsTranslated() {
        fun names(path: String) = Regex("""<string name="(\w+)"""").findAll(File(path).readText()).map { it.groupValues[1] }.toSet()
        val en = names("src/main/res/values/strings.xml")
        for (lang in listOf("ru", "it")) {
            assertEquals("missing in $lang", emptySet<String>(), en - names("src/main/res/values-$lang/strings.xml"))
        }
    }
}
