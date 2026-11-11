package com.aulaf75.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aulaf75.android.protocol.F75Layout
import com.aulaf75.android.protocol.KeyboardLayout
import com.aulaf75.android.protocol.PhysicalKey
import com.aulaf75.android.protocol.Rgb
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

fun Rgb.toColor() = Color(r, g, b)
fun Rgb.isDark() = maxOf(r, g, b) < 24

// ----------------------------------------------------------------- kartlar

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
}

/** Başlık + sağda değer hapı olan kaydırıcı. */
@Composable
fun LabeledSlider(label: String, value: Int, max: Int, onChange: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "$value / $max",
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = 0f..max.toFloat(), steps = max - 1)
    }
}

// --------------------------------------------------------------- klavye

/** F75 klavye çizimi (ANSI veya Türkçe ISO); yatay kaydırılabilir. */
@Composable
fun KeyboardView(
    layout: KeyboardLayout,
    fill: (PhysicalKey) -> Color,
    label: (PhysicalKey) -> String,
    selected: (PhysicalKey) -> Boolean,
    enabled: (PhysicalKey) -> Boolean,
    onKey: (PhysicalKey) -> Unit,
) {
    val unit = 42.dp
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Box(Modifier.size(unit * F75Layout.WIDTH, unit * F75Layout.HEIGHT)) {
                for (k in layout.keys) {
                    val bg = fill(k)
                    val lum = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
                    val on = enabled(k)
                    val sel = selected(k)
                    val shape = RoundedCornerShape(9.dp)
                    Box(
                        Modifier
                            .offset(unit * k.x, unit * k.y)
                            .size(unit * k.w - 4.dp, unit * k.h - 4.dp)
                            .background(if (on) bg else bg.copy(alpha = 0.3f), shape)
                            .border(
                                if (sel) 3.dp else 1.dp,
                                if (sel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                shape,
                            )
                            .clickable(enabled = on) { onKey(k) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label(k),
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            textAlign = TextAlign.Center,
                            color = if (lum > 0.55f) Color(0xFF1C1A22) else Color.White,
                            maxLines = 2,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ renk çarkı

private fun rgbToHsv(c: Rgb): FloatArray =
    FloatArray(3).also { android.graphics.Color.RGBToHSV(c.r, c.g, c.b, it) }

private fun hsvToRgb(h: Float, s: Float, v: Float): Rgb {
    val i = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
    return Rgb(android.graphics.Color.red(i), android.graphics.Color.green(i), android.graphics.Color.blue(i))
}

private val HUES = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)

/** Ton/doygunluk çarkı: açı = ton, merkezden uzaklık = doygunluk. */
@Composable
fun ColorWheel(hue: Float, sat: Float, value: Float, onChange: (Float, Float) -> Unit, modifier: Modifier = Modifier) {
    val cb by rememberUpdatedState(onChange)
    fun pick(pos: Offset, w: Int, h: Int) {
        val cx = w / 2f
        val cy = h / 2f
        val r = min(cx, cy)
        val dx = pos.x - cx
        val dy = pos.y - cy
        val ang = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360.0) % 360.0).toFloat()
        cb(ang, (hypot(dx, dy) / r).coerceIn(0f, 1f))
    }
    Canvas(
        modifier
            .pointerInput(Unit) { detectTapGestures { pick(it, size.width, size.height) } }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { pick(it, size.width, size.height) }) { change, _ ->
                    change.consume()
                    pick(change.position, size.width, size.height)
                }
            }
    ) {
        val r = min(size.width, size.height) / 2f
        drawCircle(Brush.sweepGradient(HUES, center), r, center)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), center, r), r, center)
        // Seçilen parlaklığı çarka yansıt
        drawCircle(Color.Black.copy(alpha = (1f - value).coerceIn(0f, 0.85f)), r, center)
        val rad = Math.toRadians(hue.toDouble())
        val p = Offset(center.x + (cos(rad) * sat * r).toFloat(), center.y + (sin(rad) * sat * r).toFloat())
        drawCircle(hsvToRgb(hue, sat, value).toColor(), 14.dp.toPx(), p)
        drawCircle(Color.White, 14.dp.toPx(), p, style = Stroke(3.dp.toPx()))
        drawCircle(Color.Black.copy(alpha = 0.35f), 16.dp.toPx(), p, style = Stroke(1.dp.toPx()))
    }
}

private val PRESETS = listOf(
    Rgb(255, 255, 255), Rgb(255, 59, 48), Rgb(255, 149, 0), Rgb(255, 214, 10), Rgb(52, 199, 89),
    Rgb(0, 199, 190), Rgb(50, 173, 230), Rgb(0, 122, 255), Rgb(124, 92, 255), Rgb(191, 90, 242), Rgb(255, 55, 160),
)

@Composable
fun ColorPicker(color: Rgb, onChange: (Rgb) -> Unit) {
    var hsv by remember { mutableStateOf(rgbToHsv(color)) }
    LaunchedEffect(color) {
        if (hsvToRgb(hsv[0], hsv[1], hsv[2]) != color) hsv = rgbToHsv(color)
    }
    fun emit(h: Float, s: Float, v: Float) {
        hsv = floatArrayOf(h, s, v)
        onChange(hsvToRgb(h, s, v))
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ColorWheel(
            hue = hsv[0], sat = hsv[1], value = hsv[2],
            onChange = { h, s -> emit(h, s, if (hsv[2] < 0.15f) 1f else hsv[2]) },
            modifier = Modifier.size(250.dp),
        )
        Column(Modifier.fillMaxWidth()) {
            Text("Parlaklık (renk)", style = MaterialTheme.typography.bodyMedium)
            Slider(value = hsv[2], onValueChange = { emit(hsv[0], hsv[1], it) }, valueRange = 0f..1f)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESETS.forEach { p ->
                Box(
                    Modifier
                        .size(34.dp)
                        .background(p.toColor(), CircleShape)
                        .border(
                            if (p == color) 3.dp else 1.dp,
                            if (p == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            CircleShape,
                        )
                        .clickable { onChange(p) }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(color.toColor(), RoundedCornerShape(14.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(12.dp))
            var text by remember(color) { mutableStateOf(color.toHex()) }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; Rgb.fromHex(it)?.let(onChange) },
                label = { Text("Hex kodu") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.width(160.dp),
            )
        }
        if (color.isDark()) {
            Text(
                "Bu renk çok koyu: klavyede ışık neredeyse görünmez.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}
