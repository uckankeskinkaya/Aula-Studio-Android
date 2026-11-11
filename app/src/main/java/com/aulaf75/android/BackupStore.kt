package com.aulaf75.android

import android.content.Context
import com.aulaf75.android.protocol.ColorTable
import com.aulaf75.android.protocol.ConfigRegion
import com.aulaf75.android.protocol.Keymap
import com.aulaf75.android.protocol.PerKeyColors
import com.aulaf75.android.protocol.hexCompact
import com.aulaf75.android.protocol.unhex
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Klavyenin ham bölgelerini JSON (hex) olarak saklar. */
class BackupStore(context: Context) {
    private val dir = File(context.filesDir, "backups").apply { mkdirs() }

    fun list(): List<File> = dir.listFiles { f -> f.extension == "json" }?.sortedByDescending { it.name } ?: emptyList()

    fun firstBackup(): File = File(dir, "0000-ilk-yedek.json")

    fun saveFirstIfMissing(data: KeyboardData) {
        if (!firstBackup().exists()) firstBackup().writeText(toJson(data, "İlk bağlantıdaki orijinal ayarlar"))
    }

    fun save(label: String, data: KeyboardData): File {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val safe = label.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val f = File(dir, "$stamp-$safe.json")
        f.writeText(toJson(data, label))
        // En fazla 40 otomatik yedek tut; ilk yedek asla silinmez.
        list().filter { it.name != firstBackup().name }.drop(40).forEach { it.delete() }
        return f
    }

    fun load(file: File): KeyboardData = fromJson(file.readText())

    fun delete(file: File) { if (file.name != firstBackup().name) file.delete() }

    companion object {
        fun toJson(d: KeyboardData, label: String): String = JSONObject().apply {
            put("format", "aula-f75-android-backup-1")
            put("label", label)
            put("created", System.currentTimeMillis())
            put("config", d.config.bytes.hexCompact())
            put("palette", d.palette.bytes.hexCompact())
            d.base?.let { put("keymapBase", it.bytes.hexCompact()) }
            d.fn?.let { put("keymapFn", it.bytes.hexCompact()) }
            d.perKey?.let { put("perKey", it.bytes.hexCompact()) }
        }.toString(2)

        fun fromJson(text: String): KeyboardData {
            val o = JSONObject(text)
            require(o.optString("format") == "aula-f75-android-backup-1") { "Bilinmeyen yedek biçimi" }
            return KeyboardData(
                config = ConfigRegion.parse(o.getString("config").unhex()),
                palette = ColorTable.parse(o.getString("palette").unhex()),
                base = o.optString("keymapBase").takeIf { it.isNotEmpty() }?.let { Keymap.parse(it.unhex()) },
                fn = o.optString("keymapFn").takeIf { it.isNotEmpty() }?.let { Keymap.parse(it.unhex()) },
                perKey = o.optString("perKey").takeIf { it.isNotEmpty() }?.let { PerKeyColors.parse(it.unhex()) },
                ansiError = null,
                readErrors = emptyList(),
            )
        }
    }
}
