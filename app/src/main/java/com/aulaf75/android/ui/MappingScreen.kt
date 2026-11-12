@file:OptIn(ExperimentalMaterial3Api::class)

package com.aulaf75.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.aulaf75.android.protocol.KeyAction
import com.aulaf75.android.protocol.KeyNames
import com.aulaf75.android.protocol.KeyboardLayout
import com.aulaf75.android.protocol.Keymap
import com.aulaf75.android.protocol.Layer

@Composable
fun MappingScreen(state: UiState, actions: AppActions, data: KeyboardData, wide: Boolean) {
    if (!state.experimental) { ExperimentalLock(actions, stringResource(R.string.mapping_title)); return }
    val layout = data.layout
    if (layout == null || !data.mappingAllowed) { NoticeCard(stringResource(R.string.notice_mapping_unavailable)); return }
    var layer by remember { mutableStateOf(Layer.BASE) }
    val map = data.keymap(layer)
    val pending = remember(data, layer) { mutableStateMapOf<Int, KeyAction>() }
    var editing by remember { mutableStateOf<Int?>(null) }
    val scheme = MaterialTheme.colorScheme

    SectionCard(stringResource(R.string.mapping_title), stringResource(R.string.mapping_sub), Icons.Rounded.Layers) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(layer == Layer.BASE, { layer = Layer.BASE }, SegmentedButtonDefaults.itemShape(0, 2)) {
                Text(stringResource(R.string.layer_base))
            }
            SegmentedButton(layer == Layer.FN, { layer = Layer.FN }, SegmentedButtonDefaults.itemShape(1, 2)) {
                Text(stringResource(R.string.layer_fn))
            }
        }
        Text(
            stringResource(if (layer == Layer.BASE) R.string.layer_base_help else R.string.layer_fn_help),
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
        )
        if (map == null) {
            NoticeCard(stringResource(R.string.err_bad_reply))
            return@SectionCard
        }
        val turkish = isTurkish() && layout == KeyboardLayout.ISO
        KeyboardEditor(
            layout = layout,
            fill = { k ->
                when {
                    pending.containsKey(k.matrixIndex) -> scheme.tertiary
                    isRemapped(map, k) -> scheme.primaryContainer
                    else -> scheme.surfaceContainerHighest
                }
            },
            label = { k ->
                pending[k.matrixIndex]?.let { actionShort(it) }
                    ?: if (!isRemapped(map, k) && k.usage != null) k.legend(turkish).ifBlank { "Space" }
                    else recordShort(map.raw(k.matrixIndex))
            },
            selected = { editing == it.matrixIndex },
            enabled = { !it.isProtected },
            onKey = { editing = it.matrixIndex },
        )
    }

    if (map != null && pending.isNotEmpty()) SectionCard(
        stringResource(R.string.mapping_pending_title, pending.size), stringResource(R.string.mapping_pending_sub), Icons.Rounded.Keyboard,
    ) {
        pending.forEach { (i, a) -> PendingRow(layout, map, i, a) { pending.remove(i) } }
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pending.clear() }) { Text(stringResource(R.string.action_discard)) }
            Button(onClick = { actions.applyRemap(layer, pending.toMap()) }, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_apply_keys, pending.size))
            }
        }
    }

    val idx = editing
    if (idx != null && map != null) {
        val key = layout.keyAt(idx)
        val turkish = isTurkish() && layout == KeyboardLayout.ISO
        ActionSheet(
            title = stringResource(R.string.mapping_sheet_title, key?.legend(turkish) ?: "#$idx"),
            current = recordName(map.raw(idx)),
            original = key?.usage?.let { KeyAction(0, it) },
            onPick = { a ->
                if (a == null || map.matches(idx, a)) pending.remove(idx) else pending[idx] = a
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

/** True when the key no longer sends its own key code (shown highlighted). Fn and vendor actions count as remapped. */
private fun isRemapped(map: Keymap, k: com.aulaf75.android.protocol.PhysicalKey): Boolean {
    val usage = k.usage ?: return false
    return runCatching { !map.matches(k.matrixIndex, KeyAction(0, usage)) }.getOrDefault(true)
}

@Composable
private fun actionShort(a: KeyAction): String = actionName(a).let { if (it.length > 8) it.take(7) + "…" else it }

@Composable
private fun PendingRow(layout: KeyboardLayout, map: Keymap, index: Int, action: KeyAction, onRemove: () -> Unit) {
    val turkish = isTurkish() && layout == KeyboardLayout.ISO
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(layout.keyAt(index)?.legend(turkish)?.ifBlank { "Space" } ?: "#$index", style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(72.dp))
        Text(recordName(map.raw(index)), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(actionName(action), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, stringResource(R.string.action_remove)) }
    }
}

@Composable
private fun ActionSheet(title: String, current: String, original: KeyAction?, onPick: (KeyAction?) -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val tabs = listOf(
        Triple(R.string.category_media, Icons.Rounded.MusicNote, KeyNames.MEDIA_USAGES.map { KeyAction(2, it) }),
        Triple(R.string.category_keys, Icons.Rounded.Keyboard, KeyNames.ASSIGNABLE_KEYBOARD.map { KeyAction(0, it) }),
        Triple(R.string.category_mouse, Icons.Rounded.Mouse, KeyNames.MOUSE_BUTTONS.map { KeyAction(1, it) }),
    )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.mapping_current, current), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (original != null) OutlinedButton(onClick = { onPick(original) }) { Text(stringResource(R.string.action_reset_key)) }
                TextButton(onClick = { onPick(null) }) { Text(stringResource(R.string.action_undo_change)) }
            }
            PrimaryTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, (label, icon, _) ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(label)) }, icon = { Icon(icon, null) })
                }
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
            )
            val options = tabs[tab].third.map { it to actionName(it) }.filter { it.second.contains(query, ignoreCase = true) }
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(options) { (action, name) ->
                    Text(
                        name,
                        Modifier.fillMaxWidth().clickable { onPick(action) }.padding(vertical = 14.dp, horizontal = 4.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
