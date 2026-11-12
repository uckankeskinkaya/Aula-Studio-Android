package com.aulaf75.android.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aulaf75.android.R

enum class Destination(@StringRes val title: Int, val icon: ImageVector) {
    OVERVIEW(R.string.nav_overview, Icons.Rounded.Dashboard),
    LIGHTING(R.string.nav_lighting, Icons.Rounded.Lightbulb),
    PER_KEY(R.string.nav_per_key, Icons.Rounded.Palette),
    MAPPING(R.string.nav_mapping, Icons.Rounded.Keyboard),
    SETTINGS(R.string.nav_settings, Icons.Rounded.Settings),
}

/** Root of the UI. Stateless: everything comes from [state] and goes out through [actions]. */
@Composable
fun AppScreen(state: UiState, actions: AppActions, initial: Destination = Destination.OVERVIEW) {
    var dest by rememberSaveable { mutableStateOf(initial) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            if (wide) {
                Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        header = { BrandMark(Modifier.padding(vertical = 12.dp)) },
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Destination.entries.forEach { d ->
                            NavigationRailItem(
                                selected = dest == d, onClick = { dest = d },
                                icon = { Icon(d.icon, null) },
                                label = { Text(stringResource(d.title), maxLines = 1) },
                            )
                        }
                    }
                    Content(state, actions, dest, wide = true) { dest = it }
                }
            } else {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    Box(Modifier.weight(1f)) { Content(state, actions, dest, wide = false) { dest = it } }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Destination.entries.forEach { d ->
                            NavigationBarItem(
                                selected = dest == d, onClick = { dest = d },
                                icon = { Icon(d.icon, null) },
                                label = { Text(stringResource(d.title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Keyboard, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun Content(state: UiState, actions: AppActions, dest: Destination, wide: Boolean, navigate: (Destination) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar(state, actions, dest)
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp))
        AnimatedVisibility(state.error != null) { state.error?.let { Banner(text(it), true, actions::dismissMessages) } }
        AnimatedVisibility(state.message != null) { state.message?.let { Banner(text(it), false, actions::dismissMessages) } }
        Box(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier.widthIn(max = 1180.dp).fillMaxWidth().padding(PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val data = state.data
                when {
                    dest == Destination.OVERVIEW -> OverviewScreen(state, actions, wide, navigate)
                    dest == Destination.SETTINGS -> SettingsScreen(state, actions, wide)
                    !state.connected || data == null -> NotConnectedCard(actions, state.busy)
                    dest == Destination.LIGHTING -> LightingScreen(state, actions, data, wide)
                    dest == Destination.PER_KEY -> PerKeyScreen(state, actions, data, wide)
                    dest == Destination.MAPPING -> MappingScreen(state, actions, data, wide)
                }
            }
        }
    }
}

@Composable
private fun TopBar(state: UiState, actions: AppActions, dest: Destination) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(dest.title), style = MaterialTheme.typography.headlineMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(8.dp).background(
                        if (state.connected) Color(0xFF34C759) else MaterialTheme.colorScheme.outline, CircleShape,
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        state.busy && state.busyText != null -> text(state.busyText)
                        state.connected -> stringResource(R.string.status_connected)
                        else -> stringResource(R.string.status_disconnected)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.connected) IconButton(onClick = actions::reread, enabled = !state.busy) {
            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_reread))
        }
    }
}

@Composable
private fun Banner(message: String, error: Boolean, onClose: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
    ) {
        val fg = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (error) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle, null, tint = fg)
            Spacer(Modifier.width(12.dp))
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = fg)
            TextButton(onClick = onClose) { Text(stringResource(R.string.action_ok)) }
        }
    }
}
