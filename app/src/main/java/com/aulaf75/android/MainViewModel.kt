package com.aulaf75.android

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aulaf75.android.protocol.ColorTable
import com.aulaf75.android.protocol.ConfigRegion
import com.aulaf75.android.protocol.F75
import com.aulaf75.android.protocol.F75Error
import com.aulaf75.android.protocol.KeyAction
import com.aulaf75.android.protocol.Layer
import com.aulaf75.android.protocol.ProtocolException
import com.aulaf75.android.protocol.Rgb
import com.aulaf75.android.protocol.hex
import com.aulaf75.android.ui.AppActions
import com.aulaf75.android.ui.ThemeMode
import com.aulaf75.android.ui.UiState
import com.aulaf75.android.ui.UiText
import com.aulaf75.android.usb.F75Connection
import com.aulaf75.android.usb.F75Usb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app), AppActions {
    private val usbManager = app.getSystemService(Context.USB_SERVICE) as UsbManager
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val backups = BackupStore(app)
    private val mutex = Mutex()
    private var connection: F75Connection? = null
    private var controller: KeyboardController? = null

    var ui by mutableStateOf(
        UiState(
            experimental = prefs.getBoolean("experimental", false),
            themeMode = runCatching { ThemeMode.valueOf(prefs.getString("theme", "SYSTEM")!!) }.getOrDefault(ThemeMode.SYSTEM),
            language = AppCompatDelegate.getApplicationLocales().toLanguageTags(),
            versionName = runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull() ?: "",
        )
    )
        private set

    private val permissionAction = app.packageName + ".USB_PERMISSION"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                permissionAction ->
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) connect()
                    else ui = ui.copy(error = UiText.Error(F75Error.PERMISSION_DENIED))
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    closeConnection()
                    ui = ui.copy(connected = false, data = null, identity = null)
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(permissionAction)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        refreshBackups()
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(receiver) }
        closeConnection()
    }

    private fun closeConnection() {
        connection?.close()
        connection = null
        controller = null
    }

    private fun refreshBackups() { ui = ui.copy(backups = backups.list()) }

    private fun errorOf(e: Throwable): UiText = when (e) {
        is ProtocolException -> UiText.Error(e.error, e.detail)
        is org.json.JSONException, is IllegalArgumentException -> UiText.Error(F75Error.BACKUP_INVALID, e.message)
        else -> UiText.Error(F75Error.TRANSFER_FAILED, e.message ?: e.toString())
    }

    // ------------------------------------------------------------ settings

    override fun dismissMessages() { ui = ui.copy(error = null, message = null) }

    override fun setExperimental(on: Boolean) {
        prefs.edit().putBoolean("experimental", on).apply()
        ui = ui.copy(experimental = on)
    }

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme", mode.name).apply()
        ui = ui.copy(themeMode = mode)
    }

    override fun setLanguage(tag: String) {
        ui = ui.copy(language = tag)
        // Recreates the activity in the new language (persisted by AppCompat on all API levels).
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    override fun scanUsb() {
        ui = ui.copy(usbDevices = runCatching { F75Usb.describeAll(usbManager) }.getOrDefault(emptyList()))
    }

    override fun openUrl(url: String) {
        val app = getApplication<Application>()
        runCatching {
            app.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun shareBackup(entry: BackupEntry) {
        val app = getApplication<Application>()
        val send = Intent(Intent.ACTION_SEND).setType("application/json")
            .putExtra(Intent.EXTRA_SUBJECT, entry.file.name)
            .putExtra(Intent.EXTRA_TEXT, entry.file.readText())
        runCatching { app.startActivity(Intent.createChooser(send, entry.file.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    override fun deleteBackup(entry: BackupEntry) { backups.delete(entry.file); refreshBackups() }

    // ---------------------------------------------------------- device I/O

    /** Runs one device operation at a time on a background thread. */
    private fun deviceOp(busyText: UiText, block: (KeyboardController, KeyboardData) -> Pair<KeyboardData?, UiText?>) {
        if (ui.busy) return
        val data = ui.data
        ui = ui.copy(busy = true, busyText = busyText, error = null, message = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                mutex.withLock {
                    runCatching {
                        val c = controller ?: throw ProtocolException(F75Error.NOT_CONNECTED)
                        block(c, data ?: throw ProtocolException(F75Error.NOT_CONNECTED))
                    }
                }
            }
            result.onSuccess { (newData, msg) ->
                ui = ui.copy(busy = false, data = newData ?: ui.data, message = msg)
            }.onFailure { e ->
                val transport = e is ProtocolException && e.error.transport
                if (transport) closeConnection()
                ui = ui.copy(busy = false, error = errorOf(e), connected = ui.connected && !transport)
                // A refused write leaves the keyboard unchanged; refresh so the next attempt starts from the truth.
                if (e is ProtocolException && e.error == F75Error.STALE_STATE) reread()
            }
            refreshBackups()
        }
    }

    override fun connect() {
        if (ui.busy) return
        scanUsb()
        val device = F75Usb.find(usbManager)
        if (device == null) {
            val none = ui.usbDevices.isNullOrEmpty()
            ui = ui.copy(error = UiText.Error(if (none) F75Error.NO_USB_DEVICES else F75Error.DEVICE_NOT_FOUND))
            return
        }
        if (!usbManager.hasPermission(device)) {
            val app = getApplication<Application>()
            val intent = Intent(permissionAction).setPackage(app.packageName)
            val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            usbManager.requestPermission(device, PendingIntent.getBroadcast(app, 0, intent, flags))
            return
        }
        ui = ui.copy(busy = true, busyText = UiText.Res(R.string.busy_connecting), error = null, message = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                mutex.withLock {
                    runCatching {
                        closeConnection()
                        val conn = F75Usb.open(usbManager, device)
                        try {
                            val c = KeyboardController(conn) { slug, d -> backups.save(BackupReason.fromSlug(slug), d) }
                            val id = c.readIdentity()
                            if (!id.contentEquals(F75.F75_IDENTITY)) throw ProtocolException(F75Error.IDENTITY_MISMATCH, id.hex())
                            val data = c.readAll()
                            backups.saveOriginalIfMissing(data)
                            backups.save(BackupReason.CONNECT, data)
                            connection = conn
                            controller = c
                            id to data
                        } catch (e: Throwable) {
                            conn.close()
                            throw e
                        }
                    }
                }
            }
            result.onSuccess { (id, data) ->
                ui = ui.copy(busy = false, connected = true, identity = id.hex(), data = data)
            }.onFailure { e ->
                ui = ui.copy(busy = false, connected = false, error = errorOf(e))
            }
            refreshBackups()
        }
    }

    override fun reread() = deviceOp(UiText.Res(R.string.busy_reading)) { c, _ -> c.readAll() to null }

    override fun applyBrightness(value: Int) = deviceOp(UiText.Res(R.string.busy_writing)) { c, d ->
        c.applyConfig(d, d.config.withPresetBrightness(value), d.palette, BackupReason.BRIGHTNESS.slug) to
            UiText.Res(R.string.msg_saved_verified)
    }

    override fun applyLighting(config: ConfigRegion, palette: ColorTable) = deviceOp(UiText.Res(R.string.busy_writing)) { c, d ->
        c.applyConfig(d, config, palette, BackupReason.LIGHTING.slug) to UiText.Res(R.string.msg_saved_verified)
    }

    override fun applySleep(seconds: Int) = deviceOp(UiText.Res(R.string.busy_writing)) { c, d ->
        c.applyConfig(d, d.config.withSleepSeconds(seconds), d.palette, BackupReason.SLEEP.slug) to
            UiText.Res(R.string.msg_saved_verified)
    }

    override fun applyRemap(layer: Layer, changes: Map<Int, KeyAction>) = deviceOp(UiText.Res(R.string.busy_writing)) { c, d ->
        c.applyRemap(d, layer, changes, BackupReason.REMAP.slug) to UiText.Res(R.string.msg_remap_saved, listOf(changes.size))
    }

    override fun applyPerKey(changes: Map<Int, Rgb>) = deviceOp(UiText.Res(R.string.busy_writing)) { c, d ->
        c.applyPerKey(d, changes, BackupReason.PER_KEY.slug) to UiText.Res(R.string.msg_perkey_saved)
    }

    override fun restore(entry: BackupEntry) = deviceOp(UiText.Res(R.string.busy_restoring)) { c, d ->
        val saved = backups.load(entry.file)
        backups.save(BackupReason.RESTORE, d)
        val (after, _) = c.restore(d, saved)
        after to UiText.Res(R.string.msg_restored)
    }
}
