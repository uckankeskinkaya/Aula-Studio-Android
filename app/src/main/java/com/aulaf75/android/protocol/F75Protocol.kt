package com.aulaf75.android.protocol

/*
 * AULA F75 (kablolu, 258a:010c, arayüz 1) HID feature-report protokolü.
 *
 * Bu dosya Dahuum/aula-studio (MIT) projesindeki `crates/aula-protocol`
 * Rust kodunun birebir Kotlin portudur. Ofsetler, imzalar ve korumalar
 * oradaki kanıtlara dayanır. Bu dosya Android'e bağımlı değildir; yalnızca
 * baytları kodlar/çözer, cihaza hiçbir şey göndermez.
 */

class ProtocolException(message: String) : Exception(message)

object F75 {
    const val VENDOR_ID = 0x258a
    const val PRODUCT_ID = 0x010c
    const val CONFIG_INTERFACE = 1

    const val REPORT_ID = 0x06
    const val REPORT_SIZE = 520
    const val HEADER_SIZE = 8
    const val CONFIG_SIZE = 128
    const val COLORS_SIZE = 512
    const val PER_KEY_SIZE = 378
    const val FN_MATRIX_INDEX = 53

    /** Yalnızca bu kimliğe sahip orijinal ANSI F75 yapılandırılır. */
    val F75_IDENTITY = byteArrayOf(3, 0, 0, 0, 0, 0xcd.toByte())

    val SLEEP_SECONDS = listOf(0, 60, 90, 120, 180, 240, 300, 600, 900, 1200)

    /** F1..F12 ve Fn: firmware kontrol tuşları, kaynak olarak değiştirilemez. */
    val PROTECTED_MATRIX = setOf(12, 18, 24, 30, 36, 42, 48, 54, 60, 66, 72, 78, FN_MATRIX_INDEX)

    val EFFECT_NAMES = linkedMapOf(
        0 to "Kapalı",
        1 to "Sabit",
        2 to "Nefes",
        3 to "Gökkuşağı",
        4 to "Flash away",
        5 to "Yağmur damlası",
        6 to "Gökkuşağı çarkı",
        7 to "Dalgalanma",
        8 to "Yıldızlar",
        9 to "Gölge",
        10 to "Retro yılan",
        11 to "Neon akış",
        12 to "Tepki (basınca)",
        13 to "Sinüs dalgası",
        14 to "Tarama",
        15 to "Yel değirmeni",
        16 to "Renkli şelale",
        17 to "Çiçeklenme",
        18 to "Fırtına",
    )

    fun effectSupportsColor(effect: Int): Boolean = effect in setOf(1, 2) || effect in 4..14 || effect == 18
    fun effectSupportsSpeed(effect: Int): Boolean = effect in 2..18

    // ---------------------------------------------------------------- framing

    private fun header(command: Int, target: Int, length: Int): ByteArray {
        val b = ByteArray(REPORT_SIZE)
        b[0] = REPORT_ID.toByte()
        b[1] = command.toByte()
        b[2] = target.toByte()
        b[3] = 0
        b[4] = 1 // toplam parça
        b[5] = 0 // parça indeksi
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
        if (reply.size != REPORT_SIZE && reply.size != 14) throw ProtocolException("Kimlik yanıtı uzunluğu hatalı: ${reply.size}")
        val req = identityRequest()
        for (i in 0 until 8) if (reply[i] != req[i]) throw ProtocolException("Kimlik yanıt başlığı hatalı: ${reply.copyOf(8).hex()}")
        return reply.copyOfRange(8, 14)
    }

    /** 0x84 / 0x8a yanıtını doğrular, yükü döndürür. */
    private fun decodeRegion(reply: ByteArray, readCmd: Int, writeCmd: Int, size: Int): ByteArray {
        if (reply.size != REPORT_SIZE && reply.size != HEADER_SIZE + size)
            throw ProtocolException("Yanıt uzunluğu ${reply.size}, beklenen ${HEADER_SIZE + size}")
        val expected = header(readCmd, 0, size)
        val cmd = reply[1].toInt() and 0xff
        if (reply[0].toInt() != REPORT_ID || (cmd != readCmd && cmd != writeCmd) ||
            (2 until 8).any { reply[it] != expected[it] }
        ) throw ProtocolException("Yanıt başlığı hatalı: ${reply.copyOf(8).hex()}")
        return reply.copyOfRange(HEADER_SIZE, HEADER_SIZE + size)
    }

    fun decodeConfig(reply: ByteArray) = ConfigRegion.parse(decodeRegion(reply, 0x84, 0x04, CONFIG_SIZE))
    fun decodePalette(reply: ByteArray) = ColorTable.parse(decodeRegion(reply, 0x8a, 0x0a, COLORS_SIZE))
    fun decodePerKey(reply: ByteArray) = PerKeyColors.parse(decodeRegion(reply, 0x86, 0x06, PER_KEY_SIZE))

