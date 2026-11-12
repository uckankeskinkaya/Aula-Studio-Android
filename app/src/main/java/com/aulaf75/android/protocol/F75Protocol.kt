package com.aulaf75.android.protocol

/*
 * AULA F75 wired configuration protocol (USB 258a:010c, interface 1,
 * HID feature report 0x06, 520 bytes).
 *
 * This is a faithful Kotlin port of `crates/aula-protocol` from
 * Dahuum/aula-studio (MIT). Offsets, signatures and safety rules follow the
 * evidence documented there (docs/PROTOCOL.md). This file is pure Kotlin: it
 * only encodes and decodes bytes and never talks to a device.
 */

/** Every failure the app can report. The UI maps each code to a localized message. */
enum class F75Error(val transport: Boolean = false) {
    // Malformed or unexpected device data
    BAD_LENGTH, BAD_HEADER, BAD_CONFIG_SIGNATURE, BAD_PALETTE_SIGNATURE, BAD_KEYMAP_SIGNATURE,
    UNKNOWN_EFFECT,

    // Rejected requests (nothing is written)
    VALUE_OUT_OF_RANGE, UNSUPPORTED_SLEEP, EFFECT_HAS_NO_COLOR, MODE_HAS_NO_BRIGHTNESS,
    INDEX_OUT_OF_RANGE, PROTECTED_KEY, UNSUPPORTED_ACTION, LAYOUT_UNKNOWN, MAPPING_UNAVAILABLE,

    // Transaction safety
    STALE_STATE, WRITE_INTERRUPTED, READBACK_MISMATCH, BACKUP_PROTECTED_DIFFERS, BACKUP_DISABLES_KEY,
    BACKUP_INVALID,

    // USB / connection
    DEVICE_NOT_FOUND, NO_USB_DEVICES, PERMISSION_DENIED, NOT_CONNECTED, IDENTITY_MISMATCH,
    INTERFACE_MISSING(transport = true), OPEN_FAILED(transport = true), CLAIM_FAILED(transport = true),
    TRANSFER_FAILED(transport = true),
}

/** Thrown for every protocol, transaction and transport failure. [detail] is technical context (not localized). */
class ProtocolException(val error: F75Error, val detail: String? = null) :
    Exception(error.name + (detail?.let { ": $it" } ?: ""))

object F75 {
    const val VENDOR_ID = 0x258a
    const val PRODUCT_ID = 0x010c
    const val RECEIVER_VENDOR_ID = 0x3554
    const val CONFIG_INTERFACE = 1

    const val REPORT_ID = 0x06
    const val REPORT_SIZE = 520
    const val HEADER_SIZE = 8
    const val CONFIG_SIZE = 128
    const val COLORS_SIZE = 512
    const val PER_KEY_SIZE = 378

    /** Matrix slot of Fn on ANSI boards (slot 59 is Right Ctrl there). */
    const val FN_MATRIX_INDEX = 53

    /** Matrix slot of Fn on ISO boards (slot 53 is AltGr there). */
    const val ISO_FN_MATRIX_INDEX = 59

    /** Six-byte model identity of the original F75; configuration is refused for anything else. */
    val F75_IDENTITY = byteArrayOf(3, 0, 0, 0, 0, 0xcd.toByte())

    val SLEEP_SECONDS = listOf(0, 60, 90, 120, 180, 240, 300, 600, 900, 1200)

    /** Hardware preset effects (0 = off). 21 is the per-key "custom" mode. */
    val EFFECTS = 0..18
    const val CUSTOM_EFFECT = 21

    /**
     * Firmware control sources that are never remapped: F1–F12 plus both possible
     * Fn slots (53, 59). Protecting both on every layout keeps Fn combinations,
     * including the factory reset, reachable whatever the board revision.
     */
    val PROTECTED_MATRIX = setOf(12, 18, 24, 30, 36, 42, 48, 54, 60, 66, 72, 78, FN_MATRIX_INDEX, ISO_FN_MATRIX_INDEX)

    fun effectSupportsColor(effect: Int): Boolean = effect in setOf(1, 2) || effect in 4..14 || effect == 18
    fun effectSupportsSpeed(effect: Int): Boolean = effect in 2..18

    // ---------------------------------------------------------------- framing

