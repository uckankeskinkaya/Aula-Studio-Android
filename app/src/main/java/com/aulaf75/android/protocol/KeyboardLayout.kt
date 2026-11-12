package com.aulaf75.android.protocol

/** One physical key as drawn on screen. [usage] is null for Fn. Geometry is in key units. */
data class PhysicalKey(
    val label: String,
    val usage: Int?,
    val matrixIndex: Int,
    val x: Float,
    val y: Float,
    val w: Float = 1f,
    val h: Float = 1f,
) {
    val isProtected: Boolean get() = matrixIndex in F75.PROTECTED_MATRIX

    /** Key-cap legend; Turkish-Q legends are used on ISO boards when [turkish] is requested. */
    fun legend(turkish: Boolean): String =
        if (turkish && usage != null && usage !in 4..29 && usage != 0x2c) KeyNames.TURKISH_HINTS[usage] ?: label else label
}

/** The two physical F75 layouts. ISO = European / Turkish-Q (AltGr + Fn, tall Enter, extra key by left Shift). */
enum class KeyboardLayout {
    ANSI, ISO;

    val keys: List<PhysicalKey> by lazy { F75Geometry.build(this) }

    /** Remappable source slots (everything except F1–F12 and the Fn slots). */
    val sourceIndices: Set<Int> by lazy { keys.filter { !it.isProtected }.map { it.matrixIndex }.toSet() }

    fun keyAt(matrixIndex: Int): PhysicalKey? = keys.firstOrNull { it.matrixIndex == matrixIndex }
}

object F75Geometry {
    const val WIDTH = 16.25f
    const val HEIGHT = 6.3f

    /** ISO-only matrix slots (upstream FR/ISO KB.ini evidence). */
    private const val ISO_HASH_INDEX = 75 // HID 0x32 Non-US #
    private const val ISO_BACKSLASH_INDEX = 76 // HID 0x64 Non-US \

    fun build(layout: KeyboardLayout): List<PhysicalKey> = buildList {
        val iso = layout == KeyboardLayout.ISO
        fun key(l: String, u: Int?, x: Float, y: Float, w: Float = 1f, h: Float = 1f, index: Int? = null) =
            add(PhysicalKey(l, u, index ?: F75.matrixIndexForUsage(u ?: -1)!!, x, y, w, h))

        key("Esc", 41, 0f, 0f)
        for (i in 1..12) key("F$i", 57 + i, 1.5f + i - 1 + ((i - 1) / 4) * 0.25f, 0f)
        listOf("`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=")
            .zip(listOf(53, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 45, 46))
            .forEachIndexed { i, (l, u) -> key(l, u, i.toFloat(), 1.3f) }
        key("Backspace", 42, 13f, 1.3f, 2f)
        key("Del", 76, 15.25f, 1.3f)

        key("Tab", 43, 0f, 2.3f, 1.5f)
        "QWERTYUIOP".forEachIndexed { i, c -> key(c.toString(), c.code - 61, 1.5f + i, 2.3f) }
        key("[", 47, 11.5f, 2.3f)
        key("]", 48, 12.5f, 2.3f)
        if (iso) key("Enter", 40, 13.75f, 2.3f, 1.25f, 2f) else key("\\", 49, 13.5f, 2.3f, 1.5f)
        key("PgUp", 75, 15.25f, 2.3f)

        key("Caps", 57, 0f, 3.3f, 1.75f)
        "ASDFGHJKL".forEachIndexed { i, c -> key(c.toString(), c.code - 61, 1.75f + i, 3.3f) }
        key(";", 51, 10.75f, 3.3f)
        key("'", 52, 11.75f, 3.3f)
        if (iso) key("#", 50, 12.75f, 3.3f, index = ISO_HASH_INDEX) else key("Enter", 40, 12.75f, 3.3f, 2.25f)
        key("PgDn", 78, 15.25f, 3.3f)

        if (iso) {
            key("Shift", 225, 0f, 4.3f, 1.25f)
            key("\\", 100, 1.25f, 4.3f, index = ISO_BACKSLASH_INDEX)
        } else {
            key("Shift", 225, 0f, 4.3f, 2.25f)
        }
        "ZXCVBNM".forEachIndexed { i, c -> key(c.toString(), c.code - 61, 2.25f + i, 4.3f) }
        key(",", 54, 9.25f, 4.3f)
        key(".", 55, 10.25f, 4.3f)
        key("/", 56, 11.25f, 4.3f)
        key("Shift", 229, 12.25f, 4.3f, 1.75f)
        key("↑", 82, 14.25f, 4.3f)
        key("End", 77, 15.25f, 4.3f)

        key("Ctrl", 224, 0f, 5.3f, 1.25f)
        key("Win", 227, 1.25f, 5.3f, 1.25f)
        key("Alt", 226, 2.5f, 5.3f, 1.25f)
        key("", 44, 3.75f, 5.3f, 6.25f)
        if (iso) {
            key("AltGr", 230, 10f, 5.3f, 1.25f, index = F75.FN_MATRIX_INDEX)
            key("Fn", null, 11.25f, 5.3f, 1.25f, index = F75.ISO_FN_MATRIX_INDEX)
        } else {
            key("Fn", null, 10f, 5.3f, 1.25f, index = F75.FN_MATRIX_INDEX)
            key("Ctrl", 228, 11.25f, 5.3f, 1.25f)
        }
        key("←", 80, 13.25f, 5.3f)
        key("↓", 81, 14.25f, 5.3f)
        key("→", 79, 15.25f, 5.3f)
    }
}
