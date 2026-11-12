@file:OptIn(ExperimentalLayoutApi::class)

package com.aulaf75.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aulaf75.android.BackupEntry
import com.aulaf75.android.R
import com.aulaf75.android.usb.F75Usb
import java.text.DateFormat
import java.util.Date

/** In-app languages: tag -> native name. "" follows the system. */
val LANGUAGES = listOf("" to null, "en" to "English", "tr" to "Türkçe", "zh-CN" to "简体中文")

@Composable
fun SettingsScreen(state: UiState, actions: AppActions, wide: Boolean) {
    if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LanguageCard(state, actions); AppearanceCard(state, actions); ExperimentalCard(state, actions); AboutCard(state, actions)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BackupsCard(state, actions); SafetyCard(); UsbCard(state, actions)
        }
    } else {
        LanguageCard(state, actions); AppearanceCard(state, actions); ExperimentalCard(state, actions)
        BackupsCard(state, actions); SafetyCard(); UsbCard(state, actions); AboutCard(state, actions)
    }
}

@Composable
private fun LanguageCard(state: UiState, actions: AppActions) {
    SectionCard(stringResource(R.string.settings_language), null, Icons.Rounded.Translate) {
        Column {
            LANGUAGES.forEach { (tag, name) ->
                val selected = state.language == tag || (tag.isNotEmpty() && state.language.startsWith(tag))
                Row(
                    Modifier.fillMaxWidth()
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { actions.setLanguage(tag) })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(name ?: stringResource(R.string.language_system), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun AppearanceCard(state: UiState, actions: AppActions) {
    SectionCard(stringResource(R.string.settings_theme), null, Icons.Rounded.Contrast) {
        val modes = listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, (mode, label) ->
                SegmentedButton(state.themeMode == mode, { actions.setThemeMode(mode) }, SegmentedButtonDefaults.itemShape(i, modes.size)) {
                    Text(stringResource(label))
                }
            }
        }
    }
}

@Composable
private fun ExperimentalCard(state: UiState, actions: AppActions) {
    SectionCard(stringResource(R.string.experimental_title), stringResource(R.string.experimental_sub), Icons.Rounded.Science) {
        ExperimentalSwitchRow(state, actions)
    }
}

@Composable
private fun BackupsCard(state: UiState, actions: AppActions) {
    var confirm by remember { mutableStateOf<BackupEntry?>(null) }
    SectionCard(stringResource(R.string.backups_title), stringResource(R.string.backups_sub), Icons.Rounded.Backup) {
        if (state.backups.isEmpty()) Text(stringResource(R.string.backups_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.backups.take(12).forEachIndexed { i, b ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (b.isOriginal) Icons.Rounded.Star else Icons.Rounded.History, null,
                    tint = if (b.isOriginal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(backupReasonLabel(b.reason), style = MaterialTheme.typography.bodyLarge)
                    b.timeMillis?.let {
                        Text(
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = { confirm = b }, enabled = state.connected && !state.busy) { Text(stringResource(R.string.action_restore)) }
                IconButton(onClick = { actions.shareBackup(b) }) { Icon(Icons.Rounded.Share, stringResource(R.string.action_share)) }
                if (!b.isOriginal) IconButton(onClick = { actions.deleteBackup(b) }) { Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete)) }
            }
        }
        if (!state.connected) Text(stringResource(R.string.backups_connect_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    confirm?.let { b ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            icon = { Icon(Icons.Rounded.History, null) },
            title = { Text(stringResource(R.string.restore_dialog_title)) },
            text = { Text(stringResource(R.string.restore_dialog_body, backupReasonLabel(b.reason))) },
            confirmButton = { TextButton(onClick = { actions.restore(b); confirm = null }) { Text(stringResource(R.string.action_restore)) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SafetyCard() {
    SectionCard(stringResource(R.string.safety_title), null, Icons.Rounded.Shield) {
        listOf(R.string.safety_1, R.string.safety_2, R.string.safety_3, R.string.safety_4, R.string.safety_5).forEach {
            Row {
                Text("•", Modifier.width(16.dp), color = MaterialTheme.colorScheme.primary)
                Text(stringResource(it), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun UsbCard(state: UiState, actions: AppActions) {
    SectionCard(stringResource(R.string.usb_title), stringResource(R.string.usb_sub), Icons.Rounded.Usb) {
        OutlinedButton(onClick = actions::scanUsb) { Text(stringResource(R.string.action_scan_usb)) }
        state.usbDevices?.let { list ->
            if (list.isEmpty()) Text(stringResource(R.string.usb_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
            list.forEach { e ->
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text("${e.ids}  ${e.name}", style = MaterialTheme.typography.bodyMedium)
                        Text(e.interfaces, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val kind = when (e.kind) {
                            F75Usb.UsbEntry.Kind.F75_WIRED -> R.string.usb_kind_f75
                            F75Usb.UsbEntry.Kind.AULA_OTHER -> R.string.usb_kind_aula
                            F75Usb.UsbEntry.Kind.AULA_RECEIVER -> R.string.usb_kind_receiver
                            F75Usb.UsbEntry.Kind.OTHER -> null
                        }
                        kind?.let { Text(stringResource(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutCard(state: UiState, actions: AppActions) {
    val source = stringResource(R.string.url_source)
    val upstream = stringResource(R.string.url_upstream)
    SectionCard(stringResource(R.string.about_title), stringResource(R.string.about_version, state.versionName), Icons.Rounded.Info) {
        Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.about_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { actions.openUrl(source) }) {
                Icon(Icons.Rounded.Code, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.about_source))
            }
            OutlinedButton(onClick = { actions.openUrl(upstream) }) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.about_upstream))
            }
        }
    }
}

