package com.aulaf75.android.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aulaf75.android.R
import com.aulaf75.android.protocol.F75Geometry
import com.aulaf75.android.protocol.KeyboardLayout
import com.aulaf75.android.protocol.PhysicalKey
import com.aulaf75.android.protocol.Rgb
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

fun Rgb.toColor() = Color(r, g, b)

// ------------------------------------------------------------------ layout

@Composable
fun SectionCard(
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (title != null) Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    IconBadge(icon)
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (subtitle != null) Text(
                        subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                trailing?.invoke(this)
            }
            content()
        }
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary, size: Dp = 38.dp) {
    Box(
        Modifier.size(size).background(tint.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f)) }
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, style = CaptionStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun StatTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, size = 34.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Label + value pill + slider. */
@Composable
fun LabeledSlider(label: String, value: Int, max: Int, enabled: Boolean = true, onChange: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "$value / $max", Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Slider(
            value = value.toFloat(), onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..max.toFloat(), steps = max - 1, enabled = enabled,
        )
    }
}

// --------------------------------------------------------------- animation

/** Seconds since the screen appeared; drives the illustrative effect previews. */
@Composable
fun rememberEffectClock(): State<Float> = rememberInfiniteTransition(label = "fx").animateFloat(
    initialValue = 0f, targetValue = 3600f,
    animationSpec = infiniteRepeatable(tween(3_600_000, easing = LinearEasing)), label = "t",
)

// ---------------------------------------------------------------- keyboard

private fun keyCenter(k: PhysicalKey) =
    Offset((k.x + k.w / 2f) / F75Geometry.WIDTH, (k.y + k.h / 2f) / F75Geometry.HEIGHT)

/** Non-interactive keyboard drawn on a canvas; [colorOf] receives the key and its normalised centre. */
@Composable
fun KeyboardCanvas(
    layout: KeyboardLayout,
    modifier: Modifier = Modifier,
    colorOf: (PhysicalKey, Offset) -> Color,
) {
    val frame = MaterialTheme.colorScheme.surfaceContainerLowest
    Canvas(modifier.fillMaxWidth().aspectRatio((F75Geometry.WIDTH + 0.5f) / (F75Geometry.HEIGHT + 0.5f))) {
        val unit = size.width / (F75Geometry.WIDTH + 0.5f)
        val pad = unit * 0.25f
        drawRoundRect(frame, cornerRadius = CornerRadius(unit * 0.4f))
        val gap = unit * 0.1f
        for (k in layout.keys) {
            val c = colorOf(k, keyCenter(k))
            val tl = Offset(pad + k.x * unit + gap / 2, pad + k.y * unit + gap / 2)
            val sz = Size(k.w * unit - gap, k.h * unit - gap)
            // soft glow under the cap, then the cap itself
            drawRoundRect(c.copy(alpha = 0.35f), tl - Offset(gap / 2, gap / 2), Size(sz.width + gap, sz.height + gap), CornerRadius(unit * 0.22f))
            drawRoundRect(Color(0xFF2B2833), tl, sz, CornerRadius(unit * 0.16f))
            drawRoundRect(c, tl + Offset(unit * 0.06f, unit * 0.06f), Size(sz.width - unit * 0.12f, sz.height - unit * 0.12f), CornerRadius(unit * 0.12f), alpha = 0.9f)
        }
    }
}

/** Animated preview of a hardware effect on the full keyboard. */
@Composable
fun EffectKeyboardPreview(layout: KeyboardLayout, effect: Int, color: Color, colorful: Boolean, speed: Int, brightness: Int, modifier: Modifier = Modifier) {
    val t by rememberEffectClock()
    KeyboardCanvas(layout, modifier) { _, p -> EffectRenderer.colorAt(effect, p.x, p.y, t, color, colorful, speed, brightness) }
}

/** Small animated swatch for the effect gallery. */
@Composable
fun EffectThumbnail(effect: Int, color: Color, colorful: Boolean, t: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cols = 10
        val rows = 4
        val cw = size.width / cols
        val ch = size.height / rows
        for (r in 0 until rows) for (c in 0 until cols) {
            val col = EffectRenderer.colorAt(effect, (c + 0.5f) / cols, (r + 0.5f) / rows, t, color, colorful)
            drawRoundRect(col, Offset(c * cw + 1.5f, r * ch + 1.5f), Size(cw - 3f, ch - 3f), CornerRadius(3f))
        }
    }
}

/**
 * Interactive keyboard. Keys scale to the available width (no scrolling on tablets); on narrow
 * phones the keyboard keeps a usable minimum key size and scrolls horizontally.
 */