    fun decodeKeymap(layer: Layer, reply: ByteArray): Keymap {
        if (reply.size != REPORT_SIZE) throw ProtocolException("Tuş haritası yanıtı 520 bayt olmalı, gelen ${reply.size}")
        val req = keymapReadRequest(layer)
        val cmd = reply[1].toInt() and 0xff
        if (reply[0].toInt() != REPORT_ID || (cmd != 0x83 && cmd != 0x03) || (2 until 8).any { reply[it] != req[it] })
            throw ProtocolException("Tuş haritası başlığı hatalı: ${reply.copyOf(8).hex()}")
        val normalized = reply.copyOf()
        normalized[1] = 0x03
        return Keymap.parse(normalized)
    }

    // --------------------------------------------------------- ANSI matrisi

    private val LETTERS = intArrayOf(
        9, 34, 22, 21, 20, 27, 33, 39, 50, 45, 51, 57, 46, 40, 56, 62, 8, 26, 15, 32, 44, 28, 14, 16, 38, 10,
    )

    /** Fiziksel tuşun (HID kullanım kodu) matris indeksini verir; F75'te yoksa null. */
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
        if (effect > 18 && effect != 21) throw ProtocolException("Bilinmeyen efekt $effect")
        if (brightness !in 0..9) throw ProtocolException("Parlaklık 0..9 olmalı")
        if (speed !in 0..4) throw ProtocolException("Hız 0..4 olmalı")
    }
}

class ConfigRegion private constructor(val bytes: ByteArray) {
    companion object {
        fun parse(data: ByteArray): ConfigRegion {
            if (data.size != F75.CONFIG_SIZE) throw ProtocolException("Ayar bölgesi 128 bayt olmalı")
            if (data[126] != 0x5a.toByte() || data[127] != 0xa5.toByte()) throw ProtocolException("Ayar imzası 5A A5 yok")
            return ConfigRegion(data.copyOf())
        }
    }

    val effectId: Int get() = bytes[10].u()

    fun lighting(): LightingSettings {
        val effect = effectId
        if (effect > 18 && effect != 21) throw ProtocolException("Klavyede bilinmeyen efekt kimliği: $effect")
        if (effect == 0 || effect == 21) return LightingSettings(effect, 0, 0, false)
        val o = 56 + effect * 2
        return LightingSettings(effect, bytes[o].u(), bytes[o + 1].u() shr 4, bytes[o + 1].u() and 0x0f == 7)
            .also { it.validate() }
    }

    /** Yalnızca 9, 10, 11 ve efektin iki parametre baytını değiştirir. */
    fun withLighting(s: LightingSettings): ConfigRegion {
        s.validate()
        val n = bytes.copyOf()
        n[9] = if (s.effect == 21) 1 else 0
        n[10] = s.effect.toByte()
        if (s.effect == 21) {
            n[11] = 0x20
        } else if (s.effect != 0) {
            val o = 56 + s.effect * 2
            n[o] = s.brightness.toByte()
            val speed = if (s.effect == 1) n[o + 1].u() and 0xf0 else s.speed shl 4
            val color = if (F75.effectSupportsColor(s.effect)) (if (s.colorful) 7 else 0) else n[o + 1].u() and 0x0f
            n[o + 1] = (speed or color).toByte()
        }
        return ConfigRegion(n)
    }

    /** Yalnızca seçili hazır efektin parlaklık baytını değiştirir (orijinal projede donanımda test edilmiş tek yazma). */
    fun withPresetBrightness(value: Int): ConfigRegion {
        if (value !in 0..9) throw ProtocolException("Parlaklık 0..9 olmalı")
        val effect = effectId
        if (effect !in 1..18) throw ProtocolException("Seçili modda (kapalı/özel) parlaklık alanı yok")
        lighting()
        return ConfigRegion(bytes.copyOf().also { it[56 + effect * 2] = value.toByte() })
    }

    /** Bir efektin klavyede kayıtlı parametreleri (efekt seçilmemiş olsa da). */
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

    fun sleepSeconds(): Int? = (bytes[24].u() * 30).takeIf { it in F75.SLEEP_SECONDS }

    fun withSleepSeconds(seconds: Int): ConfigRegion {
        if (seconds !in F75.SLEEP_SECONDS) throw ProtocolException("Desteklenmeyen uyku süresi")
        return ConfigRegion(bytes.copyOf().also { it[24] = (seconds / 30).toByte() })
    }

    override fun equals(other: Any?) = other is ConfigRegion && other.bytes.contentEquals(bytes)
    override fun hashCode() = bytes.contentHashCode()
}

// ------------------------------------------------------------------ palette

class ColorTable private constructor(val bytes: ByteArray) {
    companion object {
        fun parse(data: ByteArray): ColorTable {
            if (data.size != F75.COLORS_SIZE) throw ProtocolException("Palet 512 bayt olmalı")
            return ColorTable(data.copyOf())
        }
    }

