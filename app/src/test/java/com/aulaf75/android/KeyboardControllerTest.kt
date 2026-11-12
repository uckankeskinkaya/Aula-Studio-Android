package com.aulaf75.android

import com.aulaf75.android.protocol.F75
import com.aulaf75.android.protocol.KeyAction
import com.aulaf75.android.protocol.Layer
import com.aulaf75.android.protocol.LightingSettings
import com.aulaf75.android.protocol.ProtocolException
import com.aulaf75.android.protocol.Rgb
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gerçek F75 gibi davranan sahte cihaz: okuma isteklerine yanıt verir, yazmaları saklar. */
class FakeF75 : ReportIo {
    var config = ByteArray(128).also {
        it[10] = 1; it[24] = 10; it[126] = 0x5a; it[127] = 0xa5.toByte()
        for (e in 0..21) { it[56 + e * 2] = 6; it[57 + e * 2] = 0x27 }
    }
    var palette = ByteArray(512).also { it[506] = 0x5a; it[507] = 0xa5.toByte(); for (i in 0 until 500) it[i] = (i * 7).toByte() }
    val keymaps = Layer.values().associateWith { layer ->
        ByteArray(520).also {
            byteArrayOf(6, 3, layer.byte.toByte(), 0, 1, 0, 0, 2).copyInto(it)
            it[518] = 0x5a; it[519] = 0xa5.toByte()
            for (u in 4..229) F75.matrixIndexForUsage(u)?.let { idx -> KeyAction(0, u).record().copyInto(it, 8 + 4 * idx) }
            it[8 + 4 * 53] = 13 // Fn
        }
    }.toMutableMap()
    var perKey = ByteArray(378) { (it % 200).toByte() }
    val writes = mutableListOf<Int>()
    var failWriteAt: Int? = null
    var identity = F75.F75_IDENTITY

    private fun reply(cmd: Int, target: Int, len: Int, payload: ByteArray) = ByteArray(520).also {
        byteArrayOf(6, cmd.toByte(), target.toByte(), 0, 1, 0, (len and 0xff).toByte(), (len shr 8).toByte()).copyInto(it)
        payload.copyInto(it, 8)
    }

    override fun query(request: ByteArray): ByteArray {
        assertEquals(520, request.size)
        return when (request[1].toInt() and 0xff) {
            0x82 -> reply(0x82, 1, 6, identity)
            0x84 -> reply(0x84, 0, 128, config)
            0x8a -> reply(0x8a, 0, 512, palette)
            0x83 -> keymaps[if (request[2].toInt() == 0) Layer.BASE else Layer.FN]!!.copyOf().also { it[1] = 0x83.toByte() }
            0x86 -> reply(0x86, 0, 378, perKey)
            else -> error("beklenmeyen istek")
        }
    }

    override fun write(report: ByteArray) {
        assertEquals(520, report.size)
        if (failWriteAt == writes.size) throw java.io.IOException("kablo çıktı")
        val cmd = report[1].toInt() and 0xff
        writes += cmd
        when (cmd) {
            0x04 -> config = report.copyOfRange(8, 136)
            0x0a -> palette = report.copyOfRange(8, 520)
            0x03 -> keymaps[if (report[2].toInt() == 0) Layer.BASE else Layer.FN] = report.copyOf()
            0x06 -> perKey = report.copyOfRange(8, 386)
            else -> error("izin verilmeyen komut ${"%02x".format(cmd)}")
        }
    }
}

class KeyboardControllerTest {
    private val dev = FakeF75()
    private val backups = mutableListOf<String>()
    private val c = KeyboardController(dev) { label, _ -> backups += label }

    @Test fun readAllAndIdentity() {
        assertArrayEquals(F75.F75_IDENTITY, c.readIdentity())
        val d = c.readAll()
        assertTrue(d.mappingAllowed)
        assertEquals(300, d.config.sleepSeconds())
        assertTrue(d.readErrors.isEmpty())
    }

    @Test fun brightnessWritesOnlyConfigAndOneByte() {
        val d = c.readAll()
        val before = dev.config.copyOf()
        val after = c.applyConfig(d, d.config.withPresetBrightness(3), d.palette, "b")
        assertEquals(listOf(0x04), dev.writes)
        for (i in 0 until 128) if (i != 58) assertEquals(before[i], dev.config[i])
        assertEquals(3, dev.config[58].toInt())
        assertEquals(3, after.config.lighting().brightness)
        assertEquals(listOf("b"), backups)
    }

    @Test fun colorWritesPaletteThenConfig() {
        val d = c.readAll()
        val cfg = d.config.withLighting(LightingSettings(2, 9, 1, false))
        val pal = d.palette.withEffectColor(2, Rgb(1, 2, 3))
        c.applyConfig(d, cfg, pal, "x")
        assertEquals(listOf(0x0a, 0x04), dev.writes)
        assertEquals(Rgb(1, 2, 3), c.readPalette().effectColor(2))
    }

    @Test fun unchangedSendsNothing() {
        val d = c.readAll()
        c.applyConfig(d, d.config, d.palette, "x")
        assertTrue(dev.writes.isEmpty())
        assertTrue(backups.isEmpty())
    }

    @Test fun staleStateIsRejectedWithoutWrites() {
        val d = c.readAll()
        dev.config[30] = 99 // başka bir uygulama değiştirmiş gibi
        assertThrows(ProtocolException::class.java) { c.applyConfig(d, d.config.withSleepSeconds(60), d.palette, "x") }
        assertTrue(dev.writes.isEmpty())
    }

