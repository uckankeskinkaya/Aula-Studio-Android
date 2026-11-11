package com.aulaf75.android.protocol

/** Ekranda gösterilecek F75 tuşu. usage == null => Fn. */
data class PhysicalKey(
    val label: String,
    val usage: Int?,
    val matrixIndex: Int,
    val x: Float,
    val y: Float,
    val w: Float = 1f,
    val h: Float = 1f,
) {
    val id: String get() = usage?.toString() ?: "fn"
    val isProtected: Boolean get() = matrixIndex in F75.PROTECTED_MATRIX
}

/** F75'in iki fiziksel düzeni. ISO: Türkçe Q / Avrupa (AltGr + Fn, uzun Enter). */
enum class KeyboardLayout(val title: String) {
    ANSI("ANSI (ABD)"),
    ISO("ISO (Türkçe Q / Avrupa)");

    val keys: List<PhysicalKey> by lazy { F75Layout.build(this) }

    /** Atanabilir kaynak tuşların matris indeksleri (F1–F12 ve Fn konumları hariç). */
    val sourceIndices: Set<Int> by lazy { keys.filter { !it.isProtected }.map { it.matrixIndex }.toSet() }

    fun keyAt(matrixIndex: Int): PhysicalKey? = keys.firstOrNull { it.matrixIndex == matrixIndex }
}

object F75Layout {
    const val WIDTH = 16.25f
    const val HEIGHT = 6.3f

    /** ISO'ya özgü matris konumları (aula-studio FR/ISO KB.ini kanıtı). */
    private const val ISO_HASH_INDEX = 75 // HID 0x32 (Non-US #), TR klavyede ","
    private const val ISO_BACKSLASH_INDEX = 76 // HID 0x64 (Non-US \), TR klavyede "<"

    fun build(layout: KeyboardLayout): List<PhysicalKey> = buildList {
        val iso = layout == KeyboardLayout.ISO
        fun add(l: String, u: Int?, x: Float, y: Float, w: Float = 1f, h: Float = 1f, index: Int? = null) =
            add(PhysicalKey(l, u, index ?: F75.matrixIndexForUsage(u ?: -1)!!, x, y, w, h))

        add("Esc", 41, 0f, 0f)
        for (i in 1..12) add("F$i", 57 + i, 1.5f + i - 1 + ((i - 1) / 4) * 0.25f, 0f)
        val row1 = if (iso) listOf("\"", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "*", "-")
        else listOf("`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=")
        val row1Usages = listOf(53, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 45, 46)
        row1.forEachIndexed { i, l -> add(l, row1Usages[i], i.toFloat(), 1.3f) }
        add("Bksp", 42, 13f, 1.3f, 2f)
        add("Del", 76, 15.25f, 1.3f)

        add("Tab", 43, 0f, 2.3f, 1.5f)
        "QWERTYUIOP".forEachIndexed { i, c -> add(c.toString(), c.code - 61, 1.5f + i, 2.3f) }
        add(if (iso) "Ğ" else "[", 47, 11.5f, 2.3f)
        add(if (iso) "Ü" else "]", 48, 12.5f, 2.3f)
        if (iso) add("Enter", 40, 13.75f, 2.3f, 1.25f, 2f)
        else add("\\", 49, 13.5f, 2.3f, 1.5f)
        add("PgUp", 75, 15.25f, 2.3f)

        add("Caps", 57, 0f, 3.3f, 1.75f)
        "ASDFGHJKL".forEachIndexed { i, c -> add(c.toString(), c.code - 61, 1.75f + i, 3.3f) }
        add(if (iso) "Ş" else ";", 51, 10.75f, 3.3f)
        add(if (iso) "İ" else "'", 52, 11.75f, 3.3f)
        if (iso) add(",", 50, 12.75f, 3.3f, index = ISO_HASH_INDEX)
        else add("Enter", 40, 12.75f, 3.3f, 2.25f)
        add("PgDn", 78, 15.25f, 3.3f)

        if (iso) {
            add("Shift", 225, 0f, 4.3f, 1.25f)
            add("<", 100, 1.25f, 4.3f, index = ISO_BACKSLASH_INDEX)
        } else {
            add("Shift", 225, 0f, 4.3f, 2.25f)
        }
        "ZXCVBNM".forEachIndexed { i, c -> add(c.toString(), c.code - 61, 2.25f + i, 4.3f) }
        add(if (iso) "Ö" else ",", 54, 9.25f, 4.3f)
        add(if (iso) "Ç" else ".", 55, 10.25f, 4.3f)
        add(if (iso) "." else "/", 56, 11.25f, 4.3f)
        add("Shift", 229, 12.25f, 4.3f, 1.75f)
        add("↑", 82, 14.25f, 4.3f)
        add("End", 77, 15.25f, 4.3f)

        add("Ctrl", 224, 0f, 5.3f, 1.25f)
        add("Win", 227, 1.25f, 5.3f, 1.25f)
        add("Alt", 226, 2.5f, 5.3f, 1.25f)
        add("Space", 44, 3.75f, 5.3f, 6.25f)
        if (iso) {
            add("AltGr", 230, 10f, 5.3f, 1.25f, index = F75.FN_MATRIX_INDEX)
            add("Fn", null, 11.25f, 5.3f, 1.25f, index = F75.ISO_FN_MATRIX_INDEX)
        } else {
            add("Fn", null, 10f, 5.3f, 1.25f, index = F75.FN_MATRIX_INDEX)
            add("Ctrl", 228, 11.25f, 5.3f, 1.25f)
        }
        add("←", 80, 13.25f, 5.3f)
        add("↓", 81, 14.25f, 5.3f)
        add("→", 79, 15.25f, 5.3f)
    }
}

