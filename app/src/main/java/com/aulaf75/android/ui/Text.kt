package com.aulaf75.android.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.aulaf75.android.BackupReason
import com.aulaf75.android.R
import com.aulaf75.android.protocol.F75Error
import com.aulaf75.android.protocol.KeyAction
import com.aulaf75.android.protocol.KeyNames

@StringRes
fun errorRes(e: F75Error): Int = when (e) {
    F75Error.BAD_LENGTH, F75Error.BAD_HEADER -> R.string.err_bad_reply
    F75Error.BAD_CONFIG_SIGNATURE, F75Error.BAD_PALETTE_SIGNATURE, F75Error.BAD_KEYMAP_SIGNATURE -> R.string.err_bad_signature
    F75Error.UNKNOWN_EFFECT -> R.string.err_unknown_effect
    F75Error.VALUE_OUT_OF_RANGE -> R.string.err_value_range
    F75Error.UNSUPPORTED_SLEEP -> R.string.err_value_range
    F75Error.EFFECT_HAS_NO_COLOR -> R.string.err_effect_no_color
    F75Error.MODE_HAS_NO_BRIGHTNESS -> R.string.err_mode_no_brightness
    F75Error.INDEX_OUT_OF_RANGE -> R.string.err_bad_reply
    F75Error.PROTECTED_KEY -> R.string.err_protected_key
    F75Error.UNSUPPORTED_ACTION -> R.string.err_unsupported_action
    F75Error.LAYOUT_UNKNOWN, F75Error.MAPPING_UNAVAILABLE -> R.string.err_layout_unknown
    F75Error.STALE_STATE -> R.string.err_stale
    F75Error.WRITE_INTERRUPTED -> R.string.err_write_interrupted
    F75Error.READBACK_MISMATCH -> R.string.err_readback
    F75Error.BACKUP_PROTECTED_DIFFERS -> R.string.err_backup_protected
    F75Error.BACKUP_DISABLES_KEY -> R.string.err_backup_disables
    F75Error.BACKUP_INVALID -> R.string.err_backup_invalid
    F75Error.DEVICE_NOT_FOUND -> R.string.err_device_not_found
    F75Error.NO_USB_DEVICES -> R.string.err_no_usb
    F75Error.PERMISSION_DENIED -> R.string.err_permission
    F75Error.NOT_CONNECTED -> R.string.err_not_connected
    F75Error.IDENTITY_MISMATCH -> R.string.err_identity
    F75Error.INTERFACE_MISSING -> R.string.err_interface
    F75Error.OPEN_FAILED, F75Error.CLAIM_FAILED -> R.string.err_open
    F75Error.TRANSFER_FAILED -> R.string.err_transfer
}

@Composable
fun text(t: UiText): String = when (t) {
    is UiText.Res -> stringResource(t.id, *t.args.toTypedArray())
    is UiText.Error -> stringResource(errorRes(t.error)).let { msg ->
        if (t.error == F75Error.IDENTITY_MISMATCH && t.detail != null) "$msg (${t.detail})" else msg
    }
}

@Composable
@ReadOnlyComposable
fun isTurkish(): Boolean = LocalConfiguration.current.locales[0].language == "tr"

@Composable
fun effectName(id: Int): String {
    val names = stringArrayResource(R.array.effect_names)
    return when (id) {
        in names.indices -> names[id]
        21 -> stringResource(R.string.effect_custom)
        else -> "#$id"
    }
}

@Composable
fun effectDescription(id: Int): String = stringArrayResource(R.array.effect_descriptions).getOrElse(id) { "" }

@Composable
fun mediaName(usage: Int): String {
    val i = KeyNames.MEDIA_USAGES.indexOf(usage)
    return if (i >= 0) stringArrayResource(R.array.media_names)[i] else "0x%X".format(usage)
}

@Composable
fun mouseName(button: Int): String = stringArrayResource(R.array.mouse_names).getOrElse(button - 1) { "#$button" }

@Composable
fun actionName(a: KeyAction): String = when (a.page) {
    0 -> KeyNames.keyboardName(a.usage, isTurkish())
    1 -> mouseName(a.usage)
    else -> mediaName(a.usage)
}

/** Human description of a raw keymap record. */
@Composable
fun recordName(raw: ByteArray): String = when (val r = KeyNames.decode(raw)) {
    KeyNames.Record.Empty -> stringResource(R.string.record_empty)
    KeyNames.Record.Fn -> "Fn"
    is KeyNames.Record.Key -> KeyNames.modifierPrefix(r.modifiers) + KeyNames.keyboardName(r.usage, isTurkish())
    is KeyNames.Record.Mouse -> mouseName(r.button)
    is KeyNames.Record.Media -> mediaName(r.usage)
    is KeyNames.Record.Vendor -> stringResource(R.string.record_vendor)
}

/** Short key-cap label for the mapping editor. */
@Composable
fun recordShort(raw: ByteArray): String {
    val name = when (val r = KeyNames.decode(raw)) {
        is KeyNames.Record.Key -> KeyNames.modifierPrefix(r.modifiers) + KeyNames.keyboardName(r.usage, false)
        is KeyNames.Record.Vendor -> "•••"
        else -> recordName(raw)
    }
    return name.replace("Backspace", "Bksp").replace("Page ", "Pg").replace("Caps Lock", "Caps")
        .replace("Print Screen", "PrtSc").let { if (it.length > 8) it.take(7) + "…" else it }
}

@Composable
fun sleepLabel(seconds: Int): String = when {
    seconds == 0 -> stringResource(R.string.sleep_never)
    seconds % 60 == 0 -> stringResource(R.string.duration_min, seconds / 60)
    else -> stringResource(R.string.duration_min_sec, seconds / 60, seconds % 60)
}

@Composable
fun backupReasonLabel(r: BackupReason): String = stringResource(
    when (r) {
        BackupReason.ORIGINAL -> R.string.backup_original
        BackupReason.CONNECT -> R.string.backup_connect
        BackupReason.BRIGHTNESS -> R.string.backup_brightness
        BackupReason.LIGHTING -> R.string.backup_lighting
        BackupReason.SLEEP -> R.string.backup_sleep
        BackupReason.REMAP -> R.string.backup_remap
        BackupReason.PER_KEY -> R.string.backup_perkey
        BackupReason.RESTORE -> R.string.backup_restore
        BackupReason.UNKNOWN -> R.string.backup_unknown
    }
)
