@file:OptIn(ExperimentalLayoutApi::class)

package com.aulaf75.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aulaf75.android.KeyboardData
import com.aulaf75.android.R
import com.aulaf75.android.protocol.F75
import com.aulaf75.android.protocol.KeyboardLayout

@Composable
fun OverviewScreen(state: UiState, actions: AppActions, wide: Boolean, navigate: (Destination) -> Unit) {
    val data = state.data
    if (!state.connected || data == null) {
        ConnectHero(actions, state.busy, wide)
        return
    }
    DeviceHero(state, data, actions, wide)
    StatsRow(data, wide)
    if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f)) { QuickBrightness(state, actions, data) }
        Box(Modifier.weight(1f)) { ShortcutsCard(state, actions, navigate) }
    } else {
        QuickBrightness(state, actions, data)
        ShortcutsCard(state, actions, navigate)
    }
    if (!data.mappingAllowed) NoticeCard(stringResource(R.string.notice_mapping_unavailable))
    data.readErrors.forEach { NoticeCard(it) }
}

// ------------------------------------------------------------ disconnected

@Composable
private fun ConnectHero(actions: AppActions, busy: Boolean, wide: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        val steps = @Composable {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.connect_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.connect_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Step(1, Icons.Rounded.Cable, stringResource(R.string.connect_step_wired))
                Step(2, Icons.Rounded.Usb, stringResource(R.string.connect_step_otg))
                Step(3, Icons.Rounded.VerifiedUser, stringResource(R.string.connect_step_permission))
                Button(onClick = actions::connect, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.action_connect))
                }
                Text(stringResource(R.string.connect_hint_restart), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (wide) Row(Modifier.padding(28.dp), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { steps() }
            Box(Modifier.weight(1.2f)) {
                EffectKeyboardPreview(KeyboardLayout.ANSI, 3, Color.White, colorful = true, speed = 1, brightness = 9)
            }
        } else Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            EffectKeyboardPreview(KeyboardLayout.ANSI, 3, Color.White, colorful = true, speed = 1, brightness = 9)
            steps()
        }
    }
}

@Composable
private fun Step(n: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("$n", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelLarge) }
        Spacer(Modifier.width(12.dp))
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun NotConnectedCard(actions: AppActions, busy: Boolean) {
    SectionCard(stringResource(R.string.not_connected_title), stringResource(R.string.not_connected_body), Icons.Rounded.Usb) {
        Button(onClick = actions::connect, enabled = !busy, modifier = Modifier.widthIn(min = 200.dp)) { Text(stringResource(R.string.action_connect)) }
    }
}

// --------------------------------------------------------------- connected

/** Keyboard picture showing the active lighting: per-key colors in custom mode, otherwise the effect preview. */
@Composable
fun LiveKeyboard(data: KeyboardData, modifier: Modifier = Modifier) {
    val layout = data.layout ?: KeyboardLayout.ANSI
    val effect = data.config.effectId
    val perKey = data.perKey
    if (effect == F75.CUSTOM_EFFECT && perKey != null) {
        KeyboardCanvas(layout, modifier) { k, _ -> perKey.get(k.matrixIndex).toColor() }
    } else {
        val s = data.config.storedSettings(effect)
        val color = if (effect in 1..18) data.palette.effectColor(effect).toColor() else Color.White
        EffectKeyboardPreview(layout, effect, color, s.colorful, s.speed, s.brightness, modifier)
    }
}

