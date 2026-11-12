package com.aulaf75.android

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.aulaf75.android.protocol.ColorTable
import com.aulaf75.android.protocol.ConfigRegion
import com.aulaf75.android.protocol.F75
import com.aulaf75.android.protocol.KeyAction
import com.aulaf75.android.protocol.KeyboardLayout
import com.aulaf75.android.protocol.Keymap
import com.aulaf75.android.protocol.Layer
import com.aulaf75.android.protocol.PerKeyColors
import com.aulaf75.android.ui.AppScreen
import com.aulaf75.android.ui.AulaTheme
import com.aulaf75.android.ui.Destination
import com.aulaf75.android.ui.NoActions
import com.aulaf75.android.ui.ThemeMode
import com.aulaf75.android.ui.UiState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real UI with simulated keyboard data. With -Droborazzi.test.record=true the PNGs are
 * written to docs/screenshots (used by the README); otherwise the tests only check that every
 * screen composes without crashing in every language.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = ScreenshotTest.PHONE)
class ScreenshotTest {
    companion object {
        // Resource qualifiers: locale first, then size/orientation/density (Android order).
        const val PHONE = "w412dp-h915dp-port-xxhdpi"
        const val TABLET = "w1280dp-h800dp-land-xhdpi"
    }

    @get:Rule val compose = createComposeRule()

    private fun shot(name: String, state: UiState, dest: Destination, dark: Boolean = true) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AulaTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) { AppScreen(state, NoActions, initial = dest) }
        }
        compose.mainClock.advanceTimeBy(1_700)
        compose.onRoot().captureRoboImage("../docs/screenshots/$name.png")
    }

    private val ansi get() = Demo.connected(KeyboardLayout.ANSI)
    private val iso get() = Demo.connected(KeyboardLayout.ISO)

    // ------------------------------------------------------------------ phone

    @Test @Config(qualifiers = "en-$PHONE") fun phone_overview_en() = shot("phone-overview-en", ansi, Destination.OVERVIEW)
    @Test @Config(qualifiers = "en-$PHONE") fun phone_lighting_en() = shot("phone-lighting-en", ansi, Destination.LIGHTING)
    @Test @Config(qualifiers = "en-$PHONE") fun phone_keycolors_en() = shot("phone-keycolors-en", Demo.connected(KeyboardLayout.ANSI, custom = true), Destination.PER_KEY)
    @Test @Config(qualifiers = "en-$PHONE") fun phone_mapping_en() = shot("phone-mapping-en", ansi, Destination.MAPPING)
    @Test @Config(qualifiers = "en-$PHONE") fun phone_settings_en() = shot("phone-settings-en", ansi, Destination.SETTINGS)
    @Test @Config(qualifiers = "en-$PHONE") fun phone_connect_en() = shot("phone-connect-en", UiState(versionName = "0.2"), Destination.OVERVIEW)
    @Test @Config(qualifiers = "en-$PHONE") fun phone_overview_light_en() = shot("phone-overview-light-en", ansi, Destination.OVERVIEW, dark = false)
    @Test @Config(qualifiers = "tr-$PHONE") fun phone_overview_tr() = shot("phone-overview-tr", iso, Destination.OVERVIEW)
    @Test @Config(qualifiers = "tr-$PHONE") fun phone_mapping_tr() = shot("phone-mapping-tr", iso, Destination.MAPPING)
    @Test @Config(qualifiers = "tr-$PHONE") fun phone_lighting_tr() = shot("phone-lighting-tr", iso, Destination.LIGHTING)
    @Test @Config(qualifiers = "zh-rCN-$PHONE") fun phone_lighting_zh() = shot("phone-lighting-zh", ansi, Destination.LIGHTING)
    @Test @Config(qualifiers = "zh-rCN-$PHONE") fun phone_settings_zh() = shot("phone-settings-zh", ansi, Destination.SETTINGS)

    // ----------------------------------------------------------------- tablet

    @Test @Config(qualifiers = "en-$TABLET") fun tablet_overview_en() = shot("tablet-overview-en", ansi, Destination.OVERVIEW)
    @Test @Config(qualifiers = "en-$TABLET") fun tablet_lighting_en() = shot("tablet-lighting-en", ansi, Destination.LIGHTING)
    @Test @Config(qualifiers = "en-$TABLET") fun tablet_keycolors_en() = shot("tablet-keycolors-en", Demo.connected(KeyboardLayout.ANSI, custom = true), Destination.PER_KEY)
    @Test @Config(qualifiers = "en-$TABLET") fun tablet_mapping_en() = shot("tablet-mapping-en", ansi, Destination.MAPPING)
    @Test @Config(qualifiers = "en-$TABLET") fun tablet_settings_en() = shot("tablet-settings-en", ansi, Destination.SETTINGS)
    @Test @Config(qualifiers = "tr-$TABLET") fun tablet_mapping_tr() = shot("tablet-mapping-tr", iso, Destination.MAPPING)
    @Test @Config(qualifiers = "en-$TABLET") fun tablet_connect_en() = shot("tablet-connect-en", UiState(versionName = "0.2"), Destination.OVERVIEW)
}

