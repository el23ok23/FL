package com.fletaba.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private class Particle(
    val x0: Float, val vx: Float, val vy: Float, val size: Float, val color: Color,
    val spin: Float, val phase: Float,
)

/** Lluvia corta de papelitos (unos 2,5 s), dibujada en Canvas a 60 fps. */
@Composable
fun Confetti(modifier: Modifier = Modifier, durationMs: Int = 2600) {
    val colors = MaterialTheme.colorScheme
    val palette = remember { listOf(colors.primary, colors.tertiary, colors.primaryContainer, colors.tertiaryContainer, Color(0xFF22C55E)) }
    val particles = remember {
        val rnd = Random(42)
        List(70) {
            Particle(
                x0 = rnd.nextFloat(),
                vx = (rnd.nextFloat() - 0.5f) * 0.25f,
                vy = 0.35f + rnd.nextFloat() * 0.45f,
                size = 6f + rnd.nextFloat() * 8f,
                color = palette[it % palette.size],
                spin = (rnd.nextFloat() - 0.5f) * 720f,
                phase = rnd.nextFloat() * 6.28f,
            )
        }
    }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (t < 1f) {
            withFrameNanos { now -> t = ((now - start) / 1_000_000f / durationMs).coerceAtMost(1f) }
        }
    }
    if (t >= 1f) return
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val y = (-0.1f + p.vy * t * 1.6f + 0.5f * t * t) * h
            if (y > h) return@forEach
            val x = (p.x0 + p.vx * t + 0.03f * kotlin.math.sin(p.phase + t * 9f)) * w
            val alpha = if (t > 0.75f) (1f - (t - 0.75f) / 0.25f) else 1f
            rotate(p.spin * t, pivot = Offset(x, y)) {
                drawRect(p.color.copy(alpha = alpha), Offset(x - p.size / 2, y - p.size / 3), Size(p.size, p.size * 0.6f))
            }
        }
    }
}
