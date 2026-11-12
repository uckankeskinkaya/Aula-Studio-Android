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

/** Why a backup was taken; stored in the file name and localized by the UI. */
enum class BackupReason(val slug: String) {
    ORIGINAL("original"), CONNECT("connect"), BRIGHTNESS("brightness"), LIGHTING("lighting"),
    SLEEP("sleep"), REMAP("remap"), PER_KEY("per-key"), RESTORE("restore"), UNKNOWN("unknown");

    companion object {
        fun fromSlug(s: String) = entries.firstOrNull { it.slug == s }
            // file names written by v0.1.x
            ?: when (s) {
                "baglanti" -> CONNECT; "once-parlaklik" -> BRIGHTNESS; "once-isik" -> LIGHTING; "once-uyku" -> SLEEP
                "once-tus-atama" -> REMAP; "once-tus-rgb" -> PER_KEY; "once-geri-yukleme" -> RESTORE
                else -> UNKNOWN
            }
    }
}

data class BackupEntry(val file: File, val reason: BackupReason, val timeMillis: Long?) {
    val isOriginal: Boolean get() = reason == BackupReason.ORIGINAL
}

/** Stores raw keyboard banks as hex JSON in app-private storage. The original snapshot is never deleted. */
class BackupStore(context: Context) {
    private val dir = File(context.filesDir, "backups").apply { mkdirs() }
    private val stampFormat get() = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    private fun originalFile(): File = File(dir, "0000-ilk-yedek.json") // name kept for v0.1 compatibility

    fun list(): List<BackupEntry> = (dir.listFiles { f -> f.extension == "json" } ?: emptyArray())
        .sortedByDescending { it.name }
        .map { f ->
            if (f.name == originalFile().name) BackupEntry(f, BackupReason.ORIGINAL, f.lastModified())
            else {
                val m = Regex("""(\d{8}-\d{6})-(.*)""").matchEntire(f.nameWithoutExtension)
                BackupEntry(
                    f,
                    BackupReason.fromSlug(m?.groupValues?.get(2) ?: ""),
                    m?.groupValues?.get(1)?.let { runCatching { stampFormat.parse(it)?.time }.getOrNull() },
                )
            }
        }

    fun saveOriginalIfMissing(data: KeyboardData) {
        if (!originalFile().exists()) originalFile().writeText(toJson(data, BackupReason.ORIGINAL))
    }

    fun save(reason: BackupReason, data: KeyboardData): File {
        val f = File(dir, "${stampFormat.format(Date())}-${reason.slug}.json")
        f.writeText(toJson(data, reason))
        // Keep the 40 most recent automatic backups; the original is never pruned.
        list().filter { !it.isOriginal }.drop(40).forEach { it.file.delete() }
        return f
    }

    fun load(file: File): KeyboardData = fromJson(file.readText())

    fun delete(file: File) { if (file.name != originalFile().name) file.delete() }

    companion object {
        const val FORMAT = "aula-f75-android-backup-1"

        fun toJson(d: KeyboardData, reason: BackupReason): String = JSONObject().apply {
            put("format", FORMAT)
            put("label", reason.slug)
            put("created", System.currentTimeMillis())
            put("config", d.config.bytes.hexCompact())
            put("palette", d.palette.bytes.hexCompact())
            d.base?.let { put("keymapBase", it.bytes.hexCompact()) }
            d.fn?.let { put("keymapFn", it.bytes.hexCompact()) }
            d.perKey?.let { put("perKey", it.bytes.hexCompact()) }
        }.toString(2)

        fun fromJson(text: String): KeyboardData {
            val o = JSONObject(text)
            require(o.optString("format") == FORMAT) { "unknown backup format" }
            val base = o.optString("keymapBase").takeIf { it.isNotEmpty() }?.let { Keymap.parse(it.unhex()) }
            return KeyboardData(
                config = ConfigRegion.parse(o.getString("config").unhex()),
                palette = ColorTable.parse(o.getString("palette").unhex()),
                base = base,
                fn = o.optString("keymapFn").takeIf { it.isNotEmpty() }?.let { Keymap.parse(it.unhex()) },
                perKey = o.optString("perKey").takeIf { it.isNotEmpty() }?.let { PerKeyColors.parse(it.unhex()) },
                layout = base?.detectLayout(),
            )
        }
    }
}