/** Simulated keyboard banks shaped like real F75 reads (no hardware involved). */
object Demo {
    fun connected(layout: KeyboardLayout, custom: Boolean = false) = UiState(
        connected = true,
        identity = "03 00 00 00 00 CD",
        experimental = true,
        versionName = "0.2",
        data = data(layout, custom),
    )

    fun data(layout: KeyboardLayout, custom: Boolean): KeyboardData {
        val cfg = ByteArray(128).also {
            it[9] = if (custom) 1 else 0
            it[10] = (if (custom) F75.CUSTOM_EFFECT else 13).toByte()
            it[24] = 10
            it[126] = 0x5a; it[127] = 0xa5.toByte()
            for (e in 0..21) { it[56 + e * 2] = 7; it[57 + e * 2] = 0x27 }
        }
        val pal = ByteArray(512).also {
            it[506] = 0x5a; it[507] = 0xa5.toByte()
            for (e in 1..18) { it[e * 21] = 0x8b.toByte(); it[e * 21 + 1] = 0x7c; it[e * 21 + 2] = 0xff.toByte() }
        }
        val per = ByteArray(378)
        for (k in layout.keys) {
            val c = android.graphics.Color.HSVToColor(floatArrayOf((k.x / 16f) * 300f + 200f, 0.75f, 1f))
            per[k.matrixIndex] = (c shr 16 and 0xff).toByte()
            per[126 + k.matrixIndex] = (c shr 8 and 0xff).toByte()
            per[252 + k.matrixIndex] = (c and 0xff).toByte()
        }
        return KeyboardData(
            ConfigRegion.parse(cfg), ColorTable.parse(pal),
            keymap(layout, Layer.BASE), keymap(layout, Layer.FN), PerKeyColors.parse(per), layout,
        )
    }

    private fun keymap(layout: KeyboardLayout, layer: Layer): Keymap {
        val b = ByteArray(520)
        byteArrayOf(6, 3, layer.byte.toByte(), 0, 1, 0, 0, 2).copyInto(b)
        b[518] = 0x5a; b[519] = 0xa5.toByte()
        for (k in layout.keys) {
            val u = k.usage ?: continue
            val action = if (layer == Layer.FN) when (u) {
                0x2c -> KeyAction(2, 0xcd); 0x33 -> KeyAction(2, 0xea); 0x34 -> KeyAction(2, 0xe9)
                else -> KeyAction(0, u)
            } else KeyAction(0, u)
            runCatching { action.record() }.getOrNull()?.copyInto(b, 8 + 4 * k.matrixIndex)
        }
        val fnSlot = if (layout == KeyboardLayout.ISO) F75.ISO_FN_MATRIX_INDEX else F75.FN_MATRIX_INDEX
        byteArrayOf(13, 0, 0, 0).copyInto(b, 8 + 4 * fnSlot)
        if (layer == Layer.FN) byteArrayOf(9, 1, 0, 0).copyInto(b, 8 + 4 * 9) // a vendor action, like Fn+A on real boards
        return Keymap.parse(b)
    }
}