    /** [id, command, target, reserved, total chunks, chunk index, length LE16] + zero padding. */
    private fun header(command: Int, target: Int, length: Int): ByteArray {
        val b = ByteArray(REPORT_SIZE)
        b[0] = REPORT_ID.toByte()
        b[1] = command.toByte()
        b[2] = target.toByte()
        b[4] = 1
        b[6] = (length and 0xff).toByte()
        b[7] = (length shr 8 and 0xff).toByte()
        return b
    }

    fun identityRequest(): ByteArray = header(0x82, 1, 6)
    fun configReadRequest(): ByteArray = header(0x84, 0, CONFIG_SIZE)
    fun paletteReadRequest(): ByteArray = header(0x8a, 0, COLORS_SIZE)
    fun keymapReadRequest(layer: Layer): ByteArray = header(0x83, layer.byte, 512)
    fun perKeyReadRequest(): ByteArray = header(0x86, 0, PER_KEY_SIZE)

    fun configWriteReport(config: ConfigRegion): ByteArray =
        header(0x04, 0, CONFIG_SIZE).also { config.bytes.copyInto(it, HEADER_SIZE) }

    fun paletteWriteReport(palette: ColorTable): ByteArray {
        palette.validateForWrite()
        return header(0x0a, 0, COLORS_SIZE).also { palette.bytes.copyInto(it, HEADER_SIZE) }
    }

    fun perKeyWriteReport(colors: PerKeyColors): ByteArray =
        header(0x06, 0, PER_KEY_SIZE).also { colors.bytes.copyInto(it, HEADER_SIZE) }

    fun decodeIdentity(reply: ByteArray): ByteArray {
        if (reply.size != REPORT_SIZE && reply.size != 14) throw ProtocolException(F75Error.BAD_LENGTH, "identity ${reply.size}")
        val req = identityRequest()
        for (i in 0 until 8) if (reply[i] != req[i]) throw ProtocolException(F75Error.BAD_HEADER, reply.copyOf(8).hex())
        return reply.copyOfRange(8, 14)
    }

    /** Validates a region reply (firmware may echo the read or the write command) and returns its payload. */
    private fun decodeRegion(reply: ByteArray, readCmd: Int, writeCmd: Int, size: Int): ByteArray {
        if (reply.size != REPORT_SIZE && reply.size != HEADER_SIZE + size)
            throw ProtocolException(F75Error.BAD_LENGTH, "got ${reply.size}, expected ${HEADER_SIZE + size}")
        val expected = header(readCmd, 0, size)
        val cmd = reply[1].u()
        if (reply[0].toInt() != REPORT_ID || (cmd != readCmd && cmd != writeCmd) ||
            (2 until 8).any { reply[it] != expected[it] }
        ) throw ProtocolException(F75Error.BAD_HEADER, reply.copyOf(8).hex())
        return reply.copyOfRange(HEADER_SIZE, HEADER_SIZE + size)
    }

    fun decodeConfig(reply: ByteArray) = ConfigRegion.parse(decodeRegion(reply, 0x84, 0x04, CONFIG_SIZE))
    fun decodePalette(reply: ByteArray) = ColorTable.parse(decodeRegion(reply, 0x8a, 0x0a, COLORS_SIZE))
    fun decodePerKey(reply: ByteArray) = PerKeyColors.parse(decodeRegion(reply, 0x86, 0x06, PER_KEY_SIZE))

    /** Keymap replies must be full 520-byte reports; the command byte is normalized to write form 0x03. */
    fun decodeKeymap(layer: Layer, reply: ByteArray): Keymap {
        if (reply.size != REPORT_SIZE) throw ProtocolException(F75Error.BAD_LENGTH, "keymap ${reply.size}")
        val req = keymapReadRequest(layer)
        val cmd = reply[1].u()
        if (reply[0].toInt() != REPORT_ID || (cmd != 0x83 && cmd != 0x03) || (2 until 8).any { reply[it] != req[it] })
            throw ProtocolException(F75Error.BAD_HEADER, reply.copyOf(8).hex())
        val normalized = reply.copyOf()
        normalized[1] = 0x03
        return Keymap.parse(normalized)
    }

    // ------------------------------------------------------------ ANSI matrix

    private val LETTERS = intArrayOf(
        9, 34, 22, 21, 20, 27, 33, 39, 50, 45, 51, 57, 46, 40, 56, 62, 8, 26, 15, 32, 44, 28, 14, 16, 38, 10,
    )

