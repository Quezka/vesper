package io.github.quezka.vesper

import io.github.quezka.vesper.data.ReleaseFeed
import io.github.quezka.vesper.domain.NoticeItem
import io.github.quezka.vesper.domain.Release
import io.github.quezka.vesper.domain.Version
import io.github.quezka.vesper.domain.clockText
import io.github.quezka.vesper.domain.dateText
import io.github.quezka.vesper.domain.digest
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
        val many = (1..9).map { item("app$it", it.toLong()) }
        assertEquals(5, digest(many, emptySet()).size)
        assertEquals("app9", digest(many, emptySet()).first().packageName)
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
