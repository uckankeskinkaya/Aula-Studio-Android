package com.aulaf75.android

import com.aulaf75.android.protocol.ColorTable
import com.aulaf75.android.protocol.ConfigRegion
import com.aulaf75.android.protocol.F75
import com.aulaf75.android.protocol.F75Layout
import com.aulaf75.android.protocol.KeyAction
import com.aulaf75.android.protocol.Keymap
import com.aulaf75.android.protocol.Layer
import com.aulaf75.android.protocol.PerKeyColors
import com.aulaf75.android.protocol.ProtocolException
import com.aulaf75.android.protocol.Rgb
import com.aulaf75.android.protocol.hex

/** Klavyeden okunan tam durum. */
data class KeyboardData(
    val config: ConfigRegion,
    val palette: ColorTable,
    val base: Keymap?,
    val fn: Keymap?,
    val perKey: PerKeyColors?,
    val ansiError: String?,
    val readErrors: List<String>,
) {
    val mappingAllowed: Boolean get() = ansiError == null && base != null
    fun keymap(layer: Layer) = if (layer == Layer.BASE) base else fn
}

/** Cihaz G/Ç arayüzü; testlerde sahte cihazla değiştirilebilir. */
interface ReportIo {
    fun query(request: ByteArray): ByteArray
    fun write(report: ByteArray)
}

/**
 * Tüm yazma işlemleri aynı kalıbı izler (aula-studio işlem sözleşmesi):
 * 1) klavyeyi yeniden oku, ekrandaki durumla aynı değilse hiçbir şey yazma,
 * 2) yedek al, 3) yalnızca değişen bölgeleri resmi sırayla yaz,
 * 4) geri oku ve birebir doğrula. Otomatik tekrar/geri alma yapılmaz.
 */
class KeyboardController(private val io: ReportIo, private val backup: (String, KeyboardData) -> Unit) {

    fun readIdentity(): ByteArray = F75.decodeIdentity(io.query(F75.identityRequest()))

    fun readConfig() = F75.decodeConfig(io.query(F75.configReadRequest()))
    fun readPalette() = F75.decodePalette(io.query(F75.paletteReadRequest()))
    fun readKeymap(layer: Layer) = F75.decodeKeymap(layer, io.query(F75.keymapReadRequest(layer)))
    fun readPerKey() = F75.decodePerKey(io.query(F75.perKeyReadRequest()))

    fun readAll(): KeyboardData {
        val errors = mutableListOf<String>()
        val config = readConfig()
        val palette = readPalette()
        val base = runCatching { readKeymap(Layer.BASE) }.onFailure { errors += "Ana katman: ${it.message}" }.getOrNull()
        val fn = runCatching { readKeymap(Layer.FN) }.onFailure { errors += "Fn katmanı: ${it.message}" }.getOrNull()
        val perKey = runCatching { readPerKey() }.onFailure { errors += "Tuş renkleri: ${it.message}" }.getOrNull()
        val ansiError = if (base == null) "Ana tuş haritası okunamadı"
        else runCatching { base.validateAnsiFnPosition() }.exceptionOrNull()?.message
        return KeyboardData(config, palette, base, fn, perKey, ansiError, errors)
    }

    private fun stale(): Nothing =
        throw ProtocolException("Klavyedeki ayarlar ekrandakiyle aynı değil. Hiçbir şey yazılmadı; 'Yeniden oku' ile tekrar dene.")

    /** Işık/uyku: palet (gerekirse) sonra ayar bölgesi. */
    fun applyConfig(current: KeyboardData, newConfig: ConfigRegion, newPalette: ColorTable, label: String): KeyboardData {
        val c = readConfig()
        val p = readPalette()
        if (c != current.config || p != current.palette) stale()
        val paletteChanged = newPalette != p
        if (!paletteChanged && newConfig == c) return current
        ConfigRegion.parse(newConfig.bytes) // imza kontrolü
        backup(label, current)
        var done = 0
        try {
            if (paletteChanged) { io.write(F75.paletteWriteReport(newPalette)); done++ }
            io.write(F75.configWriteReport(newConfig)); done++
        } catch (e: Exception) {
            throw ProtocolException("Yazma $done adımdan sonra kesildi: ${e.message}. Yedek alındı; yeniden oku ve gerekirse yedekten geri yükle.")
        }
        val after = readAll()
        if (after.config != newConfig || after.palette != newPalette)
            throw ProtocolException("Geri okuma beklenenle eşleşmedi. Yedek duruyor; 'Yedekler' sekmesinden geri yükleyebilirsin.")
        return after
    }

