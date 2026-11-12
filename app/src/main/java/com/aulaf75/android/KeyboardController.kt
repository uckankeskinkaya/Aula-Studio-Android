package com.aulaf75.android

import com.aulaf75.android.protocol.ColorTable
import com.aulaf75.android.protocol.ConfigRegion
import com.aulaf75.android.protocol.F75
import com.aulaf75.android.protocol.F75Error
import com.aulaf75.android.protocol.KeyAction
import com.aulaf75.android.protocol.KeyboardLayout
import com.aulaf75.android.protocol.Keymap
import com.aulaf75.android.protocol.Layer
import com.aulaf75.android.protocol.LightingSettings
import com.aulaf75.android.protocol.PerKeyColors
import com.aulaf75.android.protocol.ProtocolException
import com.aulaf75.android.protocol.Rgb

/** Everything read from the keyboard in one snapshot. */
data class KeyboardData(
    val config: ConfigRegion,
    val palette: ColorTable,
    val base: Keymap?,
    val fn: Keymap?,
    val perKey: PerKeyColors?,
    val layout: KeyboardLayout?,
    val readErrors: List<String> = emptyList(),
) {
    /** Remapping and per-key RGB need a recognised layout (Fn record in slot 53 or 59). */
    val mappingAllowed: Boolean get() = layout != null && base != null
    fun keymap(layer: Layer) = if (layer == Layer.BASE) base else fn
}

/** Raw report I/O; the USB connection in the app, a simulated keyboard in tests. */
interface ReportIo {
    fun query(request: ByteArray): ByteArray
    fun write(report: ByteArray)
}