    @Test fun interruptedWriteReportsAndDoesNotRetry() {
        val d = c.readAll()
        dev.failWriteAt = 1
        assertThrows(ProtocolException::class.java) {
            c.applyConfig(d, d.config.withLighting(LightingSettings(2, 9, 1, false)), d.palette.withEffectColor(2, Rgb(9, 9, 9)), "x")
        }
        assertEquals(listOf(0x0a), dev.writes)
    }

    @Test fun remapChangesOnlyRequestedSlot() {
        val d = c.readAll()
        val before = dev.keymaps[Layer.BASE]!!.copyOf()
        val capsIdx = F75.matrixIndexForUsage(57)!!
        val after = c.applyRemap(d, Layer.BASE, mapOf(capsIdx to KeyAction(0, 0xe0)), "r")
        assertEquals(listOf(0x03), dev.writes)
        val now = dev.keymaps[Layer.BASE]!!
        for (i in 0 until 520) if (i !in (8 + 4 * capsIdx) until (12 + 4 * capsIdx)) assertEquals(before[i], now[i])
        assertTrue(after.base!!.matches(capsIdx, KeyAction(0, 0xe0)))
    }

    @Test fun protectedKeysCannotBeRemapped() {
        val d = c.readAll()
        assertThrows(ProtocolException::class.java) { c.applyRemap(d, Layer.BASE, mapOf(53 to KeyAction(0, 4)), "r") }
        assertThrows(ProtocolException::class.java) { c.applyRemap(d, Layer.FN, mapOf(12 to KeyAction(0, 4)), "r") }
        assertTrue(dev.writes.isEmpty())
    }

    @Test fun isoKeyboardRemapsTurkishKeys() {
        val base = dev.keymaps[Layer.BASE]!!
        // ISO F75: 53 = AltGr (modifier 0x40), 59 = Fn, 75 = Non-US # (","), 76 = Non-US \\ ("<")
        byteArrayOf(0, 0x40, 0, 0).copyInto(base, 8 + 4 * 53)
        byteArrayOf(13, 0, 0, 0).copyInto(base, 8 + 4 * 59)
        byteArrayOf(0, 0, 0, 50).copyInto(base, 8 + 4 * 75)
        byteArrayOf(0, 0, 0, 100).copyInto(base, 8 + 4 * 76)
        val d = c.readAll()
        assertEquals(com.aulaf75.android.protocol.KeyboardLayout.ISO, d.layout)
        assertTrue(d.mappingAllowed)
        val after = c.applyRemap(d, Layer.BASE, mapOf(76 to KeyAction(2, 0xe9)), "r")
        assertTrue(after.base!!.matches(76, KeyAction(2, 0xe9)))
        assertThrows(ProtocolException::class.java) { c.applyRemap(after, Layer.BASE, mapOf(59 to KeyAction(0, 4)), "r") }
        assertThrows(ProtocolException::class.java) { c.applyRemap(after, Layer.BASE, mapOf(53 to KeyAction(0, 4)), "r") }
        val p = c.applyPerKey(after, mapOf(59 to Rgb(0, 255, 0), 76 to Rgb(255, 0, 0)), "p")
        assertEquals(Rgb(0, 255, 0), p.perKey!!.get(59))
        val (restored, _) = c.restore(p, d)
        assertEquals(d.base, restored.base)
        assertEquals(d.perKey, restored.perKey)
    }

    @Test fun unknownLayoutBlocksMapping() {
        dev.keymaps[Layer.BASE]!![8 + 4 * 53] = 0
        val d = c.readAll()
        assertTrue(!d.mappingAllowed)
        assertThrows(ProtocolException::class.java) { c.applyRemap(d, Layer.BASE, mapOf(9 to KeyAction(0, 5)), "r") }
        assertThrows(ProtocolException::class.java) { c.applyPerKey(d, mapOf(9 to Rgb(1, 1, 1)), "p") }
        assertTrue(dev.writes.isEmpty())
    }

    @Test fun perKeyWritesBankThenCustomMode() {
        val d = c.readAll()
        val after = c.applyPerKey(d, mapOf(9 to Rgb(255, 0, 0)), "p")
        assertEquals(listOf(0x06, 0x04), dev.writes)
        assertEquals(21, after.config.effectId)
        assertEquals(Rgb(255, 0, 0), after.perKey!!.get(9))
    }

    @Test fun restoreBringsEverythingBack() {
        val original = c.readAll()
        var d = c.applyPerKey(original, mapOf(9 to Rgb(1, 2, 3)), "p")
        d = c.applyRemap(d, Layer.FN, mapOf(9 to KeyAction(2, 0xcd)), "r")
        d = c.applyConfig(d, d.config.withSleepSeconds(60), d.palette, "s")
        val (after, parts) = c.restore(d, original)
        assertEquals(original.config, after.config)
        assertEquals(original.palette, after.palette)
        assertEquals(original.perKey, after.perKey)
        assertEquals(original.fn, after.fn)
        assertEquals(original.base, after.base)
        assertEquals(3, parts) // Fn keymap + per-key + lighting (base keymap was unchanged)
    }

    @Test fun restoreRejectsBackupThatDisablesKeys() {
        val d = c.readAll()
        val badBase = d.base!!.bytes.copyOf().also { it[8 + 4 * 9] = 0; it[8 + 4 * 9 + 3] = 0 }
        val saved = d.copy(base = com.aulaf75.android.protocol.Keymap.parse(badBase))
        assertThrows(ProtocolException::class.java) { c.restore(d, saved) }
        assertTrue(dev.writes.isEmpty())
    }
}