    fun applyRemap(current: KeyboardData, layer: Layer, changes: Map<Int, KeyAction>, label: String): KeyboardData {
        val base = readKeymap(Layer.BASE)
        base.validateAnsiFnPosition()
        val cur = if (layer == Layer.BASE) base else readKeymap(Layer.FN)
        if (cur != current.keymap(layer)) stale()
        for ((index, action) in changes) {
            if (index !in sourceIndices) throw ProtocolException("Bu tuş değiştirilemez")
            action.validate()
        }
        val next = cur.with(changes.filter { (i, a) -> !cur.matches(i, a) })
        if (next == cur) return current
        backup(label, current)
        try {
            io.write(next.bytes)
        } catch (e: Exception) {
            throw ProtocolException("Tuş haritası yazımı kesildi: ${e.message}. Yedek alındı.")
        }
        val after = readAll()
        if (after.keymap(layer) != next) throw ProtocolException("Geri okuma eşleşmedi. Yedekten geri yükleyebilirsin.")
        return after
    }

    /** Tuş başına RGB: önce 06 bankası, sonra ayarlar özel moda (21) alınır. */
    fun applyPerKey(current: KeyboardData, changes: Map<Int, Rgb>, label: String): KeyboardData {
        readKeymap(Layer.BASE).validateAnsiFnPosition()
        val cur = readPerKey()
        val c = readConfig()
        if (cur != current.perKey || c != current.config) stale()
        val next = cur.with(changes)
        val newConfig = c.withLighting(com.aulaf75.android.protocol.LightingSettings(21, 0, 0, false))
        backup(label, current)
        var done = 0
        try {
            io.write(F75.perKeyWriteReport(next)); done++
            io.write(F75.configWriteReport(newConfig)); done++
        } catch (e: Exception) {
            throw ProtocolException("Yazma $done adımdan sonra kesildi: ${e.message}. Yedek alındı.")
        }
        val after = readAll()
        if (after.perKey != next || after.config != newConfig)
            throw ProtocolException("Geri okuma eşleşmedi. Yedekten geri yükleyebilirsin.")
        return after
    }

    /** Yedekten geri yükleme. Her parça ayrı doğrulanır; F1–F12/Fn kayıtları kopyalanmaz. */
    fun restore(current: KeyboardData, saved: KeyboardData): Pair<KeyboardData, List<String>> {
        val notes = mutableListOf<String>()
        saved.palette.validateForWrite()
        var state = current
        // Önce tuş haritaları ve tuş renkleri (ayar yazımı en sonda tüm modu geri getirir).
        for (layer in Layer.values()) {
            val s = saved.keymap(layer) ?: continue
            val cur = state.keymap(layer) ?: continue
            for (p in F75.PROTECTED_MATRIX) if (!cur.raw(p).contentEquals(s.raw(p)))
                throw ProtocolException("Yedekteki korumalı tuş kayıtları farklı; bu yedek başka bir revizyona ait olabilir. Geri yükleme yapılmadı.")
            val changes = mutableMapOf<Int, ByteArray>()
            for (i in sourceIndices) {
                val rec = s.raw(i)
                if (layer == Layer.BASE && rec.all { it.toInt() == 0 })
                    throw ProtocolException("Yedek bir tuşu devre dışı bırakırdı; geri yükleme yapılmadı.")
                if (!cur.raw(i).contentEquals(rec)) changes[i] = rec
            }
            if (changes.isEmpty()) continue
            if (!state.mappingAllowed) throw ProtocolException(state.ansiError ?: "Tuş haritası yazımı kapalı")
            val n = cur.bytes.copyOf()
            for ((i, rec) in changes) rec.copyInto(n, 8 + 4 * i)
            val next = Keymap.parse(n)
            if (readKeymap(layer) != cur) stale()
            io.write(next.bytes)
            if (readKeymap(layer) != next) throw ProtocolException("${layer.name} katmanı geri okuması eşleşmedi")
            notes += "${if (layer == Layer.BASE) "Ana" else "Fn"} katman: ${changes.size} tuş geri yüklendi"
            state = state.copy(base = if (layer == Layer.BASE) next else state.base, fn = if (layer == Layer.FN) next else state.fn)
        }
        val sp = saved.perKey
        if (sp != null && state.perKey != null && sp != state.perKey) {
            if (readPerKey() != state.perKey) stale()
            io.write(F75.perKeyWriteReport(sp))
            if (readPerKey() != sp) throw ProtocolException("Tuş renkleri geri okuması eşleşmedi")
            notes += "Tuş renkleri geri yüklendi"
        }
        val c = readConfig()
        val p = readPalette()
        if (c != state.config || p != state.palette) stale()
        if (saved.palette != p) io.write(F75.paletteWriteReport(saved.palette))
        // Resmi uygulama gibi palet/tuş rengi değişiminden sonra ayar da yazılır.
        io.write(F75.configWriteReport(saved.config))
        val after = readAll()
        if (after.config != saved.config || after.palette != saved.palette)
            throw ProtocolException("Işık ayarlarının geri okuması eşleşmedi")
        notes += "Işık, renk ve uyku ayarları geri yüklendi"
        return after to notes
    }

    companion object {
        /** Değiştirilebilir fiziksel kaynak tuşlar (F1–F12 ve Fn hariç). */
        val sourceIndices: Set<Int> = F75Layout.KEYS.filter { !it.isProtected }.map { it.matrixIndex }.toSet()

        fun identityText(id: ByteArray) = id.hex()
    }
}
