package com.aulaf75.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aulaf75.android.protocol.F75Layout
import com.aulaf75.android.protocol.PhysicalKey
import com.aulaf75.android.protocol.Rgb

fun Rgb.toColor() = Color(r, g, b)

/** Basit F75 ANSI klavye çizimi; yatay kaydırılabilir. */
@Composable
fun KeyboardView(
    fill: (PhysicalKey) -> Color,
    label: (PhysicalKey) -> String,
    selected: (PhysicalKey) -> Boolean,
    enabled: (PhysicalKey) -> Boolean,
    onKey: (PhysicalKey) -> Unit,
) {
    val unit = 40.dp
    Box(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(4.dp)
    ) {
        Box(Modifier.size(unit * F75Layout.WIDTH, unit * F75Layout.HEIGHT)) {
            for (k in F75Layout.KEYS) {
                val bg = fill(k)
                val lum = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
                val on = enabled(k)
                Box(
                    Modifier
                        .offset(unit * k.x, unit * k.y)
                        .size(unit * k.w - 3.dp, unit - 3.dp)
                        .background(if (on) bg else bg.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .border(
                            if (selected(k)) 3.dp else 1.dp,
                            if (selected(k)) MaterialTheme.colorScheme.primary else Color.Gray,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable(enabled = on) { onKey(k) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(k),
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        textAlign = TextAlign.Center,
                        color = if (lum > 0.55f) Color.Black else Color.White,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

private val PRESETS = listOf(
    Rgb(255, 0, 0), Rgb(255, 128, 0), Rgb(255, 255, 0), Rgb(0, 255, 0), Rgb(0, 255, 255),
    Rgb(0, 0, 255), Rgb(128, 0, 255), Rgb(255, 0, 255), Rgb(255, 255, 255), Rgb(0, 0, 0),
)

@Composable
fun ColorPicker(color: Rgb, onChange: (Rgb) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(color.toColor(), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
            var text by remember(color) { mutableStateOf(color.toHex()) }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; Rgb.fromHex(it)?.let(onChange) },
                label = { Text("Hex") },
                singleLine = true,
                modifier = Modifier.width(140.dp),
            )
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PRESETS.forEach { p ->
                Box(
                    Modifier
                        .size(32.dp)
                        .background(p.toColor(), CircleShape)
                        .border(if (p == color) 3.dp else 1.dp, if (p == color) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
                        .clickable { onChange(p) }
                )
            }
        }
        ChannelSlider("R", color.r) { onChange(color.copy(r = it)) }
        ChannelSlider("G", color.g) { onChange(color.copy(g = it)) }
        ChannelSlider("B", color.b) { onChange(color.copy(b = it)) }
    }
}

@Composable
private fun ChannelSlider(name: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$name ${value.toString().padStart(3)}", fontWeight = FontWeight.Medium, modifier = Modifier.width(56.dp))
        Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt().coerceIn(0, 255)) }, valueRange = 0f..255f)
    }
}