@Composable
private fun DeviceHero(state: UiState, data: KeyboardData, actions: AppActions, wide: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        val info = @Composable {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("AULA F75", style = MaterialTheme.typography.headlineMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(stringResource(R.string.status_connected), Color(0xFF34C759))
                    Pill(stringResource(R.string.wired_usb), MaterialTheme.colorScheme.secondary)
                    data.layout?.let { Pill(stringResource(if (it == KeyboardLayout.ISO) R.string.layout_iso else R.string.layout_ansi), MaterialTheme.colorScheme.tertiary) }
                }
                state.identity?.let {
                    Text(stringResource(R.string.model_identity, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(stringResource(R.string.hero_body), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (wide) Row(Modifier.padding(28.dp), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(0.8f)) { info() }
            Box(Modifier.weight(1.2f)) { LiveKeyboard(data) }
        } else Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            LiveKeyboard(data)
            info()
        }
    }
}

@Composable
private fun Pill(label: String, color: Color) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.15f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun StatsRow(data: KeyboardData, wide: Boolean) {
    val effect = data.config.effectId
    val lighting = runCatching { data.config.lighting() }.getOrNull()
    val tiles: List<@Composable (Modifier) -> Unit> = listOf(
        { m -> StatTile(Icons.Rounded.Lightbulb, stringResource(R.string.stat_effect), effectName(effect), m) },
        { m ->
            StatTile(
                Icons.Rounded.BrightnessMedium, stringResource(R.string.stat_brightness),
                if (effect in 1..18 && lighting != null) "${lighting.brightness} / 9" else "—", m,
            )
        },
        { m -> StatTile(Icons.Rounded.Bedtime, stringResource(R.string.stat_sleep), data.config.sleepSeconds()?.let { sleepLabel(it) } ?: "—", m) },
        { m ->
            StatTile(
                Icons.Rounded.Keyboard, stringResource(R.string.stat_layout),
                data.layout?.let { stringResource(if (it == KeyboardLayout.ISO) R.string.layout_iso else R.string.layout_ansi) } ?: "—", m,
            )
        },
    )
    if (wide) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { tiles.forEach { it(Modifier.weight(1f)) } }
    else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { pair.forEach { it(Modifier.weight(1f)) } }
        }
    }
}

@Composable
private fun QuickBrightness(state: UiState, actions: AppActions, data: KeyboardData) {
    val effect = data.config.effectId
    SectionCard(stringResource(R.string.quick_brightness), stringResource(R.string.quick_brightness_sub), Icons.Rounded.BrightnessMedium) {
        if (effect in 1..18) {
            val stored = data.config.storedSettings(effect).brightness
            var b by remember(data) { mutableStateOf(stored) }
            LabeledSlider(effectName(effect), b, 9) { b = it }
            Button(onClick = { actions.applyBrightness(b) }, enabled = !state.busy && b != stored, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_apply))
            }
        } else {
            Text(stringResource(R.string.quick_brightness_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ShortcutsCard(state: UiState, actions: AppActions, navigate: (Destination) -> Unit) {
    SectionCard(stringResource(R.string.experimental_title), stringResource(R.string.experimental_sub), Icons.Rounded.Science) {
        ExperimentalSwitchRow(state, actions)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { navigate(Destination.LIGHTING) }) { Text(stringResource(R.string.nav_lighting)) }
            FilledTonalButton(onClick = { navigate(Destination.PER_KEY) }) { Text(stringResource(R.string.nav_per_key)) }
            FilledTonalButton(onClick = { navigate(Destination.MAPPING) }) { Text(stringResource(R.string.nav_mapping)) }
        }
    }
}

@Composable
fun NoticeCard(message: String) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

// ------------------------------------------------------------ experimental

@Composable
fun ExperimentalSwitchRow(state: UiState, actions: AppActions) {
    var confirm by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(if (state.experimental) R.string.state_on else R.string.state_off),
            Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
        )
        androidx.compose.material3.Switch(
            checked = state.experimental,
            onCheckedChange = { if (it) confirm = true else actions.setExperimental(false) },
        )
    }
    if (confirm) ExperimentalDialog(onConfirm = { actions.setExperimental(true); confirm = false }, onDismiss = { confirm = false })
}

@Composable
fun ExperimentalDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Science, null) },
        title = { Text(stringResource(R.string.experimental_dialog_title)) },
        text = { Text(stringResource(R.string.experimental_dialog_body)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_enable)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Shown in place of an experimental editor while the features are disabled. */
@Composable
fun ExperimentalLock(actions: AppActions, feature: String) {
    var confirm by remember { mutableStateOf(false) }
    SectionCard(feature, stringResource(R.string.experimental_locked), Icons.Rounded.Lock) {
        FilledTonalButton(onClick = { confirm = true }) { Text(stringResource(R.string.action_enable_experimental)) }
    }
    if (confirm) ExperimentalDialog(onConfirm = { actions.setExperimental(true); confirm = false }, onDismiss = { confirm = false })
}
