package io.github.quezka.vesper

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextStyle
import io.github.quezka.vesper.data.Prefs
import io.github.quezka.vesper.ui.BatteryState
import io.github.quezka.vesper.ui.ClockScreen
import io.github.quezka.vesper.ui.Manrope
import io.github.quezka.vesper.ui.SettingsScreen
import io.github.quezka.vesper.ui.VesperTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The screens draw; with SCREENSHOTS=1 they also land in build/screenshots. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xxhdpi", application = android.app.Application::class)
class ScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun shoot(name: String) {
        if (System.getenv("SCREENSHOTS") == null) return
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        File("build/screenshots").apply { mkdirs() }.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun settings() {
        compose.setContent {
            VesperTheme { SettingsScreen(Prefs(compose.activity), emptyList(), {}, {}, {}, {}, {}) }
        }
        compose.waitForIdle()
        shoot("settings")
    }

    @Test fun clock() {
        compose.mainClock.autoAdvance = false  // it ticks for ever
        compose.setContent {
            ProvideTextStyle(TextStyle(fontFamily = Manrope)) {
                ClockScreen(Prefs(compose.activity), BatteryState(82, true), MutableStateFlow(emptyList()), {}, {})
            }
        }
        compose.mainClock.advanceTimeBy(1000)
        shoot("clock")
    }
}