@Composable
fun KeyboardEditor(
    layout: KeyboardLayout,
    fill: (PhysicalKey) -> Color,
    label: @Composable (PhysicalKey) -> String,
    selected: (PhysicalKey) -> Boolean,
    enabled: (PhysicalKey) -> Boolean,
    onKey: (PhysicalKey) -> Unit,
) {
    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
            .padding(10.dp)
    ) {
        val unit = (maxWidth / F75Geometry.WIDTH).coerceIn(34.dp, 60.dp)
        val scroll = rememberScrollState()
        Box(Modifier.horizontalScroll(scroll)) {
            Box(Modifier.size(unit * F75Geometry.WIDTH, unit * F75Geometry.HEIGHT)) {
                for (k in layout.keys) {
                    val bg = fill(k)
                    val lum = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
                    val on = enabled(k)
                    val sel = selected(k)
                    val shape = RoundedCornerShape(unit * 0.2f)
                    Box(
                        Modifier
                            .offset(unit * k.x, unit * k.y)
                            .size(unit * k.w - 4.dp, unit * k.h - 4.dp)
                            .background(if (on) bg else bg.copy(alpha = 0.35f), shape)
                            .border(
                                if (sel) 3.dp else 1.dp,
                                if (sel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.10f),
                                shape,
                            )
                            .clickable(enabled = on) { onKey(k) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label(k),
                            fontSize = (unit.value * 0.24f).coerceIn(9f, 13f).sp,
                            lineHeight = (unit.value * 0.27f).coerceIn(10f, 14f).sp,
                            textAlign = TextAlign.Center,
                            color = if (!on) Color.Gray else if (lum > 0.55f) Color(0xFF1B1A20) else Color.White,
                            maxLines = 2,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------- color wheel

private fun rgbToHsv(c: Rgb): FloatArray = FloatArray(3).also { android.graphics.Color.RGBToHSV(c.r, c.g, c.b, it) }

private fun hsvToRgb(h: Float, s: Float, v: Float): Rgb {
    val i = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
    return Rgb(android.graphics.Color.red(i), android.graphics.Color.green(i), android.graphics.Color.blue(i))
}

private val HUES = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)

/** Hue/saturation wheel: angle = hue, distance from the centre = saturation. Tap or drag. */
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
        drawCircle(Color.Black.copy(alpha = (1f - value).coerceIn(0f, 0.85f)), r, center)
        val rad = Math.toRadians(hue.toDouble())
        val p = Offset(center.x + (cos(rad) * sat * r).toFloat(), center.y + (sin(rad) * sat * r).toFloat())
        drawCircle(hsvToRgb(hue, sat, value).toColor(), 13.dp.toPx(), p)
        drawCircle(Color.White, 13.dp.toPx(), p, style = Stroke(3.dp.toPx()))
        drawCircle(Color.Black.copy(alpha = 0.3f), 15.dp.toPx(), p, style = Stroke(1.dp.toPx()))
    }
}

private val PRESETS = listOf(
    Rgb(255, 255, 255), Rgb(255, 59, 48), Rgb(255, 149, 0), Rgb(255, 214, 10), Rgb(52, 199, 89),
    Rgb(0, 199, 190), Rgb(50, 173, 230), Rgb(0, 122, 255), Rgb(124, 92, 255), Rgb(191, 90, 242), Rgb(255, 55, 160),
)

@Composable
fun ColorPicker(color: Rgb, onChange: (Rgb) -> Unit, wheelSize: Dp = 230.dp) {
    var hsv by remember { mutableStateOf(rgbToHsv(color)) }
    LaunchedEffect(color) { if (hsvToRgb(hsv[0], hsv[1], hsv[2]) != color) hsv = rgbToHsv(color) }
    fun emit(h: Float, s: Float, v: Float) {
        hsv = floatArrayOf(h, s, v)
        onChange(hsvToRgb(h, s, v))
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ColorWheel(
            hue = hsv[0], sat = hsv[1], value = hsv[2],
            onChange = { h, s -> emit(h, s, if (hsv[2] < 0.15f) 1f else hsv[2]) },
            modifier = Modifier.size(wheelSize),
        )
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.color_intensity), style = MaterialTheme.typography.bodyMedium)
            Slider(value = hsv[2], onValueChange = { emit(hsv[0], hsv[1], it) }, valueRange = 0f..1f)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESETS.forEach { p ->
                Box(
                    Modifier.size(32.dp).background(p.toColor(), CircleShape)
                        .border(
                            if (p == color) 3.dp else 1.dp,
                            if (p == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        )
                        .clickable { onChange(p) }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(52.dp).background(color.toColor(), RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(12.dp))
            var text by remember(color) { mutableStateOf(color.toHex()) }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; Rgb.fromHex(it)?.let(onChange) },
                label = { Text(stringResource(R.string.color_hex)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.widthIn(max = 180.dp),
            )
        }
        if (color.isDark) Text(
            stringResource(R.string.color_too_dark), color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(2.dp))
    }
}
