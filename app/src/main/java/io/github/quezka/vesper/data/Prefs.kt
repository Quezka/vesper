package io.github.quezka.vesper.data

import android.content.Context
import androidx.core.content.edit

/** Every setting in one place, in plain SharedPreferences. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("vesper", Context.MODE_PRIVATE)

    var autoStart: Boolean
        get() = sp.getBoolean("autoStart", true)
        set(v) = sp.edit { putBoolean("autoStart", v) }

    var closeOnUnplug: Boolean
        get() = sp.getBoolean("closeOnUnplug", true)
        set(v) = sp.edit { putBoolean("closeOnUnplug", v) }

    var use24h: Boolean
        get() = sp.getBoolean("use24h", true)
        set(v) = sp.edit { putBoolean("use24h", v) }

    var seconds: Boolean
        get() = sp.getBoolean("seconds", false)
        set(v) = sp.edit { putBoolean("seconds", v) }

    var nightTint: Boolean
        get() = sp.getBoolean("nightTint", false)
        set(v) = sp.edit { putBoolean("nightTint", v) }

    /** Show Quire's schedule (next class or shift, tomorrow's start, tasks, focus timer) on the clock. */
    var showQuire: Boolean
        get() = sp.getBoolean("showQuire", true)
        set(v) = sp.edit { putBoolean("showQuire", v) }

    var showNotices: Boolean
        get() = sp.getBoolean("showNotices", true)
        set(v) = sp.edit { putBoolean("showNotices", v) }

    /** Window brightness while the clock shows, 5..100 percent. */
    var brightness: Int
        get() = sp.getInt("brightness", 15)
        set(v) = sp.edit { putInt("brightness", v.coerceIn(5, 100)) }

    var hiddenApps: Set<String>
        get() = sp.getStringSet("hiddenApps", emptySet()) ?: emptySet()
        set(v) = sp.edit { putStringSet("hiddenApps", v) }
}