    fun validateForWrite() {
        if (bytes[506] != 0x5a.toByte() || bytes[507] != 0xa5.toByte())
            throw ProtocolException("Palet imzası (506/507 = 5A A5) yok; yazma engellendi")
    }

    fun effectColor(effect: Int): Rgb {
        if (effect !in 1..18) throw ProtocolException("Bu efektin palet kaydı yok")
        val o = effect * 21
        return Rgb(bytes[o].u(), bytes[o + 1].u(), bytes[o + 2].u())
    }

    fun withEffectColor(effect: Int, c: Rgb): ColorTable {
        validateForWrite()
        if (!F75.effectSupportsColor(effect)) throw ProtocolException("Bu efektin rengi ayarlanamaz")
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

class PerKeyColors private constructor(val bytes: ByteArray) {
    companion object {
        fun parse(data: ByteArray): PerKeyColors {
            if (data.size != F75.PER_KEY_SIZE) throw ProtocolException("Tuş renkleri 378 bayt olmalı")
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
            if (index !in 0 until 126) throw ProtocolException("LED indeksi sınır dışında")
            n[index] = c.r.toByte(); n[126 + index] = c.g.toByte(); n[252 + index] = c.b.toByte()
        }
        return PerKeyColors(n)
    }

    override fun equals(other: Any?) = other is PerKeyColors && other.bytes.contentEquals(bytes)
    override fun hashCode() = bytes.contentHashCode()
}

// ------------------------------------------------------------------- keymap

/** page: 0 klavye, 1 fare, 2 medya (cihaz kayıt tipi, USB sayfası değil). */
data class KeyAction(val page: Int, val usage: Int) {
    fun validate() {
        val ok = when (page) {
            0 -> usage in 4..0xa4 || usage in 0xe0..0xe7
            1 -> usage in 1..5
            2 -> usage in KeyNames.MEDIA.keys
            else -> false
        }
        if (!ok) throw ProtocolException("Desteklenmeyen tuş eylemi ($page, $usage); tuş devre dışı bırakılamaz")
    }

    fun record(): ByteArray {
        validate()
        return when {
            page == 0 && usage in 0xe0..0xe7 -> byteArrayOf(0, (1 shl (usage - 0xe0)).toByte(), 0, 0)
            page == 1 -> byteArrayOf(1, usage.toByte(), 1, 0)
            else -> byteArrayOf(page.toByte(), 0, (usage shr 8).toByte(), usage.toByte())
        }
    }
}

class Keymap private constructor(val bytes: ByteArray) {
    companion object {
        fun parse(data: ByteArray): Keymap {
            if (data.size != F75.REPORT_SIZE) throw ProtocolException("Tuş haritası 520 bayt olmalı")
            val ok = data[0].u() == 6 && data[1].u() == 3 && data[2].u() <= 1 &&
                data[3].u() == 0 && data[4].u() == 1 && data[5].u() == 0 && data[6].u() == 0 && data[7].u() == 2
            if (!ok) throw ProtocolException("Tuş haritası başlığı hatalı")
            if (data[518] != 0x5a.toByte() || data[519] != 0xa5.toByte()) throw ProtocolException("Tuş haritası imzası yok")
            return Keymap(data.copyOf())
        }
    }

    val layer: Layer get() = if (bytes[2].u() == 0) Layer.BASE else Layer.FN

    fun raw(index: Int): ByteArray {
        if (index !in 0..126) throw ProtocolException("Matris indeksi sınır dışında")
        val o = 8 + 4 * index
        return bytes.copyOfRange(o, o + 4)
    }

    fun matches(index: Int, action: KeyAction) = raw(index).contentEquals(action.record())

    fun with(changes: Map<Int, KeyAction>): Keymap {
        val n = bytes.copyOf()
        for ((index, action) in changes) {
            if (index !in 0..126) throw ProtocolException("Matris indeksi sınır dışında")
            if (index in F75.PROTECTED_MATRIX) throw ProtocolException("F1–F12 ve Fn tuşları korunur, değiştirilemez")
            action.record().copyInto(n, 8 + 4 * index)
        }
        return Keymap(n)
    }

    /** Fn tuşu ANSI konumunda (53) değilse eşleme/per-key yazma engellenir. */
    fun validateAnsiFnPosition() {
        val fn = raw(F75.FN_MATRIX_INDEX)
        val slot59 = raw(59)
        val fnMarker = byteArrayOf(13, 0, 0, 0)
        if (layer != Layer.BASE || !fn.contentEquals(fnMarker) || slot59.contentEquals(fnMarker))
            throw ProtocolException(
                "Fn tuşu beklenen ANSI konumunda değil (farklı revizyon/ISO düzen olabilir). " +
                    "Güvenlik için tuş atama ve tuş RGB yazma kapalı."
            )
    }

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
