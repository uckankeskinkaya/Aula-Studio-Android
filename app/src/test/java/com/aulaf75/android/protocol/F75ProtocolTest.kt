package com.aulaf75.android.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testler, aula-studio Rust test fikstürlerinden aktarılmıştır. */
class F75ProtocolTest {
    private fun b(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    private fun demoConfig(): ByteArray {
        val bytes = ByteArray(128)
        bytes[10] = 1
        bytes[126] = 0x5a; bytes[127] = 0xa5.toByte()
        for (e in 0..21) { bytes[56 + e * 2] = 5; bytes[57 + e * 2] = 0x27 }
        return bytes
    }

    @Test fun headers() {
        assertArrayEquals(b(6, 0x84, 0, 0, 1, 0, 0x80, 0), F75.configReadRequest().copyOf(8))
        assertArrayEquals(b(6, 0x8a, 0, 0, 1, 0, 0, 2), F75.paletteReadRequest().copyOf(8))
        assertArrayEquals(b(6, 0x83, 1, 0, 1, 0, 0, 2), F75.keymapReadRequest(Layer.FN).copyOf(8))
        assertArrayEquals(b(6, 0x86, 0, 0, 1, 0, 0x7a, 1), F75.perKeyReadRequest().copyOf(8))
        assertArrayEquals(b(6, 0x82, 1, 0, 1, 0, 6, 0), F75.identityRequest().copyOf(8))
        assertEquals(520, F75.configReadRequest().size)
        assertTrue(F75.configReadRequest().drop(8).all { it == 0.toByte() })
    }

    @Test fun lightingPreservesUnrelatedBytes() {
        val old = ConfigRegion.parse(demoConfig())
        val next = old.withLighting(LightingSettings(7, 9, 4, false))
        for (i in 0 until 128) if (i !in listOf(9, 10, 70, 71)) assertEquals(old.bytes[i], next.bytes[i])
        assertEquals(LightingSettings(7, 9, 4, false), next.lighting())
        assertEquals(0x40, next.bytes[71].toInt() and 0xff)
    }

    @Test fun effect1KeepsSpeed() {
        val next = ConfigRegion.parse(demoConfig()).withLighting(LightingSettings(1, 3, 4, true))
        assertEquals(0x27, next.bytes[59].toInt())
        assertEquals(3, next.bytes[58].toInt())
    }

    @Test fun customModeSetsFlagAndGroup() {
        val next = ConfigRegion.parse(demoConfig()).withLighting(LightingSettings(21, 0, 0, false))
        assertEquals(1, next.bytes[9].toInt()); assertEquals(21, next.bytes[10].toInt()); assertEquals(0x20, next.bytes[11].toInt())
    }

    @Test fun sleep() {
        val c = ConfigRegion.parse(demoConfig()).withSleepSeconds(300)
        assertEquals(10, c.bytes[24].toInt()); assertEquals(300, c.sleepSeconds())
        assertThrows(ProtocolException::class.java) { c.withSleepSeconds(30) }
    }

    @Test fun configSignatureRequired() {
        val bad = demoConfig().also { it[127] = 0 }
        assertThrows(ProtocolException::class.java) { ConfigRegion.parse(bad) }
    }

    @Test fun decodeConfigStrict() {
        val report = F75.configWriteReport(ConfigRegion.parse(demoConfig()))
        assertEquals(ConfigRegion.parse(demoConfig()), F75.decodeConfig(report))
        val bad = report.copyOf().also { it[2] = 1 }
        assertThrows(ProtocolException::class.java) { F75.decodeConfig(bad) }
        assertThrows(ProtocolException::class.java) { F75.decodeConfig(report.copyOf(519)) }
    }

    @Test fun paletteSignatureAndColor() {
        val bytes = ByteArray(512).also { it[506] = 0x5a; it[507] = 0xa5.toByte() }
        val p = ColorTable.parse(bytes).withEffectColor(1, Rgb(17, 34, 51))
        assertEquals(Rgb(17, 34, 51), p.effectColor(1))
        assertEquals(17, p.bytes[21].toInt())
        val wrong = ByteArray(512).also { it[510] = 0x5a; it[511] = 0xa5.toByte() }
        assertThrows(ProtocolException::class.java) { ColorTable.parse(wrong).withEffectColor(1, Rgb(1, 1, 1)) }
        assertThrows(ProtocolException::class.java) { ColorTable.parse(bytes).withEffectColor(3, Rgb(1, 1, 1)) }
    }

    @Test fun keyRecordsMatchNativeConverter() {
        assertArrayEquals(0x83010002.toInt().le(), KeyAction(2, 0x183).record())
        assertArrayEquals(0xcd000002.toInt().le(), KeyAction(2, 0xcd).record())
        assertArrayEquals(0x10301.le(), KeyAction(1, 3).record())
        assertArrayEquals(b(0, 0x10, 0, 0), KeyAction(0, 0xe4).record())
        assertArrayEquals(b(0, 0, 0, 0x68), KeyAction(0, 0x68).record())
        assertThrows(ProtocolException::class.java) { KeyAction(0, 0).record() }
        assertThrows(ProtocolException::class.java) { KeyAction(2, 0x1cd).record() }
    }

    private fun Int.le() = b(this and 0xff, this shr 8 and 0xff, this shr 16 and 0xff, this ushr 24 and 0xff)

    private fun emptyKeymap(layer: Layer): ByteArray = ByteArray(520).also {
        b(6, 3, layer.byte, 0, 1, 0, 0, 2).copyInto(it)
        it[518] = 0x5a; it[519] = 0xa5.toByte()
    }

    @Test fun keymapPatchAndProtection() {
        val map = Keymap.parse(emptyKeymap(Layer.BASE))
        val next = map.with(mapOf(9 to KeyAction(0, 5)))
        assertTrue(next.matches(9, KeyAction(0, 5)))
        for (i in 0 until 520) if (i !in 44..47) assertEquals(map.bytes[i], next.bytes[i])
        assertThrows(ProtocolException::class.java) { map.with(mapOf(12 to KeyAction(0, 4))) }
        assertThrows(ProtocolException::class.java) { map.with(mapOf(53 to KeyAction(0, 4))) }
        assertThrows(ProtocolException::class.java) { map.with(mapOf(127 to KeyAction(0, 4))) }
    }

    @Test fun keymapDecodeNormalizesCommand() {
        val reply = emptyKeymap(Layer.FN).also { it[1] = 0x83.toByte() }
        val map = F75.decodeKeymap(Layer.FN, reply)
        assertEquals(Layer.FN, map.layer)
        assertEquals(3, map.bytes[1].toInt())
        assertThrows(ProtocolException::class.java) { F75.decodeKeymap(Layer.BASE, reply) }
    }

    @Test fun ansiFnValidation() {
        val raw = emptyKeymap(Layer.BASE)
        assertThrows(ProtocolException::class.java) { Keymap.parse(raw).validateAnsiFnPosition() }
        raw[8 + 4 * 53] = 13
        Keymap.parse(raw).validateAnsiFnPosition()
        raw[8 + 4 * 59] = 13
        assertThrows(ProtocolException::class.java) { Keymap.parse(raw).validateAnsiFnPosition() }
    }

    @Test fun perKeyPlanes() {
        val c = PerKeyColors.parse(ByteArray(378)).with(mapOf(53 to Rgb(1, 2, 3)))
        assertEquals(1, c.bytes[53].toInt()); assertEquals(2, c.bytes[126 + 53].toInt()); assertEquals(3, c.bytes[252 + 53].toInt())
        val report = F75.perKeyWriteReport(c)
        assertArrayEquals(b(6, 6, 0, 0, 1, 0, 0x7a, 1), report.copyOf(8))
        assertEquals(c, F75.decodePerKey(report))
    }

    @Test fun matrixTable() {
        assertEquals(9, F75.matrixIndexForUsage(4)) // A
        assertEquals(10, F75.matrixIndexForUsage(29)) // Z
        assertEquals(59, F75.matrixIndexForUsage(228)) // Sağ Ctrl
        assertEquals(80, F75.matrixIndexForUsage(49)) // ANSI \
        assertEquals(null, F75.matrixIndexForUsage(50))
        val indices = F75Layout.KEYS.map { it.matrixIndex }
        assertEquals(80, indices.size) // 79 ANSI tuşu + Fn
        assertEquals(indices.size, indices.toSet().size)
        assertEquals(13, F75Layout.KEYS.count { it.isProtected })
        assertFalse(F75Layout.KEYS.first { it.label == "A" }.isProtected)
    }

    @Test fun identity() {
        val reply = F75.identityRequest().also { F75.F75_IDENTITY.copyInto(it, 8) }
        assertArrayEquals(F75.F75_IDENTITY, F75.decodeIdentity(reply))
        assertThrows(ProtocolException::class.java) { F75.decodeIdentity(reply.copyOf(13)) }
    }
}