/**
 * Every write follows the upstream aula-studio transaction contract:
 * 1. re-read the keyboard and refuse (writing nothing) if it differs from what the user reviewed,
 * 2. save a backup, 3. write only the changed banks in the official order,
 * 4. read everything back and require an exact match. No automatic retry or rollback.
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
        val base = runCatching { readKeymap(Layer.BASE) }.onFailure { errors += "base keymap: ${it.message}" }.getOrNull()
        val fn = runCatching { readKeymap(Layer.FN) }.onFailure { errors += "fn keymap: ${it.message}" }.getOrNull()
        val perKey = runCatching { readPerKey() }.onFailure { errors += "per-key: ${it.message}" }.getOrNull()
        return KeyboardData(config, palette, base, fn, perKey, base?.detectLayout(), errors)
    }

    private fun stale(): Nothing = throw ProtocolException(F75Error.STALE_STATE)

    /** Wraps raw I/O failures during a write; the state may be partially changed and is reported as such. */
    private inline fun writing(step: () -> Unit) {
        try {
            step()
        } catch (e: ProtocolException) {
            throw e
        } catch (e: Exception) {
            throw ProtocolException(F75Error.WRITE_INTERRUPTED, e.message)
        }
    }

    /** Lighting and sleep: palette first (if changed), then the configuration bank. */
    fun applyConfig(current: KeyboardData, newConfig: ConfigRegion, newPalette: ColorTable, label: String): KeyboardData {
        val c = readConfig()
        val p = readPalette()
        if (c != current.config || p != current.palette) stale()
        val paletteChanged = newPalette != p
        if (!paletteChanged && newConfig == c) return current
        backup(label, current)
        writing {
            if (paletteChanged) io.write(F75.paletteWriteReport(newPalette))
            // The official application commits 0x04 after 0x0a even when the config is unchanged.
            io.write(F75.configWriteReport(newConfig))
        }
        val after = readAll()
        if (after.config != newConfig || after.palette != newPalette) throw ProtocolException(F75Error.READBACK_MISMATCH)
        return after
    }

    fun applyRemap(current: KeyboardData, layer: Layer, changes: Map<Int, KeyAction>, label: String): KeyboardData {
        val base = readKeymap(Layer.BASE)
        val layout = base.requireLayout()
        if (layout != current.layout) stale()
        val cur = if (layer == Layer.BASE) base else readKeymap(Layer.FN)
        if (cur != current.keymap(layer)) stale()
        for ((index, action) in changes) {
            if (index !in layout.sourceIndices) throw ProtocolException(F75Error.PROTECTED_KEY, "$index")
            action.validate()
        }
        val next = cur.with(changes.filter { (i, a) -> !cur.matches(i, a) })
        if (next == cur) return current
        backup(label, current)
        writing { io.write(next.bytes) }
        val after = readAll()
        if (after.keymap(layer) != next) throw ProtocolException(F75Error.READBACK_MISMATCH)
        return after
    }

    /** Per-key RGB: the 0x06 bank first, then the configuration switches to custom mode (21). */
    fun applyPerKey(current: KeyboardData, changes: Map<Int, Rgb>, label: String): KeyboardData {
        if (readKeymap(Layer.BASE).requireLayout() != current.layout) stale()
        val cur = readPerKey()
        val c = readConfig()
        if (cur != current.perKey || c != current.config) stale()
        val next = cur.with(changes)
        val newConfig = c.withLighting(LightingSettings(F75.CUSTOM_EFFECT, 0, 0, false))
        backup(label, current)
        writing {
            io.write(F75.perKeyWriteReport(next))
            io.write(F75.configWriteReport(newConfig))
        }
        val after = readAll()
        if (after.perKey != next || after.config != newConfig) throw ProtocolException(F75Error.READBACK_MISMATCH)
        return after
    }

    /**
     * Restores a backup. Keymaps copy only remappable source slots (never F1–F12/Fn) and refuse
     * backups whose protected records differ or that would disable a key. Each bank is verified.
     */
    fun restore(current: KeyboardData, saved: KeyboardData): Pair<KeyboardData, Int> {
        saved.palette.validateForWrite()
        var state = current
        var restoredParts = 0
        val sources = (current.layout ?: KeyboardLayout.ANSI).sourceIndices
        for (layer in Layer.entries) {
            val s = saved.keymap(layer) ?: continue
            val cur = state.keymap(layer) ?: continue
            for (p in F75.PROTECTED_MATRIX) if (!cur.raw(p).contentEquals(s.raw(p))) throw ProtocolException(F75Error.BACKUP_PROTECTED_DIFFERS)
            val changes = mutableMapOf<Int, ByteArray>()
            for (i in sources) {
                val rec = s.raw(i)
                if (layer == Layer.BASE && rec.all { it.toInt() == 0 }) throw ProtocolException(F75Error.BACKUP_DISABLES_KEY, "$i")
                if (!cur.raw(i).contentEquals(rec)) changes[i] = rec
            }
            if (changes.isEmpty()) continue
            if (!state.mappingAllowed) throw ProtocolException(F75Error.MAPPING_UNAVAILABLE)
            val n = cur.bytes.copyOf()
            for ((i, rec) in changes) rec.copyInto(n, 8 + 4 * i)
            val next = Keymap.parse(n)
            if (readKeymap(layer) != cur) stale()
            writing { io.write(next.bytes) }
            if (readKeymap(layer) != next) throw ProtocolException(F75Error.READBACK_MISMATCH, layer.name)
            restoredParts++
            state = state.copy(base = if (layer == Layer.BASE) next else state.base, fn = if (layer == Layer.FN) next else state.fn)
        }
        val sp = saved.perKey
        if (sp != null && state.perKey != null && sp != state.perKey) {
            if (readPerKey() != state.perKey) stale()
            writing { io.write(F75.perKeyWriteReport(sp)) }
            if (readPerKey() != sp) throw ProtocolException(F75Error.READBACK_MISMATCH, "per-key")
            restoredParts++
        }
        val c = readConfig()
        val p = readPalette()
        if (c != state.config || p != state.palette) stale()
        writing {
            if (saved.palette != p) io.write(F75.paletteWriteReport(saved.palette))
            io.write(F75.configWriteReport(saved.config))
        }
        val after = readAll()
        if (after.config != saved.config || after.palette != saved.palette) throw ProtocolException(F75Error.READBACK_MISMATCH, "lighting")
        return after to restoredParts + 1
    }
}
