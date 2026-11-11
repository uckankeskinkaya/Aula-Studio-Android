package com.aulaf75.android

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aulaf75.android.protocol.ColorTable
import com.aulaf75.android.protocol.ConfigRegion
import com.aulaf75.android.protocol.F75
import com.aulaf75.android.protocol.KeyAction
import com.aulaf75.android.protocol.Layer
import com.aulaf75.android.protocol.Rgb
import com.aulaf75.android.protocol.hex
import com.aulaf75.android.usb.F75Connection
import com.aulaf75.android.usb.F75Usb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class UiState(
    val connected: Boolean = false,
    val busy: Boolean = false,
    val status: String = "Klavye bağlı değil",
    val error: String? = null,
    val message: String? = null,
    val identity: String? = null,
    val data: KeyboardData? = null,
    val experimental: Boolean = false,
    val backups: List<File> = emptyList(),
    val usbDevices: List<String>? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val usbManager = app.getSystemService(Context.USB_SERVICE) as UsbManager
    private val prefs = app.getSharedPreferences("ayarlar", Context.MODE_PRIVATE)
    val backups = BackupStore(app)
    private val mutex = Mutex()
    private var connection: F75Connection? = null
    private var controller: KeyboardController? = null

    var ui by mutableStateOf(UiState(experimental = prefs.getBoolean("experimental", false)))
        private set

    private val permissionAction = app.packageName + ".USB_PERMISSION"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                permissionAction -> {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) connect()
                    else ui = ui.copy(error = "USB izni verilmedi.")
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    closeConnection()
                    ui = ui.copy(connected = false, status = "Klavye çıkarıldı", data = null)
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

    fun dismissMessages() { ui = ui.copy(error = null, message = null) }

    fun setExperimental(on: Boolean) {
        prefs.edit().putBoolean("experimental", on).apply()
        ui = ui.copy(experimental = on)
    }

    fun refreshBackups() { ui = ui.copy(backups = backups.list()) }

    /** Tüm cihaz işlemleri tek tek, arka planda çalışır. */
    private fun run(label: String, block: (KeyboardController) -> String?) {
        if (ui.busy) return
        ui = ui.copy(busy = true, error = null, message = null, status = label)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                mutex.withLock {
                    runCatching {
                        val c = controller ?: throw IOException("Önce klavyeye bağlan")
                        block(c)
                    }
                }
            }
            result.onSuccess { msg ->
                ui = ui.copy(busy = false, status = "Hazır", message = msg)
            }.onFailure { e ->
                if (e is IOException) {
                    closeConnection()
                    ui = ui.copy(connected = false)
                }
                ui = ui.copy(busy = false, status = if (ui.connected) "Hazır" else "Bağlantı yok", error = e.message ?: e.toString())
            }
            refreshBackups()
        }
    }

    private fun setData(d: KeyboardData) {
        viewModelScope.launch(Dispatchers.Main) { ui = ui.copy(data = d) }
    }

    fun scanUsb() {
        ui = ui.copy(usbDevices = runCatching { F75Usb.describeAll(usbManager) }.getOrElse { listOf("Tarama hatası: ${it.message}") })
    }

    fun connect() {
        if (ui.busy) return
        scanUsb()
        val device = F75Usb.find(usbManager)
        if (device == null) {
            val n = ui.usbDevices?.size ?: 0
            ui = ui.copy(
                error = if (n == 0)
                    "Tablet hiçbir USB cihazı görmüyor. Klavye büyük ihtimalle sadece şarj oluyor: klavyenin arkasındaki mod anahtarını " +
                        "KABLO (USB) konumuna al, kabloyu çıkarıp tak. Klavyeyle tablete yazı yazabiliyorsan bağlantı doğrudur."
                else
                    "AULA F75 bulunamadı. Tablet $n USB cihazı görüyor; aşağıdaki 'USB teşhis' listesini bana gönder."
            )
            return
        }
        if (!usbManager.hasPermission(device)) {
            val app = getApplication<Application>()
            val intent = Intent(permissionAction).setPackage(app.packageName)
            val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            usbManager.requestPermission(device, PendingIntent.getBroadcast(app, 0, intent, flags))
            ui = ui.copy(status = "USB izni bekleniyor…")
            return
        }
        ui = ui.copy(busy = true, error = null, message = null, status = "Bağlanıyor…")
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                mutex.withLock {
                    runCatching {
                        closeConnection()
                        val conn = F75Usb.open(usbManager, device)
                        val c = KeyboardController(conn) { label, d -> backups.save(label, d) }
                        val id = try { c.readIdentity() } catch (e: Exception) { conn.close(); throw e }
                        if (!id.contentEquals(F75.F75_IDENTITY)) {
                            conn.close()
                            throw IllegalStateException(
                                "Model kimliği ${id.hex()} orijinal ANSI F75 (${F75.F75_IDENTITY.hex()}) ile eşleşmiyor. " +
                                    "Güvenlik için hiçbir ayar okunmadı/yazılmadı."
                            )
                        }
                        val data = c.readAll()
                        backups.saveFirstIfMissing(data)
                        backups.save("baglanti", data)
                        connection = conn
                        controller = c
                        id to data
                    }
                }
            }
            result.onSuccess { (id, data) ->
                ui = ui.copy(busy = false, connected = true, identity = id.hex(), data = data, status = "Bağlı: AULA F75")
            }.onFailure { e ->
                ui = ui.copy(busy = false, connected = false, status = "Bağlantı yok", error = e.message ?: e.toString())
            }
            refreshBackups()
        }
    }

    fun reread() = run("Okunuyor…") { c -> setData(c.readAll()); "Klavyeden yeniden okundu." }

    fun applyBrightness(value: Int) = run("Parlaklık yazılıyor…") { c ->
        val d = ui.data ?: error("Veri yok")
        val after = c.applyConfig(d, d.config.withPresetBrightness(value), d.palette, "once-parlaklik")
        setData(after); "Parlaklık $value olarak yazıldı ve doğrulandı."
    }

    fun applyLighting(config: ConfigRegion, palette: ColorTable) = run("Işık ayarı yazılıyor…") { c ->
        val d = ui.data ?: error("Veri yok")
        val after = c.applyConfig(d, config, palette, "once-isik")
        setData(after); "Işık ayarı yazıldı ve doğrulandı."
    }

    fun applySleep(seconds: Int) = run("Uyku süresi yazılıyor…") { c ->
        val d = ui.data ?: error("Veri yok")
        val after = c.applyConfig(d, d.config.withSleepSeconds(seconds), d.palette, "once-uyku")
        setData(after); "Uyku süresi yazıldı ve doğrulandı."
    }

    fun applyRemap(layer: Layer, changes: Map<Int, KeyAction>) = run("Tuş ataması yazılıyor…") { c ->
        val d = ui.data ?: error("Veri yok")
        val after = c.applyRemap(d, layer, changes, "once-tus-atama")
        setData(after); "${changes.size} tuş ataması yazıldı ve doğrulandı."
    }

    fun applyPerKey(changes: Map<Int, Rgb>) = run("Tuş renkleri yazılıyor…") { c ->
        val d = ui.data ?: error("Veri yok")
        val after = c.applyPerKey(d, changes, "once-tus-rgb")
        setData(after); "Tuş renkleri yazıldı, klavye özel (tuş başına) moda alındı."
    }

    fun restore(file: File) = run("Yedek geri yükleniyor…") { c ->
        val d = ui.data ?: error("Veri yok")
        val saved = backups.load(file)
        backups.save("once-geri-yukleme", d)
        val (after, notes) = c.restore(d, saved)
        setData(after); notes.joinToString("\n")
    }

    fun deleteBackup(file: File) { backups.delete(file); refreshBackups() }
}