object KeyNames {
    val MEDIA = linkedMapOf(
        0xcd to "Oynat / duraklat",
        0xb7 to "Durdur",
        0xb6 to "Önceki parça",
        0xb5 to "Sonraki parça",
        0xe9 to "Ses +",
        0xea to "Ses −",
        0xe2 to "Sessiz",
        0x183 to "Medya oynatıcı",
        0x223 to "Tarayıcı ana sayfa",
        0x192 to "Hesap makinesi",
        0x18a to "E-posta",
        0x194 to "Dosya gezgini",
        0x6f to "Ekran parlaklığı +",
        0x70 to "Ekran parlaklığı −",
    )

    val MOUSE = linkedMapOf(1 to "Sol tık", 2 to "Sağ tık", 3 to "Orta tık", 4 to "Fare 4 (geri)", 5 to "Fare 5 (ileri)")

    private val KEYBOARD = buildMap {
        for (i in 0 until 26) put(4 + i, ('A' + i).toString())
        for (i in 0 until 9) put(30 + i, ('1' + i).toString())
        put(39, "0")
        putAll(
            mapOf(
                40 to "Enter", 41 to "Esc", 42 to "Backspace", 43 to "Tab", 44 to "Space", 45 to "- / *", 46 to "= / -",
                47 to "[ / Ğ", 48 to "] / Ü", 49 to "\\ (ANSI)", 50 to "# / , (ISO)", 51 to "; / Ş", 52 to "' / İ",
                53 to "` / \"", 54 to ", / Ö", 55 to ". / Ç", 56 to "/ / .", 57 to "Caps Lock", 70 to "Print Screen", 71 to "Scroll Lock", 72 to "Pause",
                73 to "Insert", 74 to "Home", 75 to "Page Up", 76 to "Delete", 77 to "End", 78 to "Page Down",
                79 to "→", 80 to "←", 81 to "↓", 82 to "↑", 83 to "Num Lock", 84 to "Num /", 85 to "Num *",
                86 to "Num -", 87 to "Num +", 88 to "Num Enter", 99 to "Num .", 100 to "\\ / < (ISO)",
                101 to "Menü (Uygulama)", 102 to "Power", 103 to "Num =",
                0xe0 to "Sol Ctrl", 0xe1 to "Sol Shift", 0xe2 to "Sol Alt", 0xe3 to "Sol Win",
                0xe4 to "Sağ Ctrl", 0xe5 to "Sağ Shift", 0xe6 to "Sağ Alt", 0xe7 to "Sağ Win",
            )
        )
        for (i in 0 until 12) put(58 + i, "F${i + 1}")
        for (i in 0 until 9) put(89 + i, "Num ${i + 1}")
        put(98, "Num 0")
        for (i in 0 until 12) put(104 + i, "F${13 + i}")
    }

    fun keyboardName(usage: Int): String = KEYBOARD[usage] ?: "Tuş 0x%02X".format(usage)

    /** Atama listesinde sunulan klavye çıktıları. */
    val ASSIGNABLE_KEYBOARD: List<Int> =
        (4..0x73).filter { it in KEYBOARD } + (0xe0..0xe7)

    fun actionName(a: KeyAction): String = when (a.page) {
        0 -> keyboardName(a.usage)
        1 -> MOUSE[a.usage] ?: "Fare ${a.usage}"
        2 -> MEDIA[a.usage] ?: "Medya 0x%X".format(a.usage)
        else -> "?"
    }

    /** Ham 4 baytlık kaydı okunabilir hale getirir (yalnızca gösterim). */
    fun describeRecord(r: ByteArray): String {
        val t = r[0].u(); val m = r[1].u(); val hi = r[2].u(); val lo = r[3].u()
        return when {
            t == 0 && hi == 0 && lo == 0 && m == 0 -> "(boş)"
            t == 0 && hi == 0 && lo == 0 && Integer.bitCount(m) == 1 -> keyboardName(0xe0 + Integer.numberOfTrailingZeros(m))
            t == 0 && hi == 0 -> modifierPrefix(m) + keyboardName(lo)
            t == 1 && hi == 1 && lo == 0 -> MOUSE[m] ?: "Fare $m"
            t == 2 && m == 0 -> MEDIA[(hi shl 8) or lo] ?: "Medya 0x%X".format((hi shl 8) or lo)
            t == 13 && m == 0 && hi == 0 && lo == 0 -> "Fn"
            else -> "Özel ${r.hex()}"
        }
    }

    private fun modifierPrefix(m: Int): String {
        if (m == 0) return ""
        val names = listOf("Ctrl", "Shift", "Alt", "Win", "RCtrl", "RShift", "RAlt", "RWin")
        return (0 until 8).filter { m and (1 shl it) != 0 }.joinToString("+") { names[it] } + "+"
    }

    /** Tuş üstünde gösterilecek kısa etiket. */
    fun shortLabel(r: ByteArray): String = describeRecord(r)
        .replace("Sol ", "L").replace("Sağ ", "R").replace("Backspace", "Bksp")
        .replace("Page ", "Pg").replace("Caps Lock", "Caps").let { if (it.length > 7) it.take(6) + "…" else it }
}
