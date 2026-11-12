@file:OptIn(ExperimentalLayoutApi::class)

package com.aulaf75.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aulaf75.android.KeyboardData
import com.aulaf75.android.R
import com.aulaf75.android.protocol.F75
import com.aulaf75.android.protocol.Rgb

@Composable
fun PerKeyScreen(state: UiState, actions: AppActions, data: KeyboardData, wide: Boolean) {
    if (!state.experimental) { ExperimentalLock(actions, stringResource(R.string.perkey_title)); return }
    val perKey = data.perKey
    val layout = data.layout
    if (perKey == null || layout == null || !data.mappingAllowed) {
        NoticeCard(stringResource(R.string.notice_mapping_unavailable))
        return
    }
    val pending = remember(data) { mutableStateMapOf<Int, Rgb>() }
    val selected = remember(data) { mutableStateMapOf<Int, Boolean>() }
    var color by remember { mutableStateOf(Rgb(124, 92, 255)) }
    val turkish = isTurkish() && layout == com.aulaf75.android.protocol.KeyboardLayout.ISO

    SectionCard(stringResource(R.string.perkey_title), stringResource(R.string.perkey_sub), Icons.Rounded.Palette) {
        KeyboardEditor(
            layout = layout,
            fill = { k -> (pending[k.matrixIndex] ?: perKey.get(k.matrixIndex)).toColor() },
            label = { it.legend(turkish) },
            selected = { selected[it.matrixIndex] == true },
            enabled = { true },
            onKey = { k -> if (selected[k.matrixIndex] == true) selected.remove(k.matrixIndex) else selected[k.matrixIndex] = true },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { layout.keys.forEach { selected[it.matrixIndex] = true } },
                label = { Text(stringResource(R.string.action_select_all)) },
                leadingIcon = { Icon(Icons.Rounded.SelectAll, null, Modifier.size(AssistChipDefaults.IconSize)) },
            )
            AssistChip(
                onClick = { selected.clear() },
                label = { Text(stringResource(R.string.action_clear_selection)) },
                leadingIcon = { Icon(Icons.Rounded.Deselect, null, Modifier.size(AssistChipDefaults.IconSize)) },
            )
            AssistChip(
                onClick = { pending.clear() }, enabled = pending.isNotEmpty(),
                label = { Text(stringResource(R.string.action_undo_paint)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Undo, null, Modifier.size(AssistChipDefaults.IconSize)) },
            )
        }
        Text(
            stringResource(R.string.perkey_counts, selected.size, pending.size),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val picker = @Composable {
        SectionCard(stringResource(R.string.label_color), stringResource(R.string.label_color_sub), Icons.Rounded.ColorLens) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ColorPicker(color, { color = it }) }
        }
    }
    val apply = @Composable {
        SectionCard(stringResource(R.string.perkey_apply_title), stringResource(R.string.perkey_apply_sub), Icons.Rounded.FormatPaint) {
            FilledTonalButton(
                onClick = { selected.keys.forEach { pending[it] = color } },
                enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_paint_selected)) }
            Button(
                onClick = { actions.applyPerKey(pending.toMap()) },
                enabled = !state.busy && (pending.isNotEmpty() || data.config.effectId != F75.CUSTOM_EFFECT),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(
                    if (pending.isEmpty()) stringResource(R.string.action_enable_custom)
                    else stringResource(R.string.action_apply_keys, pending.size)
                )
            }
        }
    }
    if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f)) { picker() }
        Box(Modifier.weight(1f)) { apply() }
    } else {
        picker()
        apply()
    }
}
