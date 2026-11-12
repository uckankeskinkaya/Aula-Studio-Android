package com.aulaf75.android.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.aulaf75.android.ReportIo
import com.aulaf75.android.protocol.F75
import com.aulaf75.android.protocol.F75Error
import com.aulaf75.android.protocol.ProtocolException
import java.io.Closeable

/**
 * Android transport: HID feature reports on the keyboard's configuration interface (1) via USB
 * control transfers (SET_REPORT / GET_REPORT, report type Feature). This is the Android equivalent
 * of hidapi's send_feature_report / get_feature_report used by the desktop app.
 */
object F75Usb {
    /** The known wired F75 first; otherwise another device from the same vendor exposing interface 1.
     *  The six-byte model identity read after opening is what actually authorises configuration. */
    fun find(manager: UsbManager): UsbDevice? {
        val all = manager.deviceList.values
        return all.firstOrNull { it.vendorId == F75.VENDOR_ID && it.productId == F75.PRODUCT_ID }
            ?: all.firstOrNull { it.vendorId == F75.VENDOR_ID && hasInterface(it, F75.CONFIG_INTERFACE) }
    }

    private fun hasInterface(d: UsbDevice, id: Int) = (0 until d.interfaceCount).any { d.getInterface(it).id == id }

    data class UsbEntry(val vendorId: Int, val productId: Int, val name: String, val interfaces: String, val kind: Kind) {
        enum class Kind { F75_WIRED, AULA_OTHER, AULA_RECEIVER, OTHER }
        val ids: String get() = "%04x:%04x".format(vendorId, productId)
    }

    /** Diagnostics: every USB device the OS currently exposes. */
    fun describeAll(manager: UsbManager): List<UsbEntry> = manager.deviceList.values.map { d ->
        val intfs = (0 until d.interfaceCount).joinToString(", ") {
            val i = d.getInterface(it)
            "#${i.id} class ${i.interfaceClass}"
        }
        val name = listOfNotNull(d.manufacturerName, d.productName).joinToString(" ").ifBlank { "—" }
        val kind = when {
            d.vendorId == F75.VENDOR_ID && d.productId == F75.PRODUCT_ID -> UsbEntry.Kind.F75_WIRED
            d.vendorId == F75.VENDOR_ID -> UsbEntry.Kind.AULA_OTHER
            d.vendorId == F75.RECEIVER_VENDOR_ID -> UsbEntry.Kind.AULA_RECEIVER
            else -> UsbEntry.Kind.OTHER
        }
        UsbEntry(d.vendorId, d.productId, name, intfs, kind)
    }

    fun open(manager: UsbManager, device: UsbDevice): F75Connection {
        val intf = (0 until device.interfaceCount).map { device.getInterface(it) }
            .firstOrNull { it.id == F75.CONFIG_INTERFACE && it.alternateSetting == 0 }
            ?: throw ProtocolException(F75Error.INTERFACE_MISSING)
        val conn = manager.openDevice(device) ?: throw ProtocolException(F75Error.OPEN_FAILED)
        // Only the configuration interface is claimed; the typing interface (0) is never touched.
        if (!conn.claimInterface(intf, true)) {
            conn.close()
            throw ProtocolException(F75Error.CLAIM_FAILED)
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
        if (n != report.size) throw ProtocolException(F75Error.TRANSFER_FAILED, "SET_REPORT $n")
    }

    private fun getFeature(id: Int): ByteArray {
        val buf = ByteArray(F75.REPORT_SIZE)
        buf[0] = id.toByte()
        val n = conn.controlTransfer(0xA1, 0x01, 0x0300 or id, intf.id, buf, buf.size, timeoutMs)
        if (n <= 0) throw ProtocolException(F75Error.TRANSFER_FAILED, "GET_REPORT $n")
        // Some stacks drop the report-ID byte; restore the hidapi layout.
        if (n == F75.REPORT_SIZE - 1 && buf[0].toInt() != id) return byteArrayOf(id.toByte()) + buf.copyOf(n)
        return buf.copyOf(n)
    }

    /** Read: send the request, wait 20 ms, fetch the reply (upstream timing). */
    @Synchronized
    override fun query(request: ByteArray): ByteArray {
        setFeature(request)
        Thread.sleep(20)
        return getFeature(F75.REPORT_ID)
    }

    /** Write: send the report and wait 70 ms before the next request (upstream timing). */
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