    /** Matrix slot of the physical key that sends [usage] on the original ANSI F75, or null. */
    fun matrixIndexForUsage(usage: Int): Int? = when (usage) {
        in 4..29 -> LETTERS[usage - 4]
        in 30..38 -> 7 + 6 * (usage - 30)
        39 -> 61
        40 -> 81
        41 -> 0
        42 -> 79
        43 -> 2
        44 -> 35
        45 -> 67
        46 -> 73
        47 -> 68
        48 -> 74
        49 -> 80
        51 -> 63
        52 -> 69
        53 -> 1
        54 -> 52
        55 -> 58
        56 -> 64
        57 -> 3
        in 58..69 -> 12 + 6 * (usage - 58)
        75 -> 86
        76 -> 85
        77 -> 88
        78 -> 87
        79 -> 89
        80 -> 77
        81 -> 83
        82 -> 82
        224 -> 5
        225 -> 4
        226 -> 17
        227 -> 11
        228 -> 59
        229 -> 70
        else -> null
    }
}

enum class Layer(val byte: Int) { BASE(0), FN(1) }

// ------------------------------------------------------------------- config

data class LightingSettings(val effect: Int, val brightness: Int, val speed: Int, val colorful: Boolean) {
    fun validate() {
        if (effect !in F75.EFFECTS && effect != F75.CUSTOM_EFFECT) throw ProtocolException(F75Error.UNKNOWN_EFFECT, "$effect")
        if (brightness !in 0..9) throw ProtocolException(F75Error.VALUE_OUT_OF_RANGE, "brightness $brightness")
        if (speed !in 0..4) throw ProtocolException(F75Error.VALUE_OUT_OF_RANGE, "speed $speed")
    }
}

/** The 128-byte configuration bank (command 0x04 / 0x84), signed 5A A5 at 126/127. */
class ConfigRegion private constructor(val bytes: ByteArray) {
    companion object {
        fun parse(data: ByteArray): ConfigRegion {
            if (data.size != F75.CONFIG_SIZE) throw ProtocolException(F75Error.BAD_LENGTH, "config ${data.size}")
            if (data[126] != 0x5a.toByte() || data[127] != 0xa5.toByte()) throw ProtocolException(F75Error.BAD_CONFIG_SIGNATURE)
            return ConfigRegion(data.copyOf())
        }
    }

    val effectId: Int get() = bytes[10].u()

    fun lighting(): LightingSettings {
        val effect = effectId
        if (effect !in F75.EFFECTS && effect != F75.CUSTOM_EFFECT) throw ProtocolException(F75Error.UNKNOWN_EFFECT, "$effect")
        if (effect == 0 || effect == F75.CUSTOM_EFFECT) return LightingSettings(effect, 0, 0, false)
        val o = 56 + effect * 2
        return LightingSettings(effect, bytes[o].u(), bytes[o + 1].u() shr 4, bytes[o + 1].u() and 0x0f == 7)
            .also { it.validate() }
    }

    /** Changes only bytes 9, 10, 11 and the selected effect's two parameter bytes. */
    fun withLighting(s: LightingSettings): ConfigRegion {
        s.validate()
        val n = bytes.copyOf()
        n[9] = if (s.effect == F75.CUSTOM_EFFECT) 1 else 0
        n[10] = s.effect.toByte()
        if (s.effect == F75.CUSTOM_EFFECT) {
            n[11] = 0x20 // StartGp + group 0
        } else if (s.effect != 0) {
            val o = 56 + s.effect * 2
            n[o] = s.brightness.toByte()
            val speed = if (s.effect == 1) n[o + 1].u() and 0xf0 else s.speed shl 4
            val color = if (F75.effectSupportsColor(s.effect)) (if (s.colorful) 7 else 0) else n[o + 1].u() and 0x0f
            n[o + 1] = (speed or color).toByte()
        }
        return ConfigRegion(n)
    }

    /** Changes only the active preset's brightness byte (the write validated on hardware upstream). */
    fun withPresetBrightness(value: Int): ConfigRegion {
        if (value !in 0..9) throw ProtocolException(F75Error.VALUE_OUT_OF_RANGE, "brightness $value")
        val effect = effectId
        if (effect !in 1..18) throw ProtocolException(F75Error.MODE_HAS_NO_BRIGHTNESS)
        lighting()
        return ConfigRegion(bytes.copyOf().also { it[56 + effect * 2] = value.toByte() })
    }

    /** Parameters stored on the keyboard for [effect], even when it is not the active effect. */
    fun storedSettings(effect: Int): LightingSettings {
        if (effect !in 1..18) return LightingSettings(effect, 0, 0, false)
        val o = 56 + effect * 2
        return LightingSettings(
            effect,
            bytes[o].u().coerceIn(0, 9),
            (bytes[o + 1].u() shr 4).coerceIn(0, 4),
            bytes[o + 1].u() and 0x0f == 7,
        )
    }

