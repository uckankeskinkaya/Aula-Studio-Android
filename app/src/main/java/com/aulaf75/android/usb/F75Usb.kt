package com.aulaf75.android.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.aulaf75.android.ReportIo
import com.aulaf75.android.protocol.F75
import java.io.Closeable
import java.io.IOException

/**
 * Kablolu F75'in yapılandırma arayüzüne (arayüz 1) HID feature report
 * gönderir/alır. Masaüstündeki hidapi send/get_feature_report çağrılarının
 * USB kontrol aktarımı karşılığıdır (SET_REPORT / GET_REPORT, Feature tipi).
 */
object F75Usb {
    /** Önce bilinen kablolu F75 (258a:010c), yoksa aynı üreticinin başka bir PID'i denenir.
     *  Asıl doğrulama bağlantıdan sonra model kimliği okumasıyla yapılır. */
    fun find(manager: UsbManager): UsbDevice? {
        val all = manager.deviceList.values
        return all.firstOrNull { it.vendorId == F75.VENDOR_ID && it.productId == F75.PRODUCT_ID }
            ?: all.firstOrNull { it.vendorId == F75.VENDOR_ID && hasInterface(it, F75.CONFIG_INTERFACE) }
    }

    private fun hasInterface(d: UsbDevice, id: Int) = (0 until d.interfaceCount).any { d.getInterface(it).id == id }

    /** Teşhis: tabletin gördüğü tüm USB cihazları. */
    fun describeAll(manager: UsbManager): List<String> = manager.deviceList.values.map { d ->
        val intfs = (0 until d.interfaceCount).joinToString(", ") {
            val i = d.getInterface(it)
            "#${i.id} sınıf ${i.interfaceClass}"
        }
        val name = listOfNotNull(d.manufacturerName, d.productName).joinToString(" ").ifBlank { "(adsız)" }
        val note = when {
            d.vendorId == F75.VENDOR_ID && d.productId == F75.PRODUCT_ID -> " ← kablolu F75"
            d.vendorId == F75.VENDOR_ID -> " ← AULA üreticisi, farklı PID"
            d.vendorId == 0x3554 -> " ← AULA 2.4G alıcısı (ayar için kablo gerekir)"
            else -> ""
        }
        "%04x:%04x %s [%s]%s".format(d.vendorId, d.productId, name, intfs, note)
    }

    fun open(manager: UsbManager, device: UsbDevice): F75Connection {
        val intf = (0 until device.interfaceCount).map { device.getInterface(it) }
            .firstOrNull { it.id == F75.CONFIG_INTERFACE && it.alternateSetting == 0 }
            ?: throw IOException("Yapılandırma arayüzü (1) bulunamadı. Klavye kablo modunda mı?")
        val conn = manager.openDevice(device) ?: throw IOException("USB cihazı açılamadı (izin verildi mi?)")
        // Yalnızca yapılandırma arayüzü alınır; yazı yazan klavye arayüzü (0) dokunulmaz.
        if (!conn.claimInterface(intf, true)) {
            conn.close()
            throw IOException("Arayüz 1 alınamadı. Başka bir uygulama kullanıyor olabilir.")
        }
        return F75Connection(conn, intf)
    }
}

class F75Connection(private val conn: UsbDeviceConnection, private val intf: UsbInterface) : Closeable, ReportIo {
    private val timeoutMs = 2000

    private fun setFeature(report: ByteArray) {
        require(report.size == F75.REPORT_SIZE)
        val id = report[0].toInt() and 0xff
        val n = conn.controlTransfer(0x21, 0x09, 0x0300 or id, intf.id, report, report.size, timeoutMs)
        if (n != report.size) throw IOException("SET_REPORT başarısız (sonuç $n)")
    }

    private fun getFeature(id: Int): ByteArray {
        val buf = ByteArray(F75.REPORT_SIZE)
        buf[0] = id.toByte()
        val n = conn.controlTransfer(0xA1, 0x01, 0x0300 or id, intf.id, buf, buf.size, timeoutMs)
        if (n <= 0) throw IOException("GET_REPORT başarısız (sonuç $n)")
        // Bazı yığınlar rapor kimliği baytını atlayabilir; masaüstü biçimine çevir.
        if (n == F75.REPORT_SIZE - 1 && buf[0].toInt() != id) return byteArrayOf(id.toByte()) + buf.copyOf(n)
        return buf.copyOf(n)
    }

    /** Okuma: istek gönder, 20 ms bekle, yanıtı al (orijinal uygulamadaki zamanlama). */
    @Synchronized
    override fun query(request: ByteArray): ByteArray {
        setFeature(request)
        Thread.sleep(20)
        return getFeature(F75.REPORT_ID)
    }

    /** Yazma: raporu gönder, sonraki işlemden önce 70 ms bekle. */
    @Synchronized
    override fun write(report: ByteArray) {
        setFeature(report)
        Thread.sleep(70)
    }

    override fun close() {
        runCatching { conn.releaseInterface(intf) }
        runCatching { conn.close() }
    }
}
