package com.aulaf75.android.ui

import androidx.compose.ui.graphics.Color
import com.aulaf75.android.protocol.F75
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * On-screen approximation of the F75 hardware effects, used for the animated previews.
 * It is illustrative only: the keyboard renders the real effect in firmware.
 */
object EffectRenderer {
    private val OFF = Color(0xFF1E1C24)

    private fun hue(h: Float): Color = Color.hsv(((h % 360f) + 360f) % 360f, 0.9f, 1f)
    private fun frac(v: Float) = v - floor(v)
    private fun hash(x: Float, y: Float): Float = frac(sin(x * 127.1f + y * 311.7f) * 43758.547f)

    private fun scale(c: Color, i: Float): Color {
        val k = i.coerceIn(0f, 1f)
        return Color(
            OFF.red + (c.red - OFF.red) * k,
            OFF.green + (c.green - OFF.green) * k,
            OFF.blue + (c.blue - OFF.blue) * k,
        )
    }

    /**
     * Color of a key centred at normalised ([x], [y]) ∈ [0,1]² at time [t] seconds.
     * [speed] is the hardware speed 0..4, [brightness] 0..9.
     */
    fun colorAt(effect: Int, x: Float, y: Float, t: Float, base: Color, colorful: Boolean, speed: Int = 2, brightness: Int = 9): Color {
        val s = 0.45f + speed * 0.35f
        val rainbow = colorful || !F75.effectSupportsColor(effect)
        val dx = (x - 0.5f) * 2.6f
        val dy = y - 0.5f
        val dist = sqrt(dx * dx + dy * dy)
        val angle = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f
        fun tint(h: Float) = if (rainbow) hue(h) else base

        val c: Color = when (effect) {
            0 -> OFF
            1 -> if (rainbow) hue(x * 300f) else base
            2 -> scale(tint(t * 30f), 0.15f + 0.85f * (0.5f + 0.5f * sin(t * s * 1.6f)))
            3 -> hue(x * 360f - t * 80f * s)
            4 -> {
                val p = frac(t * 0.35f * s) * 1.4f - 0.2f
                scale(tint(p * 360f), 0.08f + exp(-((x - p).pow(2)) / 0.008f))
            }
            5 -> {
                val h = hash(x, y)
                val phase = frac(t * 0.5f * s + h * 3f)
                scale(tint(h * 360f), if (h > 0.55f) max(0f, 1f - phase * 3.5f) else 0.04f)
            }
            6 -> hue(angle + t * 90f * s)
            7 -> {
                val r = frac(t * 0.45f * s) * 1.6f
                scale(tint(r * 200f), 0.15f + exp(-((dist - r).pow(2)) / 0.012f))
            }
            8 -> {
                val h = hash(x, y)
                scale(tint(h * 360f), 0.05f + max(0f, sin((t * 0.6f * s + h * 6f) * PI.toFloat())).pow(8))
            }
            9 -> if (rainbow) hue(t * 35f * s) else scale(base, 0.35f + 0.65f * (0.5f + 0.5f * sin(t * s)))
            10 -> {
                val row = (y * 5f).toInt().coerceIn(0, 5)
                val u = if (row % 2 == 0) x else 1f - x
                val pos = (row + u) / 6f
                val d = frac(frac(t * 0.12f * s) - pos)
                scale(tint(pos * 360f), if (d < 0.1f) 1f - d / 0.1f else 0.04f)
            }
            11 -> scale(if (rainbow) hue(y * 140f + t * 60f * s) else base, 0.55f + 0.45f * sin(y * 9f + t * 2f * s))
            12 -> {
                val h = hash(x, y)
                val phase = frac(t * 0.4f + h * 7f)
                scale(tint(h * 360f), if (h > 0.72f) max(0.04f, 1f - phase * 2.5f) else 0.04f)
            }
            13 -> scale(tint(x * 360f), 0.1f + 0.9f * (0.5f + 0.5f * sin((x * 1.6f - t * 0.45f * s) * 2f * PI.toFloat())))
            14 -> {
                val p = 0.5f + 0.45f * sin(t * 0.9f * s)
                scale(tint(t * 40f), 0.15f + exp(-((x - p).pow(2)) / 0.006f))
            }
            15 -> scale(hue(angle + t * 40f), 0.15f + 0.85f * (0.5f + 0.5f * cos(((angle / 360f) * 4f + t * 0.5f * s) * 2f * PI.toFloat())))
            16 -> hue(y * 220f - t * 110f * s + x * 20f)
            17 -> hue(dist * 300f - t * 110f * s)
            18 -> {
                val spiral = angle + dist * 420f - t * 180f * s
                if (rainbow) hue(spiral) else scale(base, 0.2f + 0.8f * (0.5f + 0.5f * sin(spiral * PI.toFloat() / 90f)))
            }
            else -> base
        }
        return if (effect == 0 || brightness >= 9) c else scale(c, 0.3f + 0.7f * brightness / 9f)
    }
}