    /** Sleep timeout in seconds (byte 24, 30-second units), or null if the stored value is unknown. */
    fun sleepSeconds(): Int? = (bytes[24].u() * 30).takeIf { it in F75.SLEEP_SECONDS }

    fun withSleepSeconds(seconds: Int): ConfigRegion {
        if (seconds !in F75.SLEEP_SECONDS) throw ProtocolException(F75Error.UNSUPPORTED_SLEEP, "$seconds")
        return ConfigRegion(bytes.copyOf().also { it[24] = (seconds / 30).toByte() })
    }

    override fun equals(other: Any?) = other is ConfigRegion && other.bytes.contentEquals(bytes)
    override fun hashCode() = bytes.contentHashCode()
}

// ------------------------------------------------------------------ palette

/** The 512-byte effect palette (0x0a / 0x8a): seven RGB triples per effect at 21 × id, signed at 506/507. */
class ColorTable private constructor(val bytes: ByteArray) {
    companion object {
        fun parse(data: ByteArray): ColorTable {
            if (data.size != F75.COLORS_SIZE) throw ProtocolException(F75Error.BAD_LENGTH, "palette ${data.size}")
            return ColorTable(data.copyOf())
        }
    }

    fun validateForWrite() {
        if (bytes[506] != 0x5a.toByte() || bytes[507] != 0xa5.toByte()) throw ProtocolException(F75Error.BAD_PALETTE_SIGNATURE)
    }

    fun effectColor(effect: Int): Rgb {
        if (effect !in 1..18) throw ProtocolException(F75Error.EFFECT_HAS_NO_COLOR, "$effect")
        val o = effect * 21
        return Rgb(bytes[o].u(), bytes[o + 1].u(), bytes[o + 2].u())
    }

    /** Changes only the first RGB triple of [effect]; every other byte is preserved. */
    fun withEffectColor(effect: Int, c: Rgb): ColorTable {
        validateForWrite()
        if (!F75.effectSupportsColor(effect)) throw ProtocolException(F75Error.EFFECT_HAS_NO_COLOR, "$effect")
        val o = effect * 21
        val n = bytes.copyOf()
        n[o] = c.r.toByte(); n[o + 1] = c.g.toByte(); n[o + 2] = c.b.toByte()
        return ColorTable(n)
    }

    override fun equals(other: Any?) = other is ColorTable && other.bytes.contentEquals(bytes)
    override fun hashCode() = bytes.contentHashCode()
}

data class Rgb(val r: Int, val g: Int, val b: Int) {
    fun toHex() = "#%02X%02X%02X".format(r, g, b)
    val isDark: Boolean get() = maxOf(r, g, b) < 24

    companion object {
        fun fromHex(s: String): Rgb? {
            val h = s.trim().removePrefix("#")
            if (h.length != 6) return null
            val v = h.toIntOrNull(16) ?: return null
            return Rgb(v shr 16 and 0xff, v shr 8 and 0xff, v and 0xff)
        }
    }
}

// ------------------------------------------------------------- per-key RGB

/** Per-key colors (0x06 / 0x86): three planar 126-byte planes R, G, B indexed by matrix slot. */
class PerKeyColors private constructor(val bytes: ByteArray) {
    companion object {
        fun parse(data: ByteArray): PerKeyColors {
            if (data.size != F75.PER_KEY_SIZE) throw ProtocolException(F75Error.BAD_LENGTH, "per-key ${data.size}")
            return PerKeyColors(data.copyOf())
        }
    }

    fun get(index: Int): Rgb {
        require(index in 0 until 126)
        return Rgb(bytes[index].u(), bytes[126 + index].u(), bytes[252 + index].u())
    }

    fun with(changes: Map<Int, Rgb>): PerKeyColors {
        val n = bytes.copyOf()
        for ((index, c) in changes) {
            if (index !in 0 until 126) throw ProtocolException(F75Error.INDEX_OUT_OF_RANGE, "$index")
            n[index] = c.r.toByte(); n[126 + index] = c.g.toByte(); n[252 + index] = c.b.toByte()
        }
        return PerKeyColors(n)
    }

    override fun equals(other: Any?) = other is PerKeyColors && other.bytes.contentEquals(bytes)
    override fun hashCode() = bytes.contentHashCode()
}

// ------------------------------------------------------------------- keymap

