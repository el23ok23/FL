package com.fletarg.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fletarg.core.Argentina
import com.fletarg.core.LatLng
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Mapa esquemático en Canvas: encuadra automáticamente todas las paradas, dibuja el recorrido
 * (que se "traza" animado al aparecer) y marca cada parada con su letra. Sin SDK de mapas.
 */
@Composable
fun MiniMap(
    points: List<LatLng?>,
    insideFlags: List<Boolean>,
    path: List<LatLng>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val known = points.filterNotNull()
    val pathKey = "${path.size}-${known.size}-${known.lastOrNull()}"
    val progress = remember { Animatable(0f) }
    LaunchedEffect(pathKey) {
        if (known.size >= 2) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 900))
        } else {
            progress.snapTo(0f)
        }
    }
    val measure = remember { PathMeasure() }
    val segment = remember { Path() }

    Canvas(modifier) {
        // Encuadre: caja que contiene las paradas (o el centro del país si no hay ninguna).
        val lats = known.map { it.lat }
        val lngs = known.map { it.lng }
        val cLat = if (known.isEmpty()) Argentina.center.lat else (lats.min() + lats.max()) / 2
        val cLng = if (known.isEmpty()) Argentina.center.lng else (lngs.min() + lngs.max()) / 2
        val cosLat = cos(Math.toRadians(cLat)).toFloat().coerceAtLeast(0.2f)
        var spanLat = if (known.size >= 2) (lats.max() - lats.min()).toFloat() else 0f
        var spanLng = if (known.size >= 2) ((lngs.max() - lngs.min()) * cosLat).toFloat() else 0f
        val minSpan = 0.02f
        spanLat = max(spanLat, minSpan)
        spanLng = max(spanLng, minSpan)
        val padPx = 34.dp.toPx()
        val scale = min((size.width - 2 * padPx) / spanLng, (size.height - 2 * padPx) / spanLat)

        fun project(p: LatLng): Offset {
            val x = size.width / 2 + ((p.lng - cLng).toFloat() * cosLat) * scale
            val y = size.height / 2 - (p.lat - cLat).toFloat() * scale
            return Offset(x, y)
        }

        // Cuadrícula suave de fondo
        val grid = colors.onSurface.copy(alpha = 0.05f)
        val step = 28.dp.toPx()
        var gx = 0f
        while (gx < size.width) { drawLine(grid, Offset(gx, 0f), Offset(gx, size.height), 1f); gx += step }
        var gy = 0f
        while (gy < size.height) { drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1f); gy += step }

        val projected = points.map { it?.let(::project) }
        val resolved = projected.filterNotNull()

        if (resolved.size >= 2) {
            val full = Path()
            val source = if (path.size >= 2) path.map(::project) else resolved
            source.forEachIndexed { i, pt -> if (i == 0) full.moveTo(pt.x, pt.y) else full.lineTo(pt.x, pt.y) }
            measure.setPath(full, false)
            segment.reset()
            measure.getSegment(0f, measure.length * progress.value, segment, true)
            val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(segment, colors.primary.copy(alpha = 0.22f), style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (path.size >= 2) {
                drawPath(segment, colors.primary, style = stroke)
            } else {
                drawPath(
                    segment, colors.primary,
                    style = Stroke(
                        width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx())),
                    ),
                )
            }
        }

        projected.forEachIndexed { i, pt ->
            if (pt == null) return@forEachIndexed
            val inside = insideFlags.getOrElse(i) { true }
            val color = when {
                !inside -> colors.error
                i == 0 -> colors.primary
                i == points.lastIndex -> colors.tertiary
                else -> colors.secondary
            }
            marker(pt, ('A' + i).toString(), color, measurer, labelStyle)
        }
    }
}

private fun DrawScope.marker(at: Offset, label: String, color: Color, measurer: TextMeasurer, style: TextStyle) {
    val r = 11.dp.toPx()
    drawCircle(color.copy(alpha = 0.22f), r * 1.75f, at)
    drawCircle(Color.White, r + 2.dp.toPx(), at)
    drawCircle(color, r, at)
    val layout = measurer.measure(label, style)
    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
}
