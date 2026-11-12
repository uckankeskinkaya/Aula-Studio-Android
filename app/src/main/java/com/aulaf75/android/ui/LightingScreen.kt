@file:OptIn(ExperimentalLayoutApi::class)

package com.aulaf75.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aulaf75.android.KeyboardData
import com.aulaf75.android.R
import com.aulaf75.android.protocol.F75
import com.aulaf75.android.protocol.KeyboardLayout
import com.aulaf75.android.protocol.LightingSettings
import com.aulaf75.android.protocol.Rgb

@Composable
fun LightingScreen(state: UiState, actions: AppActions, data: KeyboardData, wide: Boolean) {
    if (!state.experimental) {
        BrightnessOnlyCard(state, actions, data)
        ExperimentalLock(actions, stringResource(R.string.lighting_effects_title))
        return
    }
    val current = data.config.effectId
    var effect by remember(data) { mutableStateOf(if (current in F75.EFFECTS) current else 1) }

    if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f)) { EffectGallery(data, effect, current) { effect = it } }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EffectSettings(state, actions, data, effect)
            SleepCard(state, actions, data)
        }
    } else {
        EffectGallery(data, effect, current) { effect = it }
        EffectSettings(state, actions, data, effect)
        SleepCard(state, actions, data)
    }
}

@Composable
private fun BrightnessOnlyCard(state: UiState, actions: AppActions, data: KeyboardData) {
    val effect = data.config.effectId
    SectionCard(stringResource(R.string.quick_brightness), stringResource(R.string.quick_brightness_sub), Icons.Rounded.Tune) {
        if (effect in 1..18) {
            val stored = data.config.storedSettings(effect).brightness
            var b by remember(data) { mutableStateOf(stored) }
            LiveKeyboard(data)
            LabeledSlider(effectName(effect), b, 9) { b = it }
            Button(onClick = { actions.applyBrightness(b) }, enabled = !state.busy && b != stored, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(stringResource(R.string.action_apply))
            }
        } else Text(stringResource(R.string.quick_brightness_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EffectGallery(data: KeyboardData, selected: Int, active: Int, onSelect: (Int) -> Unit) {
    val t by rememberEffectClock()
    SectionCard(stringResource(R.string.lighting_effects_title), stringResource(R.string.lighting_effects_sub), Icons.Rounded.AutoAwesome) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            F75.EFFECTS.forEach { id ->
                val stored = data.config.storedSettings(id)
                val color = if (id in 1..18) data.palette.effectColor(id).let { if (it.isDark) Rgb(255, 255, 255) else it } else Rgb(0, 0, 0)
                EffectCard(id, selected == id, active == id, color, stored.colorful, t) { onSelect(id) }
            }
        }
    }
}

@Composable
private fun EffectCard(id: Int, selected: Boolean, active: Boolean, color: Rgb, colorful: Boolean, t: Float, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh,
        border = if (selected) BorderStroke(2.dp, scheme.primary) else null,
        modifier = Modifier.width(118.dp),
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            EffectThumbnail(id, color.toColor(), colorful, t, Modifier.fillMaxWidth().height(44.dp))
            Text(
                effectName(id), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
            )
            Text(
                if (active) stringResource(R.string.badge_active) else " ",
                style = MaterialTheme.typography.labelSmall, color = scheme.primary,
            )
        }
    }
}

@Composable
private fun EffectSettings(state: UiState, actions: AppActions, data: KeyboardData, effect: Int) {
    val stored = data.config.storedSettings(effect)
    val storedColor = if (effect in 1..18) data.palette.effectColor(effect) else Rgb(255, 255, 255)
    var brightness by remember(data, effect) { mutableStateOf(if (effect in 1..18 && stored.brightness == 0) 6 else stored.brightness) }
    var speed by remember(data, effect) { mutableStateOf(stored.speed) }
    var colorful by remember(data, effect) { mutableStateOf(stored.colorful) }
    // A black stored color makes an effect look broken; start from white instead.
    var color by remember(data, effect) { mutableStateOf(if (storedColor.isDark) Rgb(255, 255, 255) else storedColor) }
    val layout = data.layout ?: KeyboardLayout.ANSI

    SectionCard(effectName(effect), effectDescription(effect), Icons.Rounded.Tune) {
        EffectKeyboardPreview(layout, effect, color.toColor(), colorful, speed, brightness)
        Text(stringResource(R.string.preview_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (effect != 0) {
            LabeledSlider(stringResource(R.string.label_brightness), brightness, 9) { brightness = it }
            if (F75.effectSupportsSpeed(effect)) LabeledSlider(stringResource(R.string.label_speed), speed, 4) { speed = it }
            if (F75.effectSupportsColor(effect)) Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_colorful), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.label_colorful_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = colorful, onCheckedChange = { colorful = it })
            } else Text(stringResource(R.string.effect_fixed_colors), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    val needsColor = effect != 0 && F75.effectSupportsColor(effect) && !colorful
    if (needsColor) SectionCard(stringResource(R.string.label_color), stringResource(R.string.label_color_sub), Icons.Rounded.ColorLens) {
        if (storedColor.isDark) Text(stringResource(R.string.color_was_black), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ColorPicker(color, { color = it }) }
    }

    val result = runCatching {
        val cfg = data.config.withLighting(LightingSettings(effect, brightness, speed, colorful))
        val pal = if (needsColor && color != data.palette.effectColor(effect)) data.palette.withEffectColor(effect, color) else data.palette
        cfg to pal
    }
    Button(
        onClick = { result.getOrNull()?.let { (c, p) -> actions.applyLighting(c, p) } },
        enabled = !state.busy && result.isSuccess && !(needsColor && color.isDark),
        modifier = Modifier.fillMaxWidth().height(54.dp),
    ) { Text(stringResource(R.string.action_apply_to_keyboard)) }
}

@Composable
private fun SleepCard(state: UiState, actions: AppActions, data: KeyboardData) {
    val cur = data.config.sleepSeconds()
    var sel by remember(data) { mutableStateOf(cur ?: 300) }
    SectionCard(stringResource(R.string.sleep_title), stringResource(R.string.sleep_sub), Icons.Rounded.Bedtime) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            F75.SLEEP_SECONDS.forEach { s -> FilterChip(selected = sel == s, onClick = { sel = s }, label = { Text(sleepLabel(s)) }) }
        }
        FilledTonalButton(onClick = { actions.applySleep(sel) }, enabled = !state.busy && sel != cur) { Text(stringResource(R.string.action_apply)) }
    }
}