/** A key assignment. [page] is the device record type (0 keyboard, 1 mouse, 2 consumer), not a USB usage page. */
data class KeyAction(val page: Int, val usage: Int) {
    fun validate() {
        val ok = when (page) {
            0 -> usage in 4..0xa4 || usage in 0xe0..0xe7
            1 -> usage in 1..5
            2 -> usage in KeyNames.MEDIA_USAGES
            else -> false
        }
        if (!ok) throw ProtocolException(F75Error.UNSUPPORTED_ACTION, "$page/$usage")
    }

    /** Four-byte record exactly as the official converter emits it. */
    fun record(): ByteArray {
        validate()
        return when {
            page == 0 && usage in 0xe0..0xe7 -> byteArrayOf(0, (1 shl (usage - 0xe0)).toByte(), 0, 0)
            page == 1 -> byteArrayOf(1, usage.toByte(), 1, 0)
            else -> byteArrayOf(page.toByte(), 0, (usage shr 8).toByte(), usage.toByte())
        }
    }
}

/** A complete 520-byte keymap report (0x03 / 0x83): 128 four-byte slots, signed 5A A5 at 518/519. */
class Keymap private constructor(val bytes: ByteArray) {
    companion object {
        fun parse(data: ByteArray): Keymap {
            if (data.size != F75.REPORT_SIZE) throw ProtocolException(F75Error.BAD_LENGTH, "keymap ${data.size}")
            val ok = data[0].u() == 6 && data[1].u() == 3 && data[2].u() <= 1 &&
                data[3].u() == 0 && data[4].u() == 1 && data[5].u() == 0 && data[6].u() == 0 && data[7].u() == 2
            if (!ok) throw ProtocolException(F75Error.BAD_HEADER, data.copyOf(8).hex())
            if (data[518] != 0x5a.toByte() || data[519] != 0xa5.toByte()) throw ProtocolException(F75Error.BAD_KEYMAP_SIGNATURE)
            return Keymap(data.copyOf())
        }
    }

    val layer: Layer get() = if (bytes[2].u() == 0) Layer.BASE else Layer.FN

    fun raw(index: Int): ByteArray {
        if (index !in 0..126) throw ProtocolException(F75Error.INDEX_OUT_OF_RANGE, "$index")
        val o = 8 + 4 * index
        return bytes.copyOfRange(o, o + 4)
    }

    fun matches(index: Int, action: KeyAction) = raw(index).contentEquals(action.record())

    /** Patches only the given slots. Slot 127 overlaps the signature and protected slots are refused. */
    fun with(changes: Map<Int, KeyAction>): Keymap {
        val n = bytes.copyOf()
        for ((index, action) in changes) {
            if (index !in 0..126) throw ProtocolException(F75Error.INDEX_OUT_OF_RANGE, "$index")
            if (index in F75.PROTECTED_MATRIX) throw ProtocolException(F75Error.PROTECTED_KEY, "$index")
            action.record().copyInto(n, 8 + 4 * index)
        }
        return Keymap(n)
    }

    /**
     * Physical layout from the position of the Fn record (0D 00 00 00):
     * slot 53 => ANSI, slot 59 => ISO (upstream FR/ISO package evidence). Null when ambiguous.
     */
    fun detectLayout(): KeyboardLayout? {
        if (layer != Layer.BASE) return null
        val marker = byteArrayOf(13, 0, 0, 0)
        val at53 = raw(F75.FN_MATRIX_INDEX).contentEquals(marker)
        val at59 = raw(F75.ISO_FN_MATRIX_INDEX).contentEquals(marker)
        return when {
            at53 && !at59 -> KeyboardLayout.ANSI
            at59 && !at53 -> KeyboardLayout.ISO
            else -> null
        }
    }

    fun requireLayout(): KeyboardLayout = detectLayout() ?: throw ProtocolException(F75Error.LAYOUT_UNKNOWN)

    override fun equals(other: Any?) = other is Keymap && other.bytes.contentEquals(bytes)
    override fun hashCode() = bytes.contentHashCode()
}

// -------------------------------------------------------------------- utils

internal fun Byte.u(): Int = toInt() and 0xff
fun ByteArray.hex(): String = joinToString(" ") { "%02X".format(it) }
fun ByteArray.hexCompact(): String = joinToString("") { "%02x".format(it) }
fun String.unhex(): ByteArray {
    val s = filter { !it.isWhitespace() }
    require(s.length % 2 == 0)
    return ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
