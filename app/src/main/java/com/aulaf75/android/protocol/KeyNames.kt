package com.aulaf75.android.protocol

/**
 * Language-neutral key naming. Key-cap symbols ("A", "F5", "Enter") are shown as-is in every
 * language; anything that needs words (media actions, mouse buttons) is localized by the UI from
 * the codes exposed here.
 */
object KeyNames {
    /** Consumer actions emitted by the official converter (record type 2). Order = display order. */
    val MEDIA_USAGES = listOf(0xcd, 0xb7, 0xb6, 0xb5, 0xe9, 0xea, 0xe2, 0x183, 0x223, 0x192, 0x18a, 0x194, 0x6f, 0x70)

    /** The five ordinary mouse-button records (record type 1). */
    val MOUSE_BUTTONS = 1..5

    private val SYMBOLS: Map<Int, String> = buildMap {
        for (i in 0 until 26) put(4 + i, ('A' + i).toString())
        for (i in 0 until 9) put(30 + i, ('1' + i).toString())
        put(39, "0")
        putAll(
            mapOf(
                40 to "Enter", 41 to "Esc", 42 to "Backspace", 43 to "Tab", 44 to "Space", 45 to "-", 46 to "=",
                47 to "[", 48 to "]", 49 to "\\", 50 to "# (ISO)", 51 to ";", 52 to "'", 53 to "`", 54 to ",",
                55 to ".", 56 to "/", 57 to "Caps Lock", 70 to "Print Screen", 71 to "Scroll Lock", 72 to "Pause",
                73 to "Insert", 74 to "Home", 75 to "Page Up", 76 to "Delete", 77 to "End", 78 to "Page Down",
                79 to "→", 80 to "←", 81 to "↓", 82 to "↑", 83 to "Num Lock", 84 to "Num /", 85 to "Num *",
                86 to "Num −", 87 to "Num +", 88 to "Num Enter", 98 to "Num 0", 99 to "Num .", 100 to "\\ (ISO)",
                101 to "Menu", 102 to "Power", 103 to "Num =",
                0xe0 to "L-Ctrl", 0xe1 to "L-Shift", 0xe2 to "L-Alt", 0xe3 to "L-Win",
                0xe4 to "R-Ctrl", 0xe5 to "R-Shift", 0xe6 to "AltGr", 0xe7 to "R-Win",
            )
        )
        for (i in 0 until 12) put(58 + i, "F${i + 1}")
        for (i in 0 until 9) put(89 + i, "Num ${i + 1}")
        for (i in 0 until 12) put(104 + i, "F${13 + i}")
    }

    /** Turkish-Q legends for keys whose US symbol differs; shown as a hint when the UI is Turkish. */
    val TURKISH_HINTS: Map<Int, String> = mapOf(
        45 to "*", 46 to "-", 47 to "Ğ", 48 to "Ü", 49 to ",", 50 to ",", 51 to "Ş", 52 to "İ", 53 to "\"",
        54 to "Ö", 55 to "Ç", 56 to ".", 100 to "<",
    )

    fun keyboardName(usage: Int, turkishHints: Boolean = false): String {
        val base = SYMBOLS[usage] ?: "Key 0x%02X".format(usage)
        val hint = if (turkishHints) TURKISH_HINTS[usage] else null
        return if (hint != null) "$base  ($hint)" else base
    }

    /** Keyboard outputs offered in the assignment picker. */
    val ASSIGNABLE_KEYBOARD: List<Int> = (4..0x73).filter { it in SYMBOLS } + (0xe0..0xe7)

    /** Structured meaning of a raw four-byte keymap record, formatted by the UI. */
    sealed interface Record {
        data object Empty : Record
        data object Fn : Record
        data class Key(val usage: Int, val modifiers: Int) : Record
        data class Mouse(val button: Int) : Record
        data class Media(val usage: Int) : Record
        data class Vendor(val hex: String) : Record
    }

    fun decode(r: ByteArray): Record {
        val t = r[0].u(); val m = r[1].u(); val hi = r[2].u(); val lo = r[3].u()
        return when {
            t == 0 && m == 0 && hi == 0 && lo == 0 -> Record.Empty
            t == 0 && hi == 0 && lo == 0 && Integer.bitCount(m) == 1 -> Record.Key(0xe0 + Integer.numberOfTrailingZeros(m), 0)
            t == 0 && hi == 0 -> Record.Key(lo, m)
            t == 1 && hi == 1 && lo == 0 -> Record.Mouse(m)
            t == 2 && m == 0 -> Record.Media((hi shl 8) or lo)
            t == 13 && m == 0 && hi == 0 && lo == 0 -> Record.Fn
            else -> Record.Vendor(r.hex())
        }
    }

    /** "Ctrl+Shift+" style prefix for modifier masks inside combined records. */
    fun modifierPrefix(mask: Int): String {
        if (mask == 0) return ""
        val names = listOf("Ctrl", "Shift", "Alt", "Win", "RCtrl", "RShift", "AltGr", "RWin")
        return (0 until 8).filter { mask and (1 shl it) != 0 }.joinToString("+") { names[it] } + "+"
    }
}
