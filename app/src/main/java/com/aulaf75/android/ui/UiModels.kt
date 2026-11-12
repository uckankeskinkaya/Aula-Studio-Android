package com.aulaf75.android.ui

import androidx.annotation.StringRes
import com.aulaf75.android.BackupEntry
import com.aulaf75.android.KeyboardData
import com.aulaf75.android.protocol.ColorTable
import com.aulaf75.android.protocol.ConfigRegion
import com.aulaf75.android.protocol.F75Error
import com.aulaf75.android.protocol.KeyAction
import com.aulaf75.android.protocol.Layer
import com.aulaf75.android.protocol.Rgb
import com.aulaf75.android.usb.F75Usb

/** Text resolved by the UI in the current language. */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Error(val error: F75Error, val detail: String? = null) : UiText
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UiState(
    val connected: Boolean = false,
    val busy: Boolean = false,
    val busyText: UiText? = null,
    val error: UiText? = null,
    val message: UiText? = null,
    val identity: String? = null,
    val data: KeyboardData? = null,
    val experimental: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** BCP-47 tag of the in-app language, or "" to follow the system. */
    val language: String = "",
    val backups: List<BackupEntry> = emptyList(),
    val usbDevices: List<F75Usb.UsbEntry>? = null,
    val versionName: String = "",
)

/** Everything the UI can ask for. Implemented by MainViewModel; screenshot tests use no-ops. */
interface AppActions {
    fun connect()
    fun reread()
    fun scanUsb()
    fun dismissMessages()
    fun setExperimental(on: Boolean)
    fun setThemeMode(mode: ThemeMode)
    fun setLanguage(tag: String)
    fun applyBrightness(value: Int)
    fun applyLighting(config: ConfigRegion, palette: ColorTable)
    fun applySleep(seconds: Int)
    fun applyRemap(layer: Layer, changes: Map<Int, KeyAction>)
    fun applyPerKey(changes: Map<Int, Rgb>)
    fun restore(entry: BackupEntry)
    fun deleteBackup(entry: BackupEntry)
    fun shareBackup(entry: BackupEntry)
    fun openUrl(url: String)
}

object NoActions : AppActions {
    override fun connect() {}
    override fun reread() {}
    override fun scanUsb() {}
    override fun dismissMessages() {}
    override fun setExperimental(on: Boolean) {}
    override fun setThemeMode(mode: ThemeMode) {}
    override fun setLanguage(tag: String) {}
    override fun applyBrightness(value: Int) {}
    override fun applyLighting(config: ConfigRegion, palette: ColorTable) {}
    override fun applySleep(seconds: Int) {}
    override fun applyRemap(layer: Layer, changes: Map<Int, KeyAction>) {}
    override fun applyPerKey(changes: Map<Int, Rgb>) {}
    override fun restore(entry: BackupEntry) {}
    override fun deleteBackup(entry: BackupEntry) {}
    override fun shareBackup(entry: BackupEntry) {}
    override fun openUrl(url: String) {}
}
